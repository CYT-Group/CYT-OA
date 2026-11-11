package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.core.security.Principal;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.me.service.MeTeamService;
import com.cyt.center.module.me.mapper.NotificationMapper;
import com.cyt.center.module.me.model.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class WorkspaceIdentity {
    private final CurrentUser current;
    private final HrmEmployeeService employees;
    private final MeTeamService teams;
    private final JdbcTemplate jdbc;
    private final NotificationMapper notifications;
    public Principal principal(){return current.require();}
    public long user(){return principal().userId();}
    public boolean admin(){return principal().isAdmin()||principal().has("*:*:*");}
    public boolean can(String permission){return principal().has(permission);}
    public void require(String permission){if(!can(permission))throw AuthException.forbidden("没有该业务的操作权限");}
    public void personal(){var p=principal();if(!admin()&&p.roles().contains("hr")&&p.roles().stream().allMatch(r->Set.of("hr","employee").contains(r)))throw AuthException.forbidden("此账号仅可使用人事工作区");}
    public Long dept(){var e=employees.getEmployeeByUserId(user());return e==null?null:e.getDeptId();}
    public Long employee(){var e=employees.getEmployeeByUserId(user());return e==null?null:e.getId();}
    public List<Long> team(){return teams.myTeam(user()).stream().map(e->e.getUserId()).toList();}
    public boolean manages(long userId){return team().contains(userId);}
    public List<Long> teamAndSelf(){var result=new ArrayList<>(team());result.add(user());return result;}
    public List<Long> allUsers(){return jdbc.queryForList("SELECT id FROM sys_user WHERE enabled=1 ORDER BY id",Long.class);}
    public List<Long> deptUsers(Long dept){return dept==null?List.of():jdbc.queryForList("SELECT id FROM sys_user WHERE enabled=1 AND main_dept_id=? ORDER BY id",Long.class,dept);}
    public boolean active(long id){return jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id=? AND enabled=1",Long.class,id)>0;}
    public String name(long id){var rows=jdbc.queryForList("SELECT name FROM sys_user WHERE id=?",id);return rows.isEmpty()?"已停用账号":Objects.toString(rows.getFirst().get("name"),"");}
    public Map<String,Object> context(){var p=principal();var out=new LinkedHashMap<String,Object>();out.put("userId",String.valueOf(user()));out.put("name",p.name());out.put("admin",admin());out.put("canManageRules",can("can_manage_workspace_rules"));out.put("canManageVoice",can("can_manage_voice"));out.put("teamCount",team().size());out.put("deptId",dept()==null?null:String.valueOf(dept()));return out;}
    public List<Map<String,Object>> options(){
        var out=new ArrayList<Map<String,Object>>();
        for(var row:jdbc.queryForList("SELECT u.id,u.name,d.name dept_name FROM sys_user u LEFT JOIN sys_dept d ON d.id=u.main_dept_id WHERE u.enabled=1 ORDER BY u.name LIMIT 1000"))
            out.add(Map.of("value",String.valueOf(row.get("id")),"label",Objects.toString(row.get("name"),"")+" · "+Objects.toString(row.get("dept_name"),"未分配部门")));
        return out;
    }
    public void notify(long recipient,String type,long id,String title,String link){
        Notification n=new Notification();n.setUserId(recipient);n.setCategory("work");n.setType(type);n.setTitle(title);n.setLink(link);n.setReadFlag(false);n.setChannel("in_app");n.setDedupKey("workspace:"+type+":"+id+":"+recipient);n.setCreatedAt(LocalDateTime.now());
        try{notifications.insert(n);}catch(org.springframework.dao.DuplicateKeyException ignored){}
    }
}
