package com.cyt.center.module.portal.dal.mysql.resumes;

import com.cyt.center.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PortalResumeInterviewRepository {
    private final JdbcTemplate jdbc;
    public List<Map<String,Object>> processes(long resumeId) { return jdbc.queryForList("SELECT * FROM portal_resume_interview_processes WHERE resume_id=? ORDER BY created_at DESC,id DESC",resumeId); }
    public List<Map<String,Object>> rounds(long processId) { return jdbc.queryForList("SELECT * FROM portal_resume_interview_rounds WHERE process_id=? ORDER BY sequence,id",processId); }
    public Map<String,Object> process(long resumeId,long processId,boolean lock) {
        var rows=jdbc.queryForList("SELECT * FROM portal_resume_interview_processes WHERE id=? AND resume_id=?"+(lock?" FOR UPDATE":""),processId,resumeId);
        if(rows.isEmpty())throw new BizException("面试流程不存在");return rows.getFirst();
    }
    public Map<String,Object> round(long processId,long roundId) { return rounds(processId).stream().filter(r->((Number)r.get("id")).longValue()==roundId).findFirst().orElseThrow(()->new BizException("面试轮次不存在")); }
    public long create(long resumeId,String position,String actor) {
        GeneratedKeyHolder key=new GeneratedKeyHolder();jdbc.update(c->{var p=c.prepareStatement("INSERT INTO portal_resume_interview_processes(resume_id,position,created_by,updated_by) VALUES(?,?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);p.setLong(1,resumeId);p.setString(2,position);p.setString(3,actor);p.setString(4,actor);return p;},key);return key.getKey().longValue();
    }
    public void createRound(long processId,int sequence,Map<String,Object> r,String actor) { jdbc.update("INSERT INTO portal_resume_interview_rounds(process_id,sequence,name,interviewer,scheduled_at,location,status,updated_by) VALUES(?,?,?,?,?,?,'pending',?)",processId,sequence,r.get("name"),r.get("interviewer"),r.get("scheduledAt"),r.get("location"),actor); }
    public void updateRound(long processId,long roundId,Map<String,Object> r,String actor) {
        jdbc.update("UPDATE portal_resume_interview_rounds SET name=?,interviewer=?,scheduled_at=?,location=?,status=?,result=?,feedback=?,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND process_id=?",r.get("name"),r.get("interviewer"),r.get("scheduledAt"),r.get("location"),r.get("status"),r.get("result"),r.get("feedback"),actor,roundId,processId);
        jdbc.update("UPDATE portal_resume_interview_processes SET updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",actor,processId);
    }
    public void close(long processId,String status,String result,String summary,String actor) {
        jdbc.update("UPDATE portal_resume_interview_rounds SET status='cancelled',result=NULL,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE process_id=? AND status='pending'",actor,processId);
        jdbc.update("UPDATE portal_resume_interview_processes SET status=?,result=?,summary=?,updated_by=?,finished_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=?",status,result,summary,actor,processId);
    }
    public void deleteResume(long resumeId) { jdbc.update("DELETE FROM portal_resume_interview_rounds WHERE process_id IN (SELECT id FROM portal_resume_interview_processes WHERE resume_id=?)",resumeId);jdbc.update("DELETE FROM portal_resume_interview_processes WHERE resume_id=?",resumeId); }
}
