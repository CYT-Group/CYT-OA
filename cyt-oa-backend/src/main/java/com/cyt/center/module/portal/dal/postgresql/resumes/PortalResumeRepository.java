package com.cyt.center.module.portal.dal.postgresql.resumes;

import com.cyt.center.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class PortalResumeRepository {
    private final JdbcTemplate jdbc;
    public static final List<String> SYNC=List.of("name","gender","birth_date","phone","email","current_city","highest_degree","years_of_experience","current_company","current_title","expected_position","expected_salary","summary");
    public Map<String,Object> get(long id,boolean lock) {
        var rows=jdbc.queryForList("SELECT * FROM portal_resumes WHERE id=?"+(lock?" FOR UPDATE":""),id);
        if(rows.isEmpty()) throw new BizException("简历不存在");return rows.getFirst();
    }
    private String where(String q,String status,List<Object> args) {
        StringBuilder where=new StringBuilder(" WHERE 1=1");
        if(q!=null&&!q.isBlank()){where.append(" AND (name LIKE ? OR phone LIKE ? OR email LIKE ? OR expected_position LIKE ?)");for(int i=0;i<4;i++)args.add("%"+q.trim()+"%");}
        if(status!=null&&!status.isBlank()){where.append(" AND status=?");args.add(status);}return where.toString();
    }
    public List<Map<String,Object>> page(String q,String status,int offset,int size) {
        List<Object> args=new ArrayList<>();String where=where(q,status,args);args.add(size);args.add(offset);
        return jdbc.queryForList("SELECT * FROM portal_resumes"+where+" ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",args.toArray());
    }
    public long count(String q,String status) { List<Object> args=new ArrayList<>();return jdbc.queryForObject("SELECT COUNT(*) FROM portal_resumes"+where(q,status,args),Long.class,args.toArray()); }
    public long insert(String filename,String ext,int size,String sha,String key,String actor) {
        GeneratedKeyHolder id=new GeneratedKeyHolder();
        jdbc.update(c -> {var p=c.prepareStatement("INSERT INTO portal_resumes(status,source_filename,file_ext,file_size,file_sha256,storage_key,extracted,uploaded_by) VALUES('failed',?,?,?,?,?,'{}',?)",new String[]{"id"});
            p.setString(1,filename);p.setString(2,ext);p.setInt(3,size);p.setString(4,sha);p.setString(5,key);p.setString(6,actor);return p;},id);
        return id.getKey().longValue();
    }
    public Long duplicate(long id,String sha,String phone,String email) {
        var rows=jdbc.queryForList("SELECT id FROM portal_resumes WHERE id<>? AND (file_sha256=? OR (CAST(? AS varchar) IS NOT NULL AND phone=?) OR (CAST(? AS varchar) IS NOT NULL AND email=?)) ORDER BY id LIMIT 1",Long.class,id,sha,phone,phone,email,email);
        return rows.isEmpty()?null:rows.getFirst();
    }
    public void save(long id,long revision,Map<String,Object> extracted,String serialized,String raw,String model,String status,String error,Long duplicate) {
        List<Object> args=new ArrayList<>();StringBuilder set=new StringBuilder();
        for(String key:SYNC){if(!set.isEmpty())set.append(',');set.append(key).append("=?");args.add(extracted.get(key));}
        set.append(",extracted=?,ai_raw=COALESCE(?,ai_raw),ai_model=COALESCE(?,ai_model),status=?,error_msg=?,duplicate_of_id=?,confirmed_by=NULL,confirmed_at=NULL,revision=revision+1,updated_at=CURRENT_TIMESTAMP");
        Collections.addAll(args,serialized,raw,model,status,error,duplicate,id,revision);
        if(jdbc.update("UPDATE portal_resumes SET "+set+" WHERE id=? AND revision=?",args.toArray())!=1)
            throw new BizException("简历已被其他人修改，请刷新后重试");
    }
    public void confirmed(long id,String actor) { jdbc.update("UPDATE portal_resumes SET status='confirmed',confirmed_by=?,confirmed_at=CURRENT_TIMESTAMP,revision=revision+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",actor,id); }
    public void delete(long id) { jdbc.update("DELETE FROM portal_resume_interview_rounds WHERE process_id IN (SELECT id FROM portal_resume_interview_processes WHERE resume_id=?)",id);jdbc.update("DELETE FROM portal_resume_interview_processes WHERE resume_id=?",id);jdbc.update("UPDATE portal_resumes SET duplicate_of_id=NULL WHERE duplicate_of_id=?",id);jdbc.update("DELETE FROM portal_resumes WHERE id=?",id); }
}
