package com.cyt.center.module.portal.service.hrdoc;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.portal.dal.mysql.hrdoc.PortalHrLayoutJobRepository;
import com.cyt.center.module.portal.domain.hrdoc.HrDocumentBlockValidator;
import com.cyt.center.module.portal.service.admin.PortalAiService;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/** 持久化任务 + 独立 worker；请求返回后不再读取登录上下文，输出只供编辑器应用。 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalHrLayoutService {
    private final PortalHrLayoutJobRepository repository;
    private final PortalHrLayoutDocumentReader reader;
    private final PortalHrLayoutNormalizer normalizer;
    private final PortalAiService ai;
    private final PortalOperatorResolver operators;
    private final PlatformTransactionManager transactions;
    private final ObjectMapper mapper;
    private final ThreadPoolExecutor workers=new ThreadPoolExecutor(2,2,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(100),Thread.ofPlatform().name("hr-layout-job-",0).factory(),new ThreadPoolExecutor.AbortPolicy());
    private JsonNode schema;
    private String system;
    private static final Pattern CHAPTER=Pattern.compile("^\\s*第[一二三四五六七八九十百零0-9]+(?:[章节编]|部分)");

    @PostConstruct void loadContract(){
        try(var schemaIn=new ClassPathResource("portal/hr-layout-schema.json").getInputStream();var tasksIn=new ClassPathResource("portal/ai-tasks.json").getInputStream()){
            schema=mapper.readTree(schemaIn);for(JsonNode task:mapper.readTree(tasksIn))if(task.path("name").asText().equals("hr.layout")){system=task.path("systemPrompt").asText();break;}
            if(system==null||system.isBlank())throw new IllegalStateException("缺少原 HR 排版提示词");
        }catch(Exception e){throw new IllegalStateException("HR 排版契约加载失败",e);}
    }
    @EventListener(ApplicationReadyEvent.class) public void recoverInterrupted(){repository.interruptStale();}
    @PreDestroy void shutdown(){workers.shutdownNow();}

    public Map<String,Object> create(long actor,String text,String sourceType,String sourceName){
        ai.requireConfiguration();String input=PortalHrLayoutDocumentReader.checkText(text);String username=operators.resolve(actor).username();
        String type=Set.of("paste","upload","current").contains(Objects.toString(sourceType,""))?sourceType:"paste";
        String name=sourceName==null?null:sourceName.substring(0,Math.min(255,sourceName.length()));
        Long id=tx().execute(status->repository.create(actor,username,input,type,name));
        if(id==null)throw new BizException("任务创建失败");submit(actor,id,input,username);return repository.detail(actor,id);
    }
    public Map<String,Object> createFile(long actor,MultipartFile file){var input=reader.read(file,true);return create(actor,input.text(),"upload",input.name());}
    public List<Map<String,Object>> list(long actor){return repository.list(actor);}
    public Map<String,Object> detail(long actor,long id){return repository.detail(actor,id);}
    public void delete(long actor,long id){repository.detail(actor,id);if(!repository.delete(actor,id))throw new BizException("进行中的排版任务不能删除");}
    public Map<String,Object> retry(long actor,long id){
        ai.requireConfiguration();var input=repository.input(actor,id);
        if(!"failed".equals(input.get("status")))throw new BizException("只有失败任务可以重试");
        String username=operators.resolve(actor).username();
        Boolean retried=tx().execute(status->{repository.reserveAttempt(actor);if(!repository.retry(actor,id))throw new BizException("任务状态已变化，请刷新");return true;});
        if(!Boolean.TRUE.equals(retried))throw new BizException("任务重试失败");submit(actor,id,Objects.toString(input.get("input_text"),""),username);return repository.detail(actor,id);
    }
    private void submit(long actor,long id,String text,String username){
        try{workers.execute(()->run(id,text,username));}catch(RejectedExecutionException e){repository.fail(id,"排版队列繁忙，请稍后重试");}
    }
    private void run(long id,String text,String username){
        if(!repository.start(id))return;
        try{var result=layout(text,username,(progress,stage)->repository.progress(id,progress,stage));repository.succeed(id,result.blocks(),result.summary());}
        catch(Exception e){log.warn("HR layout job {} failed: {}",id,e.getClass().getSimpleName());repository.fail(id,failure(e));}
    }
    public Map<String,Object> synchronous(long actor,String text){
        return synchronousInput(actor,new PortalHrLayoutDocumentReader.Input(null,PortalHrLayoutDocumentReader.checkText(text),null,null));
    }
    public Map<String,Object> synchronousFile(long actor,MultipartFile file){return synchronousInput(actor,reader.read(file,false));}
    private Map<String,Object> synchronousInput(long actor,PortalHrLayoutDocumentReader.Input input){
        ai.requireConfiguration();String username=operators.resolve(actor).username();
        tx().executeWithoutResult(status->repository.reserveAttempt(actor));
        try{
            PortalHrLayoutNormalizer.Result result;
            if(input.binary()!=null){String raw=ai.generateDocumentForActor("hr.layout","gemini-3.8-flash",system,"请将附件完整排版为 HR 文档块，不省略任何制度条款。",input.mime(),input.binary(),schema,username);result=normalizer.normalize(raw,"");}
            else result=layout(input.text(),username,(progress,stage)->{});
            return Map.of("blocks",result.blocks(),"summary",result.summary(),"sourceText",input.text());
        }catch(Exception e){if(e instanceof BizException b)throw b;throw new BizException("AI 排版失败，请重试或手动排版");}
    }
    PortalHrLayoutNormalizer.Result layout(String source,String username,BiConsumer<Integer,String> progress){
        List<String> segments=segments(source);
        if(segments.size()==1){progress.accept(10,"正在排版");var result=segment(source,username);progress.accept(95,"正在校验文档块");return result;}
        AtomicInteger completed=new AtomicInteger();
        List<CompletableFuture<Segment>> futures=new ArrayList<>();
        try(var pool=Executors.newFixedThreadPool(3,Thread.ofPlatform().name("hr-layout-segment-",0).factory())){
            for(String segment:segments)futures.add(CompletableFuture.supplyAsync(()->{
                try{var result=segment(segment,username);return new Segment(result.blocks(),result.summary(),true);}
                catch(Exception e){log.warn("HR layout segment failed: {}",e.getClass().getSimpleName());return new Segment(List.of(PortalHrLayoutNormalizer.fallback(segment)),"部分段落排版失败，已保留原文，请复核",false);}
                finally{int done=completed.incrementAndGet();progress.accept(5+done*85/segments.size(),"已完成 "+done+" / "+segments.size()+" 段");}
            },pool));
            List<Object> blocks=new ArrayList<>();List<String> notes=new ArrayList<>();int successes=0;
            for(var future:futures){Segment result=future.join();blocks.addAll(result.blocks);if(result.success)successes++;if(!result.summary.isBlank())notes.add(result.summary);}
            if(successes==0)throw new BizException("所有段落排版失败，请重试或手动排版");
            HrDocumentBlockValidator.validate(blocks);progress.accept(95,"正在合并并校验文档块");
            return new PortalHrLayoutNormalizer.Result(blocks,"分 "+segments.size()+" 段排版；"+String.join("；",notes.stream().distinct().toList()),0);
        }
    }
    private PortalHrLayoutNormalizer.Result segment(String text,String username){
        PortalHrLayoutNormalizer.Result best=null;String feedback="";
        for(int attempt=0;attempt<2;attempt++){
            try{
                String raw=ai.generateForActor("hr.layout","gemini-3.8-flash",system,"请将以下原文完整排版，不省略条款。\n\n"+text+(feedback.isBlank()?"":"\n\n上次输出需修正："+feedback),schema,username);
                var result=normalizer.normalize(raw,text);
                if(best==null||result.preservedLines()<best.preservedLines())best=result;
                if(result.preservedLines()<=3)return result;
                feedback="遗漏较多原文，请完整保留每条内容，特别是数值、条件与例外。";
            }catch(Exception e){feedback=failure(e);if(attempt==1&&best==null)throw e instanceof BizException b?b:new BizException("AI 排版失败，请重试或手动排版");}
        }
        return Objects.requireNonNull(best);
    }
    static List<String> segments(String source){
        if(source.length()<=1800)return List.of(source);
        List<String> parts=new ArrayList<>();StringBuilder current=new StringBuilder();
        for(String line:source.split("\\R",-1)){
            if((CHAPTER.matcher(line).find()&&current.length()>300)||current.length()+line.length()+1>1800){if(!current.isEmpty()){parts.add(current.toString());current.setLength(0);}}
            while(line.length()>1800){parts.add(line.substring(0,1800));line=line.substring(1800);}
            if(!current.isEmpty())current.append('\n');current.append(line);
        }
        if(!current.isEmpty())parts.add(current.toString());return parts.isEmpty()?List.of(source):parts;
    }
    private record Segment(List<Object> blocks,String summary,boolean success){}
    private TransactionTemplate tx(){return new TransactionTemplate(transactions);}
    private String failure(Exception error){return error instanceof BizException?Objects.toString(error.getMessage(),"排版失败，请重试"):"AI 排版失败，请重试或手动排版";}
}
