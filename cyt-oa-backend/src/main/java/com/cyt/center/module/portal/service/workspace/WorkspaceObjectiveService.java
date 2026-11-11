package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.portal.service.support.PortalAudit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import static com.cyt.center.module.portal.service.workspace.WorkspaceValidation.*;

@Service @RequiredArgsConstructor
public class WorkspaceObjectiveService {
    private final PortalWorkspaceStore store;
    private final WorkspaceIdentity identity;
    private final WorkspacePolicyService policies;
    private final PortalAudit audit;

    public List<Map<String,Object>> list(String period,boolean alignment){
        identity.personal();String selected=period==null||period.isBlank()?currentPeriod():period;
        if(!selected.matches("20\\d{2}-Q[1-4]"))throw new BizException("目标周期格式为 2026-Q4");
        var users=policies.scopeUsers("okr");
        String ownership=users.isEmpty()?"1=0":"owner_id IN ("+String.join(",",Collections.nCopies(users.size(),"?"))+")";
        var args=new ArrayList<Object>();args.add(selected);args.addAll(users);
        if(alignment){ownership="("+ownership+" OR JSON_UNQUOTE(JSON_EXTRACT(payload,'$.scope'))='company' OR (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.scope'))='department' AND dept_id=?))";args.add(identity.dept());}
        return store.select("objective","JSON_UNQUOTE(JSON_EXTRACT(payload,'$.period'))=? AND ("+ownership+")",args).stream().map(this::view).toList();
    }
    boolean visible(PortalWorkspaceStore.Row row,List<Long> users){
        if(users.contains(row.ownerId()))return true;
        String scope=Objects.toString(row.data().get("scope"),"personal");
        return scope.equals("company")||(scope.equals("department")&&row.deptId()!=null&&Objects.equals(row.deptId(),identity.dept()));
    }
    @Transactional
    public Map<String,Object> save(Long recordId,Map<String,Object> body){
        identity.personal();var old=recordId==null?null:store.get("objective",recordId,true);
        if(old!=null&&old.ownerId()!=identity.user()&&!identity.admin())throw AuthException.forbidden("只能维护自己的目标");
        var data=validate(body);String scope=(String)data.get("scope");
        if(scope.equals("company")&&!identity.admin())throw AuthException.forbidden("公司目标由超级管理员维护");
        if(scope.equals("department")&&!identity.admin()&&identity.team().isEmpty())throw AuthException.forbidden("部门目标由团队负责人维护");
        Long parent=body.get("parentId")==null||body.get("parentId").toString().isBlank()?null:id(body.get("parentId"));
        if(parent!=null){
            var upper=store.get("objective",parent,false);
            if(!visible(upper,policies.scopeUsers("okr"))||!Objects.equals(upper.data().get("period"),data.get("period")))throw new BizException("请选择同一周期内可见的上级目标");
            if(Objects.equals(parent,recordId))throw new BizException("目标不能对齐自身");
            var seen=new HashSet<Long>();
            for(var node=upper;node!=null;){
                if(!seen.add(node.id())||Objects.equals(node.id(),recordId))throw new BizException("目标对齐不能形成循环");
                node=node.parentId()==null?null:store.get("objective",node.parentId(),false);
            }
        }
        String status=text(body,"status",20,true);oneOf(status,"active","completed","archived");
        if(status.equals("completed")&&progress(data)<100)throw new BizException("关键结果全部完成后才能完成目标");
        String title=text(body,"title",255,true);long saved;
        if(old==null)saved=store.insert("objective",null,identity.user(),identity.dept(),parent,title,status,data);
        else{store.update(old,version(body),title,status,data,parent);saved=old.id();}
        audit.record(identity.user(),"objective_save","portal_workspace_records",saved,old==null?null:store.encode(old.data()),store.encode(data),title);
        return view(store.get("objective",saved,false));
    }
    @Transactional
    public void delete(long id){
        identity.personal();var row=store.get("objective",id,true);
        if(row.ownerId()!=identity.user()&&!identity.admin())throw AuthException.forbidden("只能删除自己的目标");
        if(store.list("objective").stream().anyMatch(r->Objects.equals(r.parentId(),id)))throw new BizException("请先调整下级目标的对齐关系");
        store.delete(row);audit.record(identity.user(),"objective_delete","portal_workspace_records",id,store.encode(row.data()),null,row.title());
    }
    Map<String,Object> view(PortalWorkspaceStore.Row row){var value=store.view(row);value.put("progress",progress(row.data()));value.put("ownerName",identity.name(row.ownerId()));value.put("editable",identity.admin()||row.ownerId()==identity.user());return value;}
    public static String currentPeriod(){var day=LocalDate.now();return day.getYear()+"-Q"+((day.getMonthValue()-1)/3+1);}
    static Map<String,Object> validate(Map<String,Object> body){
        String period=text(body,"period",7,true);if(!period.matches("20\\d{2}-Q[1-4]"))throw new BizException("目标周期格式为 2026-Q4");
        String scope=text(body,"scope",20,true);oneOf(scope,"personal","department","company");
        var results=objects(body.get("keyResults"),20,"关键结果");if(results.isEmpty())throw new BizException("目标至少需要一个关键结果");
        var normalized=new ArrayList<Map<String,Object>>();
        for(var kr:results){String title=text(kr,"title",255,true);double target=decimal(kr.get("target"),0.0001,1e12,"目标值");double current=decimal(kr.get("current"),0,target,"当前值");double weight=decimal(kr.get("weight"),0.01,100,"权重");normalized.add(Map.of("title",title,"target",target,"current",current,"weight",weight,"unit",text(kr,"unit",30,false)));}
        return new LinkedHashMap<>(Map.of("period",period,"scope",scope,"description",text(body,"description",4000,false),"keyResults",normalized));
    }
    static double progress(Map<String,Object> data){double sum=0,weights=0;for(var kr:objects(data.get("keyResults"),20,"关键结果")){double weight=((Number)kr.get("weight")).doubleValue();sum+=((Number)kr.get("current")).doubleValue()/((Number)kr.get("target")).doubleValue()*weight;weights+=weight;}return weights==0?0:Math.round(sum/weights*1000)/10.0;}
}
