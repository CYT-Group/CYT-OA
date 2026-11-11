package com.cyt.center.module.portal.service.push;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.portal.dal.mysql.push.PortalContentPushRepository;
import com.cyt.center.module.portal.dal.mysql.news.PortalNewsMapper;
import com.cyt.center.module.portal.dal.dataobject.news.PortalNewsDO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDO;
import com.cyt.center.module.portal.service.admin.PortalRows;
import com.cyt.center.module.portal.service.hrdoc.PortalHrDocumentService;
import com.cyt.center.module.portal.service.support.PortalAccess;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class PortalContentPushService {
    @Resource private PortalContentPushRepository repository;
    @Resource private PortalContentPushDispatcher dispatcher;
    @Resource private PortalOperatorResolver operators;
    @Resource private PortalAccess access;
    @Resource private PortalNewsMapper news;
    @Resource private PortalHrDocumentService documents;
    @Resource(name="portalPushExecutor") private Executor executor;

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> news(Long actor,PortalNewsDO row,List<String> scopes,boolean force,boolean test) {
        if(!test&&!PortalNewsDO.STATUS_PUBLISHED.equals(row.getStatus()))throw new BizException("仅已发布动态可推送");
        String operator=operators.resolve(actor).username();List<String> recipients=repository.recipients(clean(scopes),test?List.of(operator):null);
        String snippet=org.jsoup.Jsoup.parse(Objects.toString(row.getSummary(),Objects.toString(row.getContent(),""))).text();if(snippet.length()>80)snippet=snippet.substring(0,80)+"…";
        String content="【CYT 动态】\n"+row.getTitle()+"\n"+snippet+"\n查看：{PORTAL_DISPLAY_BASE_URL}/news-detail?id="+row.getId();
        return enqueue(test?"news_test":"news","news:"+row.getId()+(test?":test:"+operator:""),content,recipients,operator,force||test);
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> document(Long actor,long id,String audience,List<String> scopes,List<String> usernames,boolean force,String customMessage,boolean test) {
        PortalHrDocumentDO row=documents.getDetail(id).doc();
        if(!test&&(!row.publishedBefore()||row.isDeleted()||row.isOffline()))throw new BizException("仅已发布且在架文档可推送");
        String operator=operators.resolve(actor).username();List<String> recipients;
        if(test)recipients=repository.recipients(null,List.of(operator));
        else if("users".equals(audience)){List<String> selected=clean(usernames);if(selected==null||selected.isEmpty()||selected.size()>200)throw new BizException("指定员工须为1至200人");recipients=repository.recipients(null,selected);}
        else if("all".equals(audience))recipients=repository.recipients(null,null);
        else if("scope".equals(audience)||audience==null)recipients=repository.recipients(clean(scopes==null?row.getDeptScope():scopes),null);
        else throw new BizException("推送范围须为 all、scope 或 users");
        if(!test&&recipients.size()>200)throw new BizException("一次最多推送200名员工，请缩小范围");
        String content="【CYT 制度通知】\n"+row.getTitle()+" "+Objects.toString(row.getVersion(),"")+"\n"+(Boolean.TRUE.equals(row.getRequiresSignoff())?"请阅读并完成线上签收。":"请阅读最新发布内容。")
                +"\n查看：{PORTAL_DISPLAY_BASE_URL}/article/"+row.getId();
        if(customMessage!=null&&!customMessage.isBlank()){if(customMessage.length()>300)throw new BizException("附加文案最多300字");content+="\n"+customMessage.trim();}
        String key=Objects.toString(row.getSlug(),"article:"+row.getId())+":"+Objects.toString(row.getVersion(),"draft");
        return enqueue(test?"hr_doc_test":"hr_doc",key+(test?":test:"+operator:""),content,recipients,operator,force||test);
    }
    private Map<String,Object> enqueue(String trigger,String key,String content,List<String> recipients,String actor,boolean force) {
        long id=repository.enqueue(trigger,key,content,recipients,actor,force);
        Runnable schedule=()->submit(id);
        if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){schedule.run();}});else schedule.run();
        return Map.of("jobId",id,"status","queued","targetCount",recipients.size(),"sent",0,"failed",0,"trigger",trigger,"targetKey",key);
    }
    private void submit(long id){try{executor.execute(()->{try{dispatcher.dispatch(id);}catch(RuntimeException ex){log.warn("门户内容推送任务{}未完成，请查看管理页发送记录",id);}});}catch(RuntimeException ex){log.warn("门户内容推送队列已满，任务{}保留至下一轮恢复",id);}}
    @Scheduled(fixedDelay=30000,initialDelay=30000) public void recover(){try{for(Long id:repository.queued())submit(id);}catch(RuntimeException ex){log.debug("门户内容推送表尚未就绪，等待管理员完成数据库升级");}}
    public Map<String,Object> latest(Long actor,String trigger,String key){
        if(!Set.of("news","hr_doc","news_test","hr_doc_test").contains(trigger))throw new BizException("无效推送类型");
        if(trigger.startsWith("hr_doc")){if(!access.has(actor,"can_edit_hr_documents"))throw new BizException("没有HR文档管理权限");}
        else if(!access.has(actor,"can_review")){String[] parts=key.split(":");if(parts.length<2)throw new BizException("动态标识无效");long id;try{id=Long.parseLong(parts[1]);}catch(Exception ex){throw new BizException("动态标识无效");}var row=news.selectById(id);if(row==null||!operators.resolve(actor).username().equals(row.getAuthorUsername()))throw new BizException("无权查看该推送结果");}
        var row=repository.latest(trigger,key);return row==null?null:result(row);
    }
    public Map<String,Object> result(Map<String,Object> row){var result=PortalRows.camel(row);result.remove("recipients");result.remove("content");result.put("sent",row.get("sent_count"));result.put("failed",row.get("failed_count"));return result;}
    private List<String> clean(List<String> values){if(values==null)return null;if(values.size()>200)throw new BizException("选择范围过大");LinkedHashSet<String> out=new LinkedHashSet<>();for(String value:values){if(value==null||value.isBlank()||value.length()>128)throw new BizException("部门或员工标识无效");out.add(value.trim());}return new ArrayList<>(out);}
}
