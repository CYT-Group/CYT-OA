package com.cyt.center.module.portal.dal.mysql.hrdoc;

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 原 HR 编辑器的附加读模型，SQL 不进入控制器或排版规则。 */
@Repository
@RequiredArgsConstructor
public class PortalHrEditorRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public Map<String,Object> lastPush(String key) {
        var rows=jdbc.queryForList("SELECT pushed_at AS pushedAt,recipient_count AS recipientCount,failed_count AS failedCount,pushed_by AS pushedBy FROM portal_wecom_push_log WHERE `trigger`='hr_doc' AND target_key=? ORDER BY pushed_at DESC,id DESC LIMIT 1",key);
        return rows.isEmpty()?null:rows.getFirst();
    }

    public List<Map<String,Object>> searchPushUsers(String query) {
        if(query==null||query.isBlank())return List.of();
        String q=query.trim();if(q.length()>100)q=q.substring(0,100);
        var rows=jdbc.queryForList("SELECT u.username,u.name AS displayName,COALESCE((SELECT JSON_ARRAYAGG(d.name) FROM sys_dept d WHERE d.id=u.main_dept_id OR EXISTS(SELECT 1 FROM sys_user_dept ud WHERE ud.user_id=u.id AND ud.dept_id=d.id)),JSON_ARRAY()) AS department,u.enabled AS isActive FROM sys_user u WHERE u.name LIKE ? OR u.username LIKE ? ORDER BY u.enabled DESC,u.name,u.id LIMIT 20","%"+q+"%","%"+q+"%");
        for(var row:rows){try{row.put("department",mapper.readValue(row.get("department").toString(),new TypeReference<List<String>>(){}));}catch(Exception e){throw new IllegalStateException("部门数据无效",e);}Object active=row.get("isActive");row.put("isActive",active instanceof Boolean b?b:active instanceof Number n&&n.intValue()==1);}
        return rows;
    }

    public List<String> departments() {
        return jdbc.queryForList("SELECT DISTINCT name FROM sys_dept WHERE active=1 AND name<>'' ORDER BY name",String.class);
    }

    public List<Map<String,Object>> departmentStats() {
        var rows=jdbc.queryForList("SELECT d.name,COUNT(DISTINCT u.id) AS userCount FROM sys_dept d LEFT JOIN sys_user u ON u.enabled=1 AND (u.main_dept_id=d.id OR EXISTS(SELECT 1 FROM sys_user_dept ud WHERE ud.user_id=u.id AND ud.dept_id=d.id)) WHERE d.active=1 AND d.name<>'' GROUP BY d.name ORDER BY d.name");
        rows.forEach(row->{if(row.get("userCount") instanceof Number count)row.put("userCount",count.intValue());});
        return rows;
    }

    public boolean allActive(List<String> usernames) {
        if(usernames.isEmpty())return false;
        String marks=String.join(",",java.util.Collections.nCopies(usernames.size(),"?"));
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE enabled=1 AND username IN ("+marks+")",Integer.class,usernames.toArray());
        return count!=null&&count==usernames.size();
    }
}
