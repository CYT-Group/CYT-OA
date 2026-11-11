package com.cyt.center.module.portal.dal.mysql.hrdoc;

import com.cyt.center.core.exception.BizException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.Statement;
import java.util.*;

/** 任务持久化与按提交人隔离，任务正文不会进入文档 CMS。 */
@Repository
@RequiredArgsConstructor
public class PortalHrLayoutJobRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private static final String COLUMNS="id,status,source_type AS sourceType,source_name AS sourceName,input_chars AS inputChars,progress,stage,error,summary,COALESCE(JSON_LENGTH(blocks),0) AS blockCount,created_at AS createdAt,finished_at AS finishedAt";
    public long create(long userId,String username,String text,String type,String name){
        reserveAttempt(userId);
        GeneratedKeyHolder key=new GeneratedKeyHolder();jdbc.update(c->{var p=c.prepareStatement("INSERT INTO portal_hr_layout_jobs(user_id,actor_username,status,source_type,source_name,input_text,input_chars,progress,stage) VALUES(?,?,'pending',?,?,?, ?,0,'等待处理')",Statement.RETURN_GENERATED_KEYS);p.setLong(1,userId);p.setString(2,username);p.setString(3,type);p.setString(4,name);p.setString(5,text);p.setInt(6,text.length());return p;},key);return Objects.requireNonNull(key.getKey()).longValue();
    }
    public void reserveAttempt(long userId){
        // 同一用户并发请求串行检查额度，重试也计次，避免多窗口越过每小时 20 次限制。
        jdbc.queryForObject("SELECT id FROM sys_user WHERE id=? FOR UPDATE",Long.class,userId);
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM portal_hr_layout_attempts WHERE user_id=? AND created_at>DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 1 HOUR)",Integer.class,userId);
        if(count!=null&&count>=20)throw new BizException("排版任务每小时最多 20 次，请稍后重试");
        jdbc.update("INSERT INTO portal_hr_layout_attempts(user_id) VALUES(?)",userId);
    }
    public List<Map<String,Object>> list(long userId){var rows=jdbc.queryForList("SELECT "+COLUMNS+" FROM portal_hr_layout_jobs WHERE user_id=? ORDER BY id DESC LIMIT 20",userId);rows.forEach(this::numericCount);return rows;}
    public Map<String,Object> detail(long userId,long id){var rows=jdbc.queryForList("SELECT "+COLUMNS+",blocks FROM portal_hr_layout_jobs WHERE user_id=? AND id=?",userId,id);if(rows.isEmpty())throw new BizException("排版任务不存在");var row=rows.getFirst();Object raw=row.get("blocks");try{row.put("blocks",raw==null?List.of():mapper.readValue(raw.toString(),new TypeReference<List<Object>>(){}));}catch(Exception e){throw new IllegalStateException("排版任务结果无法读取",e);}numericCount(row);return row;}
    private void numericCount(Map<String,Object> row){row.put("blockCount",row.get("blockCount") instanceof Number count?count.intValue():0);}
    public Map<String,Object> input(long userId,long id){var rows=jdbc.queryForList("SELECT * FROM portal_hr_layout_jobs WHERE user_id=? AND id=?",userId,id);if(rows.isEmpty())throw new BizException("排版任务不存在");return rows.getFirst();}
    public boolean start(long id){return jdbc.update("UPDATE portal_hr_layout_jobs SET status='running',progress=1,stage='正在准备排版' WHERE id=? AND status='pending'",id)==1;}
    public void progress(long id,int progress,String stage){jdbc.update("UPDATE portal_hr_layout_jobs SET progress=GREATEST(progress,?),stage=? WHERE id=? AND status='running'",progress,stage,id);}
    public void succeed(long id,List<Object> blocks,String summary){String json;try{json=mapper.writeValueAsString(blocks);}catch(Exception e){throw new IllegalStateException(e);}jdbc.update("UPDATE portal_hr_layout_jobs SET status='succeeded',progress=100,stage='排版完成',blocks=?,summary=?,error=NULL,finished_at=CURRENT_TIMESTAMP WHERE id=? AND status='running'",json,summary,id);}
    public void fail(long id,String error){String safe=error==null?"排版失败，请重试":error;if(safe.length()>2000)safe=safe.substring(0,2000);jdbc.update("UPDATE portal_hr_layout_jobs SET status='failed',stage='排版失败',error=?,finished_at=CURRENT_TIMESTAMP WHERE id=? AND status IN ('pending','running')",safe,id);}
    public boolean retry(long userId,long id){return jdbc.update("UPDATE portal_hr_layout_jobs SET status='pending',progress=0,stage='等待重试',error=NULL,summary=NULL,blocks=NULL,finished_at=NULL,created_at=CURRENT_TIMESTAMP WHERE user_id=? AND id=? AND status='failed'",userId,id)==1;}
    public boolean delete(long userId,long id){return jdbc.update("DELETE FROM portal_hr_layout_jobs WHERE user_id=? AND id=? AND status IN ('succeeded','failed')",userId,id)==1;}
    public void interruptStale(){jdbc.update("UPDATE portal_hr_layout_jobs SET status='failed',stage='任务已中断',error='服务重启，任务已中断，请重新提交或重试',finished_at=CURRENT_TIMESTAMP WHERE status IN ('pending','running')");}
}
