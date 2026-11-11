package com.cyt.center.module.portal.service.hrdoc;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.portal.dal.mysql.hrdoc.PortalHrEditorRepository;
import com.cyt.center.module.portal.dal.mysql.push.PortalContentPushRepository;
import com.cyt.center.module.portal.service.push.PortalContentPushDispatcher;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 原编辑器需要同步得到真实发送结果；测试预览只写推送审计，不改任何文档或草稿字段。 */
@Service
@RequiredArgsConstructor
public class PortalHrEditorCompatibilityService {
    private final PortalHrDocumentService documents;
    private final PortalHrEditorRepository editor;
    private final PortalContentPushDispatcher dispatcher;
    private final PortalContentPushRepository jobs;
    private final PortalOperatorResolver operators;
    private final PlatformTransactionManager transactions;

    public Map<String,Object> notify(Long actor,long id,String audience,List<String> scopes,
                                     List<String> usernames,boolean force,String customMessage) {
        var doc=documents.getDetail(id).doc();
        if(doc.getPublishedAt()==null||doc.getOfflineAt()!=null||doc.getDeletedAt()!=null)throw new BizException("仅已发布且在线的文档可以推送");
        String username=operators.resolve(actor).username();
        List<String> recipients;
        switch(Objects.toString(audience,"scope")){
            case "all" -> recipients=jobs.recipients(null,null);
            case "scope" -> {
                var effective=scopes==null?doc.getDeptScope():scopes;
                recipients=jobs.recipients(effective==null||effective.isEmpty()?null:effective,null);
                if(recipients.size()>50)throw new BizException("部门定向推送最多 50 人，请缩小范围");
            }
            case "users" -> {
                var selected=usernames==null?List.<String>of():usernames.stream().filter(Objects::nonNull).map(String::trim).filter(s->!s.isEmpty()).distinct().toList();
                if(selected.isEmpty()||selected.size()>50)throw new BizException("请选择 1 至 50 名收件人");
                if(!editor.allActive(selected))throw new BizException("部分收件人不存在或已停用，请重新选择");
                recipients=jobs.recipients(null,selected);
            }
            default -> throw new BizException("无效的推送范围");
        }
        String message=publishMessage(id,doc.getTitle(),doc.getVersion(),Boolean.TRUE.equals(doc.getRequiresSignoff()),false);
        if(customMessage!=null&&!customMessage.isBlank())message+="\n"+customMessage.trim();
        String key=(doc.getSlug()==null?"article:"+id:doc.getSlug())+":"+doc.getVersion();
        String content=message;
        Long jobId=new TransactionTemplate(transactions).execute(status->jobs.enqueue("hr_doc",key,content,recipients,username,force));
        if(jobId==null)throw new BizException("推送任务创建失败");
        dispatcher.dispatch(jobId);
        var result=result(jobs.job(jobId,false));result.put("audience",audience);result.put("recipients",recipients.size());return result;
    }

    public Map<String,Object> test(Long actor,long id,String title,String version,Boolean requiresSignoff) {
        var doc=documents.getDetail(id).doc();String username=operators.resolve(actor).username();
        String effectiveTitle=nonblank(title,nonblank(doc.getDraftTitle(),doc.getTitle()));
        String effectiveVersion=nonblank(version,nonblank(doc.getDraftVersion(),doc.getVersion()));
        boolean effectiveSignoff=requiresSignoff!=null?requiresSignoff:doc.getDraftRequiresSignoff()!=null?doc.getDraftRequiresSignoff():Boolean.TRUE.equals(doc.getRequiresSignoff());
        if(effectiveTitle.length()>200||effectiveVersion.length()>16)throw new BizException("测试标题或版本号过长");
        String message=publishMessage(id,effectiveTitle,effectiveVersion,effectiveSignoff,true);
        String key=(doc.getSlug()==null?"article:"+id:doc.getSlug())+":test:"+username;
        List<String> recipients=jobs.recipients(null,List.of(username));
        Long jobId=new TransactionTemplate(transactions).execute(status->jobs.enqueue("hr_doc_test",key,message,recipients,username,true));
        if(jobId==null)throw new BizException("测试推送任务创建失败");
        dispatcher.dispatch(jobId);return result(jobs.job(jobId,false));
    }
    private String publishMessage(long id,String title,String version,boolean signoff,boolean test){return (test?"【测试推送 · 仅你可见】\n":"")+"【CYT 制度更新】\n《"+title+"》已更新至 "+version+(signoff?"\n⚠️ 本制度需要签收，请尽快确认":"")+"\n查看：{PORTAL_DISPLAY_BASE_URL}/article/"+id;}

    private Map<String,Object> result(Map<String,Object> row) {
        String status=Objects.toString(row.get("status"),"");
        if(status.equals("queued"))throw new BizException("推送尚未完成，请稍后查看发送记录");
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("success",status.equals("sent")||status.equals("no_recipients"));
        result.put("skipped",status.equals("cooldown"));result.put("reason",status.equals("cooldown")?"cooldown":null);
        result.put("noRecipients",status.equals("no_recipients"));
        result.put("sent",row.get("sent_count"));result.put("failed",row.get("failed_count"));result.put("targetCount",row.get("target_count"));
        result.put("status",status);result.put("jobId",row.get("id"));
        Object error=row.get("error_msg");result.put("errors",error==null?List.of():List.of(error.toString()));
        result.put("allowRetry",status.equals("failed"));
        if(status.equals("cooldown")){var previous=editor.lastPush((String)row.get("target_key"));result.put("lastPushedAt",previous==null?null:previous.get("pushedAt"));result.put("cooldownHours",24);}
        return result;
    }
    private String nonblank(String a,String fallback){return a==null||a.isBlank()?Objects.toString(fallback,""):a;}
}
