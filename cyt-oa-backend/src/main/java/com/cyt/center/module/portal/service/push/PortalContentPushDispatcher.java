package com.cyt.center.module.portal.service.push;

import com.cyt.center.core.integration.WecomGatewayClient;
import com.cyt.center.module.portal.dal.postgresql.push.PortalContentPushRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/** 独立事务发送：发布事务真正提交之后，异步执行，失败留可查询的真实结果。 */
@Service
@RequiredArgsConstructor
public class PortalContentPushDispatcher {
    private final PortalContentPushRepository repository;
    private final WecomGatewayClient gateway;
    private final ObjectMapper json;

    @Transactional(propagation=Propagation.REQUIRES_NEW,rollbackFor=Exception.class)
    public void dispatch(long id) {
        var job=repository.job(id,true);if(!"queued".equals(job.get("status")))return;
        String trigger=(String)job.get("push_trigger"),key=(String)job.get("target_key"),content=(String)job.get("content");
        LocalDateTime latest=repository.lockGuard(trigger,key),old=repository.historicalSuccess(trigger,key);
        if(old!=null&&(latest==null||old.isAfter(latest)))latest=old;
        boolean force=Boolean.TRUE.equals(job.get("force_send"))||job.get("force_send") instanceof Number n&&n.intValue()!=0;
        if(!force&&latest!=null&&latest.isAfter(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).minusHours(24))){repository.finish(id,"cooldown",0,0,"24小时内已成功推送，请确认后选择重新推送");return;}
        List<String> recipients;
        try{Object raw=job.get("recipients");recipients=json.readValue(raw instanceof byte[] b?new String(b,java.nio.charset.StandardCharsets.UTF_8):raw.toString(),new TypeReference<>(){});}catch(Exception ex){repository.finish(id,"failed",0,((Number)job.get("target_count")).intValue(),"推送任务数据无效");return;}
        if(recipients.isEmpty()){repository.finish(id,"no_recipients",0,0,"没有符合范围且绑定企微的在职员工");return;}
        String base=displayBase();
        if(base==null){String error="未配置 PORTAL_DISPLAY_BASE_URL，未发送无法访问的内容链接";repository.finish(id,"failed",0,recipients.size(),error);repository.audit(trigger,key,content,0,recipients.size(),(String)job.get("pushed_by"),error);return;}
        content=content.replace("{PORTAL_DISPLAY_BASE_URL}",base);
        int sent=0,failed=0;String error=null;
        for(int start=0;start<recipients.size();start+=100) {
            List<String> batch=recipients.subList(start,Math.min(start+100,recipients.size()));
            try {
                var result=gateway.call("wecom/messages/send-text",Map.of("content",content,"to_users",batch,
                        "enable_duplicate_check",force?0:1,"duplicate_check_interval",14400),Duration.ofSeconds(30));
                if(result.has("errcode")&&result.path("errcode").asInt()!=0)throw new IllegalStateException();
                Set<String> invalid=new HashSet<>();String invalidUsers=result.path("invaliduser").asText("");if(!invalidUsers.isBlank())invalid.addAll(Arrays.asList(invalidUsers.split("\\|")));
                int rejected=(int)batch.stream().filter(invalid::contains).count();sent+=batch.size()-rejected;failed+=rejected;if(rejected>0)error="部分员工不在企微应用可见范围";
            }catch(RuntimeException ex){failed+=batch.size();error="企微发送失败，请检查网关授权、应用可见范围和网络连接";}
        }
        repository.finish(id,failed==0?"sent":sent==0?"failed":"partial",sent,failed,error);
        repository.audit(trigger,key,content,sent,failed,(String)job.get("pushed_by"),error);
        if(sent>0)repository.success(trigger,key);
    }
    private String displayBase(){String value=System.getenv("PORTAL_DISPLAY_BASE_URL");if(value==null||value.isBlank()||value.length()>400)return null;try{URI u=URI.create(value);if(!Set.of("http","https").contains(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getFragment()!=null||u.getQuery()!=null)return null;return value.replaceAll("/+$","");}catch(Exception ex){return null;}}
}
