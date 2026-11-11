package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.portal.service.support.PortalAudit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalTime;
import java.util.*;
import static com.cyt.center.module.portal.service.workspace.WorkspaceValidation.*;

@Service
@RequiredArgsConstructor
public class WorkspacePolicyService {
    private final PortalWorkspaceStore store;
    private final WorkspaceIdentity identity;
    private final PortalAudit audit;
    public Map<String,Object> read(String key){
        oneOf(key,"attendance","leave","expense","data-scope","flow-leave","flow-expense","flow-travel","flow-purchase","flow-seal","flow-overtime");
        identity.principal();
        if(key.equals("data-scope")&&!identity.admin())throw AuthException.forbidden("仅超级管理员可维护数据权限");
        var row=store.byKey("policy",key);
        return row==null?Map.of("id","","version",0,"data",defaults(key)):store.view(row);
    }
    public Map<String,Object> data(String key){var row=store.byKey("policy",key);return row==null?defaults(key):row.data();}
    @Transactional
    public Map<String,Object> save(String key,Map<String,Object> body){
        identity.require("can_manage_workspace_rules");
        if(key.equals("data-scope")&&!identity.admin())throw AuthException.forbidden("仅超级管理员可维护数据权限");
        read(key);var data=object(body.get("data"));validate(key,data);
        var old=store.byKey("policy",key);long id;
        if(old==null){if(version(body)!=0)throw new BizException("配置已更新，请刷新重试");id=store.insert("policy",key,identity.user(),identity.dept(),null,key,"active",data);}
        else {old=store.get("policy",old.id(),true);store.update(old,version(body),key,"active",data,null);id=old.id();}
        audit.record(identity.user(),"workspace_policy","portal_workspace_records",id,old==null?null:store.encode(old.data()),store.encode(data),key);
        return store.view(store.get("policy",id,false));
    }
    public List<Long> scopeUsers(String module){
        if(identity.admin())return identity.allUsers();
        var policy=data("data-scope");String scope="self";
        for(String role:identity.principal().roles()){
            Object raw=policy.get(role);if(!(raw instanceof Map<?,?> rules))continue;
            String next=Objects.toString(rules.get(module),"self");
            if(rank(next)>rank(scope))scope=next;
        }
        return switch(scope){case "company"->identity.allUsers();case "department"->identity.deptUsers(identity.dept());case "team"->identity.teamAndSelf();default->List.of(identity.user());};
    }
    public List<Long> resolveApprovers(String type){
        var data=data("flow-"+type);
        if(!Boolean.TRUE.equals(data.get("enabled")))throw new BizException("该申请流程尚未启用，请联系管理员配置");
        var result=new ArrayList<Long>();
        for(var node:objects(data.get("nodes"),10,"审批节点")){
            String kind=text(node,"type",20,true);long who;
            if(kind.equals("leader")){
                var row=storeIdentityLeader();if(row==null)throw new BizException("请先维护直属上级，或由管理员指定审批人");who=row;
            }else who=id(node.get("userId"));
            if(who==identity.user()||!identity.active(who))throw new BizException("审批人须为其他有效账号，请联系管理员调整流程");
            if(!result.contains(who))result.add(who);
        }
        if(result.isEmpty())throw new BizException("审批流程至少需要一个审批节点");return result;
    }
    private Long storeIdentityLeader(){return leaderLookup==null?null:leaderLookup.leader(identity.user());}
    private final WorkspaceLeaderLookup leaderLookup;
    private static int rank(String scope){return switch(scope){case "company"->4;case "department"->3;case "team"->2;default->1;};}
    public static Map<String,Object> defaults(String key){
        if(key.startsWith("flow-"))return new LinkedHashMap<>(Map.of("enabled",false,"nodes",List.of(),"description",""));
        return new LinkedHashMap<>(switch(key){
            case "attendance"->Map.of("startTime","09:00","endTime","18:00","allowRemote",false,"lateMinutes",0,"earlyMinutes",0,"overtimeApproval",true);
            case "leave"->Map.of("annual",5,"sick",10,"personal",10,"compensatory",0);
            case "expense"->Map.of("hotel",500,"transport",300,"cityTransport",100,"hospitality",300,"currency","CNY");
            case "data-scope"->Map.of("employee",Map.of("home","self","reports","self","okr","self","approvals","self"),"manager",Map.of("home","team","reports","team","okr","team","approvals","team"));
            default->throw new BizException("未知规则类型");
        });
    }
    static void validate(String key,Map<String,Object> data){
        if(key.startsWith("flow-")){
            if(!(data.get("enabled") instanceof Boolean))throw new BizException("请设置流程启用状态");
            var nodes=objects(data.get("nodes"),10,"审批节点");
            if(Boolean.TRUE.equals(data.get("enabled"))&&nodes.isEmpty())throw new BizException("启用流程前请至少配置一个审批节点");
            for(var n:nodes){String type=text(n,"type",20,true);oneOf(type,"leader","user");if(type.equals("user"))id(n.get("userId"));}
            text(data,"description",2000,false);return;
        }
        switch(key){
            case "attendance"->{try{var start=LocalTime.parse(text(data,"startTime",5,true));var end=LocalTime.parse(text(data,"endTime",5,true));if(!end.isAfter(start))throw new IllegalArgumentException();}catch(Exception e){throw new BizException("下班时间必须晚于上班时间");}decimal(data.get("lateMinutes"),0,120,"迟到宽限");decimal(data.get("earlyMinutes"),0,120,"早退宽限");for(String b:List.of("allowRemote","overtimeApproval"))if(!(data.get(b) instanceof Boolean))throw new BizException("规则开关格式不正确");}
            case "leave"->{for(String field:List.of("annual","sick","personal","compensatory"))decimal(data.get(field),0,366,"假期额度");}
            case "expense"->{for(String field:List.of("hotel","transport","cityTransport","hospitality"))decimal(data.get(field),0,100000,"报销标准");oneOf(text(data,"currency",3,true),"CNY");}
            case "data-scope"->{if(data.size()>100)throw new BizException("角色配置过多");for(var e:data.entrySet()){if(!e.getKey().matches("[a-z][a-z0-9_-]{1,63}"))throw new BizException("角色标识不正确");var rules=object(e.getValue());for(String module:List.of("home","reports","okr","approvals"))oneOf(text(rules,module,20,true),"self","team","department","company");}}
            default->throw new BizException("未知规则类型");
        }
    }
}
