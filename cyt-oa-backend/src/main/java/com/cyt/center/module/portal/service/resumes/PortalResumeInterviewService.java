package com.cyt.center.module.portal.service.resumes;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.portal.dal.postgresql.resumes.PortalResumeRepository;
import com.cyt.center.module.portal.dal.postgresql.resumes.PortalResumeInterviewRepository;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import com.cyt.center.module.portal.service.admin.PortalRows;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PortalResumeInterviewService {
    private final PortalResumeInterviewRepository repository;
    private final PortalResumeRepository resumes;
    private final PortalOperatorResolver operators;
    private final PortalAudit audit;
    public List<Map<String,Object>> list(long resumeId) { resumes.get(resumeId,false);return repository.processes(resumeId).stream().map(p->snapshot(resumeId,((Number)p.get("id")).longValue())).toList(); }
    public Map<String,Object> snapshot(long resumeId,long processId) {
        var p=wire(repository.process(resumeId,processId,false));p.remove("activeResumeId");p.put("rounds",repository.rounds(processId).stream().map(this::wire).toList());return p;
    }
    private Map<String,Object> wire(Map<String,Object> raw) { var out=PortalRows.camel(raw);out.replaceAll((k,v)->v instanceof java.sql.Timestamp t?t.toLocalDateTime().atOffset(ZoneOffset.ofHours(8)).toString():v instanceof LocalDateTime d?d.atOffset(ZoneOffset.ofHours(8)).toString():v);return out; }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> create(Long actor,long resumeId,String position,List<Map<String,Object>> rounds) {
        var resume=resumes.get(resumeId,true);
        if(!List.of("parsed","confirmed").contains(resume.get("status"))||resume.get("name")==null||resume.get("name").toString().isBlank())throw new BizException("请先完成简历解析并填写候选人姓名");
        if(rounds==null||rounds.isEmpty()||rounds.size()>10)throw new BizException("面试轮次须为 1 至 10 轮");
        String job=text(position,"应聘岗位",120,true),operator=operators.resolve(actor).username();
        for(var p:repository.processes(resumeId))if("active".equals(p.get("status")))throw new BizException("该候选人已有进行中的面试流程");
        long id;try{id=repository.create(resumeId,job,operator);}catch(DuplicateKeyException ex){throw new BizException("该候选人已有进行中的面试流程");}
        for(int i=0;i<rounds.size();i++){Map<String,Object> values=new LinkedHashMap<>(rounds.get(i));values.put("status","pending");values.put("result",null);values.put("feedback",null);repository.createRound(id,i+1,cleanRound(values,true),operator);}
        audit.record(actor,"resume_interview_create","portal_resume_interview_processes",id,null,"resumeId="+resumeId,null);return snapshot(resumeId,id);
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> updateRound(Long actor,long resumeId,long processId,long roundId,Map<String,Object> patch) {
        resumes.get(resumeId,true);var process=repository.process(resumeId,processId,true);active(process);
        if(patch==null||patch.isEmpty()||!Set.of("name","interviewer","scheduledAt","location","status","result","feedback").containsAll(patch.keySet()))throw new BizException("没有可更新的轮次字段或包含未知字段");
        Map<String,Object> values=PortalRows.camel(repository.round(processId,roundId));
        if(patch.containsKey("scheduledAt"))patch=new LinkedHashMap<>(patch);
        values.putAll(patch);values=cleanRound(values,patch.containsKey("scheduledAt"));
        repository.updateRound(processId,roundId,values,operators.resolve(actor).username());
        audit.record(actor,"resume_interview_round_update","portal_resume_interview_processes",processId,null,"fields="+String.join(",",patch.keySet()),null);return snapshot(resumeId,processId);
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> close(Long actor,long resumeId,long processId,String summary,String result,boolean cancel) {
        resumes.get(resumeId,true);active(repository.process(resumeId,processId,true));String text=text(summary,"流程总结",5000,true);
        if(!cancel&&!Set.of("passed","rejected").contains(result==null?"":result))throw new BizException("流程结论只能为 passed 或 rejected");
        repository.close(processId,cancel?"cancelled":"completed",cancel?null:result,text,operators.resolve(actor).username());
        audit.record(actor,cancel?"resume_interview_cancel":"resume_interview_finish","portal_resume_interview_processes",processId,null,cancel?"cancelled":result,null);return snapshot(resumeId,processId);
    }

    public Map<String,Object> cleanRound(Map<String,Object> values,boolean parseDate) {
        Map<String,Object> r=new LinkedHashMap<>();
        for(var spec:List.of(new Object[]{"name","轮次名称",100,true},new Object[]{"interviewer","面试官",100,false},new Object[]{"location","面试地点",500,false},new Object[]{"feedback","面试反馈",5000,false}))r.put((String)spec[0],text(values.get(spec[0]),(String)spec[1],(Integer)spec[2],(Boolean)spec[3]));
        if(values.containsKey("status")&&values.get("status")==null)throw new BizException("轮次状态不能为空");
        String status=Objects.toString(values.get("status"),"pending"),result=text(values.get("result"),"面试结论",16,false);
        if(!Set.of("pending","completed","cancelled").contains(status))throw new BizException("无效轮次状态");
        if(status.equals("completed")){if(!Set.of("passed","rejected").contains(result==null?"":result)||r.get("feedback")==null)throw new BizException("完成轮次须填写结论及面试反馈");}
        else if(result!=null)throw new BizException("待面试或已取消轮次不能保留结论");
        r.put("status",status);r.put("result",result);Object scheduled=values.get("scheduledAt");
        if(parseDate&&scheduled!=null){try{scheduled=OffsetDateTime.parse(scheduled.toString()).atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime();}catch(Exception ex){throw new BizException("面试时间须为带时区的 ISO 时间");}}
        r.put("scheduledAt",scheduled);return r;
    }
    private void active(Map<String,Object> process){if(!"active".equals(process.get("status")))throw new BizException("面试流程已结束，不能修改");}
    private String text(Object value,String label,int cap,boolean required){if(value!=null&&!(value instanceof String))throw new BizException(label+"须为文字");String s=value==null?null:value.toString().trim();if(s==null||s.isBlank()){if(required)throw new BizException("请填写"+label);return null;}if(s.length()>cap)throw new BizException(label+"不能超过"+cap+"字");return s;}
}
