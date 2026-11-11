package com.cyt.center.module.portal.dal.mysql.push;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.portal.service.admin.PortalRows;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class PortalContentPushRepository {
    private final JdbcTemplate jdbc;
    public List<String> recipients(List<String> scopes,List<String> usernames) {
        StringBuilder sql=new StringBuilder("SELECT DISTINCT u.wecom_userid FROM sys_user u WHERE u.enabled=1 AND u.wecom_userid IS NOT NULL AND u.wecom_userid<>''");List<Object> args=new ArrayList<>();
        if(usernames!=null){if(usernames.isEmpty())return List.of();String marks=String.join(",",Collections.nCopies(usernames.size(),"?"));sql.append(" AND u.username IN (").append(marks).append(')');args.addAll(usernames);}
        if(scopes!=null&&!scopes.isEmpty()) {
            String marks=String.join(",",Collections.nCopies(scopes.size(),"?"));
            sql.append(" AND EXISTS (SELECT 1 FROM sys_dept d LEFT JOIN sys_user_dept ud ON ud.dept_id=d.id AND ud.user_id=u.id WHERE (ud.user_id=u.id OR d.id=u.main_dept_id) AND (CAST(d.id AS CHAR) IN (").append(marks).append(") OR d.name IN (").append(marks).append(") OR d.path IN (").append(marks).append(")))");
            args.addAll(scopes);args.addAll(scopes);args.addAll(scopes);
        }
        sql.append(" ORDER BY u.wecom_userid");return jdbc.queryForList(sql.toString(),String.class,args.toArray());
    }
    public long enqueue(String trigger,String key,String content,List<String> recipients,String actor,boolean force) {
        GeneratedKeyHolder id=new GeneratedKeyHolder();jdbc.update(c->{var p=c.prepareStatement("INSERT INTO portal_content_push_jobs(push_trigger,target_key,content,recipients,pushed_by,force_send,target_count,status) VALUES(?,?,?,?,?,?,?,'queued')",java.sql.Statement.RETURN_GENERATED_KEYS);
            p.setString(1,trigger);p.setString(2,key);p.setString(3,content);p.setString(4,PortalRows.json(recipients));p.setString(5,actor);p.setBoolean(6,force);p.setInt(7,recipients.size());return p;},id);return id.getKey().longValue();
    }
    public Map<String,Object> job(long id,boolean lock) {var rows=jdbc.queryForList("SELECT * FROM portal_content_push_jobs WHERE id=?"+(lock?" FOR UPDATE":""),id);if(rows.isEmpty())throw new BizException("推送任务不存在");return rows.getFirst();}
    public List<Long> queued() {return jdbc.queryForList("SELECT id FROM portal_content_push_jobs WHERE status='queued' ORDER BY id LIMIT 20",Long.class);}
    public Map<String,Object> latest(String trigger,String key) {var rows=jdbc.queryForList("SELECT * FROM portal_content_push_jobs WHERE push_trigger=? AND target_key=? ORDER BY id DESC LIMIT 1",trigger,key);return rows.isEmpty()?null:rows.getFirst();}
    public LocalDateTime lockGuard(String trigger,String key) {
        jdbc.update("INSERT IGNORE INTO portal_content_push_guard(push_trigger,target_key) VALUES(?,?)",trigger,key);
        var value=jdbc.queryForMap("SELECT last_success_at FROM portal_content_push_guard WHERE push_trigger=? AND target_key=? FOR UPDATE",trigger,key).get("last_success_at");
        return value==null?null:value instanceof java.sql.Timestamp t?t.toLocalDateTime():(LocalDateTime)value;
    }
    public void success(String trigger,String key) {jdbc.update("UPDATE portal_content_push_guard SET last_success_at=CURRENT_TIMESTAMP WHERE push_trigger=? AND target_key=?",trigger,key);}
    public void finish(long id,String status,int sent,int failed,String error) {jdbc.update("UPDATE portal_content_push_jobs SET status=?,sent_count=?,failed_count=?,error_msg=?,finished_at=CURRENT_TIMESTAMP WHERE id=?",status,sent,failed,error,id);}
    public void audit(String trigger,String key,String content,int sent,int failed,String actor,String error) {
        jdbc.update("INSERT INTO portal_wecom_push_log(`trigger`,target_key,content,recipient_count,failed_count,pushed_by,pushed_at,error_msg) VALUES(?,?,?,?,?,?,?,?)",trigger,key.length()>128?key.substring(0,128):key,content,sent,failed,actor,sent>0?LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")):LocalDateTime.of(1970,1,1,0,0),error);
    }
    /** 历史成功日志同样占用冷却窗口，切流当天不会把刚发过的内容再群发一次。 */
    public LocalDateTime historicalSuccess(String trigger,String key) {
        Object v=jdbc.queryForMap("SELECT MAX(pushed_at) AS latest FROM portal_wecom_push_log WHERE `trigger`=? AND target_key=? AND recipient_count>0",trigger,key).get("latest");
        return v==null?null:v instanceof java.sql.Timestamp t?t.toLocalDateTime():(LocalDateTime)v;
    }
}
