package com.cyt.center.module.portal.dal.mysql.celebrations;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PortalCelebrationRepository {
    private final JdbcTemplate jdbc;
    public List<Map<String,Object>> employees(String q) {
        return jdbc.queryForList("""
            SELECT u.id AS userId,u.username,u.wecom_userid AS wecomUserid,u.name,u.enabled,
                   d.name AS department,e.birthday,e.entry_time AS hireDate,COALESCE(p.opt_out,0) AS optOut,
                   COALESCE(p.birthday_override,0) AS birthdayOverride,p.birthday_month_day AS birthdayMonthDay
            FROM sys_user u LEFT JOIN sys_dept d ON d.id=u.main_dept_id
            LEFT JOIN hrm_employee e ON e.user_id=u.id AND e.deleted=0
            LEFT JOIN portal_celebration_preferences p ON p.user_id=u.id
            WHERE (?='' OR u.name LIKE ? OR u.username LIKE ?) ORDER BY u.id
            """,q==null?"":q,"%"+(q==null?"":q)+"%","%"+(q==null?"":q)+"%");
    }
    public Map<String,Object> settings() {
        return jdbc.queryForMap("SELECT enabled,send_hour AS sendHour,send_minute AS sendMinute,birthday_enabled AS birthdayEnabled,birthday_card_enabled AS birthdayCardEnabled,templates,updated_by AS updatedBy,updated_at AS updatedAt FROM portal_celebration_settings WHERE id=1");
    }
    public void settings(boolean enabled,int hour,int minute,boolean birthday,boolean card,String templates,String actor) {
        jdbc.update("UPDATE portal_celebration_settings SET enabled=?,send_hour=?,send_minute=?,birthday_enabled=?,birthday_card_enabled=?,templates=?,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=1",enabled,hour,minute,birthday,card,templates,actor);
    }
    public void preference(long userId,boolean optOut) { jdbc.update("INSERT INTO portal_celebration_preferences(user_id,opt_out) VALUES(?,?) ON DUPLICATE KEY UPDATE opt_out=VALUES(opt_out)",userId,optOut); }
    public void employee(long userId,String birthday,java.time.LocalDate hireDate,boolean optOut) {
        var rows=jdbc.queryForList("SELECT id FROM hrm_employee WHERE user_id=? AND deleted=0 ORDER BY id FOR UPDATE",Long.class,userId);
        if(rows.size()!=1)throw new com.cyt.center.core.exception.BizException("该员工尚未建立唯一人事档案，请先完善员工档案");
        // 只改显式维护的人事日期，企微同步基线不变，下次同步将识别人工更正冲突。
        jdbc.update("UPDATE hrm_employee SET entry_time=?,update_time=CURRENT_TIMESTAMP WHERE id=?",hireDate==null?null:hireDate.atStartOfDay(),rows.getFirst());
        jdbc.update("INSERT INTO portal_celebration_preferences(user_id,opt_out,birthday_override,birthday_month_day) VALUES(?,?,1,?) ON DUPLICATE KEY UPDATE opt_out=VALUES(opt_out),birthday_override=1,birthday_month_day=VALUES(birthday_month_day)",userId,optOut,birthday);
    }
    public List<Map<String,Object>> runs() { return jdbc.queryForList("SELECT id,run_date AS runDate,kind,status,detail,created_at AS createdAt FROM portal_celebration_runs ORDER BY id DESC LIMIT 20"); }
    public boolean claim(LocalDate day,String kind) { return jdbc.update("INSERT IGNORE INTO portal_celebration_runs(run_date,kind,status) VALUES(?,?,'failed')",day,kind)==1; }
    public void finish(LocalDate day,String kind,String status,String detail) { jdbc.update("UPDATE portal_celebration_runs SET status=?,detail=? WHERE run_date=? AND kind=?",status,detail,day,kind); }
    public void log(Long userId,String kind,String title,boolean ok,String error) { jdbc.update("INSERT INTO portal_celebration_push_log(user_id,kind,title,sent_ok,error_msg) VALUES(?,?,?,?,?)",userId,kind,title,ok,error); }
    public List<Map<String,Object>> logs(int offset,int size) { return jdbc.queryForList("SELECT id,user_id AS userId,kind,title,sent_ok AS sentOk,error_msg AS errorMsg,created_at AS createdAt FROM portal_celebration_push_log ORDER BY id DESC LIMIT ? OFFSET ?",size,offset); }
    public long logCount() { return jdbc.queryForObject("SELECT COUNT(*) FROM portal_celebration_push_log",Long.class); }
    public List<Long> hrRecipients() { return jdbc.queryForList("SELECT DISTINCT u.id FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id LEFT JOIN sys_role_menu rm ON rm.role_id=r.id LEFT JOIN sys_menu m ON m.id=rm.menu_id WHERE u.enabled=1 AND (r.code IN ('admin','hr') OR m.permission='can_manage_celebrations') AND u.wecom_userid IS NOT NULL",Long.class); }
}
