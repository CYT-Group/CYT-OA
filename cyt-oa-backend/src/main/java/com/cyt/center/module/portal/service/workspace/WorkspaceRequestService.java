package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.portal.service.admin.PortalRows;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.cyt.center.module.portal.service.workspace.WorkspaceValidation.*;

@Service @RequiredArgsConstructor
public class WorkspaceRequestService {
    private final PortalWorkspaceStore store;
    private final WorkspaceIdentity identity;
    private final WorkspacePolicyService policies;
    private final JdbcTemplate jdbc;
    public static final Map<String,String> TYPES=Map.of("leave","请假","expense","报销","travel","出差","purchase","采购","seal","用章","overtime","加班");

    public List<Map<String,Object>> list(String view,String type,String status){
        identity.personal();oneOf(view,"mine","history","pending","processed","team","company");
        var users=view.equals("company")?policies.scopeUsers("approvals"):view.equals("team")?identity.teamAndSelf():List.of(identity.user());
        if(view.equals("company")&&!identity.admin()&&users.size()==1)throw AuthException.forbidden("没有全公司审批数据权限");
        if(view.equals("team")&&identity.team().isEmpty()&&!identity.admin())throw AuthException.forbidden("没有团队数据权限");
        String where;var args=new ArrayList<Object>();
        if(view.equals("pending")||view.equals("processed")){
            where="EXISTS (SELECT 1 FROM portal_workspace_approval_steps s WHERE s.request_id=portal_workspace_records.id AND s.approver_id=? AND "+(view.equals("pending")?"s.status='pending'":"s.status IN ('approved','rejected')")+")";
            args.add(identity.user());
        }else if(view.equals("history")){
            where="owner_id=? AND status IN ('approved','rejected','cancelled')";args.add(identity.user());
        }else{
            where=users.isEmpty()?"1=0":"owner_id IN ("+String.join(",",Collections.nCopies(users.size(),"?"))+")";args.addAll(users);
        }
        if(type!=null&&!type.isBlank()){if(!TYPES.containsKey(type))throw new BizException("申请类型无效");where+=" AND JSON_UNQUOTE(JSON_EXTRACT(payload,'$.type'))=?";args.add(type);}
        if(status!=null&&!status.isBlank()){oneOf(status,"draft","pending","approved","rejected","cancelled");where+=" AND status=?";args.add(status);}
        return store.select("request",where,args).stream().map(this::view).toList();
    }
    public Map<String,Object> get(long id){var row=store.get("request",id,false);requireVisible(row);return view(row);}
    private void requireVisible(PortalWorkspaceStore.Row row){
        identity.personal();if(row.ownerId()==identity.user()||policies.scopeUsers("approvals").contains(row.ownerId())||identity.team().contains(row.ownerId()))return;
        if(jdbc.queryForObject("SELECT COUNT(*) FROM portal_workspace_approval_steps WHERE request_id=? AND approver_id=?",Long.class,row.id(),identity.user())==0)throw AuthException.forbidden("没有此申请的查看权限");
    }
    @Transactional
    public Map<String,Object> save(Long id,Map<String,Object> body,boolean submit){
        identity.personal();var old=id==null?null:store.get("request",id,true);
        if(old!=null&&(old.ownerId()!=identity.user()||!old.status().equals("draft")))throw AuthException.forbidden("只能修改自己的草稿申请");
        var data=validate(body,policies.data("attendance"));if(data.get("type").equals("expense"))validateExpense(data,policies.data("expense"));String title=text(body,"title",255,true);long saved;
        if(old==null)saved=store.insert("request",null,identity.user(),identity.dept(),null,title,"draft",data);
        else{store.update(old,version(body),title,"draft",data,null);saved=old.id();}
        if(submit)submitInternal(saved);return view(store.get("request",saved,false));
    }
    private void submitInternal(long id){
        var row=store.get("request",id,true);String type=(String)row.data().get("type");var approvers=policies.resolveApprovers(type);
        if(type.equals("expense"))validateExpense(row.data(),policies.data("expense"));
        // 同一申请人的额度预占串行，避免两张并发请假单同时通过额度检查。
        jdbc.queryForList("SELECT id FROM sys_user WHERE id=? FOR UPDATE",identity.user());
        if(type.equals("leave"))validateQuota(row);
        for(int i=0;i<approvers.size();i++)jdbc.update("INSERT INTO portal_workspace_approval_steps(request_id,step_no,approver_id,status) VALUES(?,?,?,?)",id,i+1,approvers.get(i),i==0?"pending":"waiting");
        store.update(row,row.version(),row.title(),"pending",row.data(),null);
        identity.notify(approvers.getFirst(),"approval",id,"待审批："+row.title(),"/me/requests?view=pending&requestId="+id);
    }
    @Transactional
    public Map<String,Object> act(long id,boolean approve,String comment){
        identity.personal();if(comment==null||comment.length()>1000||(!approve&&comment.isBlank()))throw new BizException("请填写审批意见，最多 1000 字");
        var row=store.get("request",id,true);if(!row.status().equals("pending"))throw new BizException("申请状态已变化，请刷新");
        var steps=jdbc.queryForList("SELECT * FROM portal_workspace_approval_steps WHERE request_id=? AND status='pending' FOR UPDATE",id);
        if(steps.size()!=1||((Number)steps.getFirst().get("approver_id")).longValue()!=identity.user())throw AuthException.forbidden("当前节点不由你审批");
        long step=((Number)steps.getFirst().get("id")).longValue();jdbc.update("UPDATE portal_workspace_approval_steps SET status=?,comment=?,acted_at=NOW() WHERE id=?",approve?"approved":"rejected",comment.trim(),step);
        String status;
        if(!approve){status="rejected";jdbc.update("UPDATE portal_workspace_approval_steps SET status='cancelled' WHERE request_id=? AND status='waiting'",id);}
        else{
            var next=jdbc.queryForList("SELECT * FROM portal_workspace_approval_steps WHERE request_id=? AND status='waiting' ORDER BY step_no LIMIT 1 FOR UPDATE",id);
            status=next.isEmpty()?"approved":"pending";
            if(!next.isEmpty()){var n=next.getFirst();jdbc.update("UPDATE portal_workspace_approval_steps SET status='pending' WHERE id=?",n.get("id"));identity.notify(((Number)n.get("approver_id")).longValue(),"approval",id,"待审批："+row.title(),"/me/requests?view=pending&requestId="+id);}
        }
        store.update(row,row.version(),row.title(),status,row.data(),null);
        if(!status.equals("pending"))identity.notify(row.ownerId(),"approval-result",id,row.title()+"："+(approve?"已通过":"已驳回"),"/me/requests?view=mine&requestId="+id);
        return view(store.get("request",id,false));
    }
    @Transactional
    public void cancel(long id){identity.personal();var row=store.get("request",id,true);if(row.ownerId()!=identity.user()||!Set.of("draft","pending").contains(row.status()))throw AuthException.forbidden("只能撤销自己的草稿或审批中申请");jdbc.update("UPDATE portal_workspace_approval_steps SET status='cancelled' WHERE request_id=? AND status IN ('pending','waiting')",id);store.update(row,row.version(),row.title(),"cancelled",row.data(),null);}
    private void validateQuota(PortalWorkspaceStore.Row row){
        String kind=String.valueOf(row.data().get("leaveKind"));double allowance=((Number)policies.data("leave").get(kind)).doubleValue();
        var existing=store.select("request","id<>? AND owner_id=? AND status IN ('pending','approved') AND JSON_UNQUOTE(JSON_EXTRACT(payload,'$.type'))='leave' AND JSON_UNQUOTE(JSON_EXTRACT(payload,'$.leaveKind'))=?",List.of(row.id(),row.ownerId(),kind));
        var allocations=object(row.data().get("allocations"));
        for(var allocation:allocations.entrySet()){double used=0;for(var other:existing)used+=((Number)object(other.data().get("allocations")).getOrDefault(allocation.getKey(),0)).doubleValue();if(used+((Number)allocation.getValue()).doubleValue()>allowance+0.0001)throw new BizException(allocation.getKey()+" 年该假种余额不足（含审批中预占），剩余 "+Math.max(0,Math.round((allowance-used)*100)/100.0)+" 天");}
    }
    Map<String,Object> view(PortalWorkspaceStore.Row row){
        var out=store.view(row);out.put("ownerName",identity.name(row.ownerId()));out.put("typeText",TYPES.get(row.data().get("type")));
        var steps=PortalRows.camel(jdbc.queryForList("SELECT step_no,approver_id,status,comment,acted_at FROM portal_workspace_approval_steps WHERE request_id=? ORDER BY step_no",row.id()));
        for(var step:steps){long person=((Number)step.get("approverId")).longValue();step.put("approverName",identity.name(person));step.put("approverId",String.valueOf(person));}
        out.put("steps",steps);out.put("canApprove",row.status().equals("pending")&&steps.stream().anyMatch(s->String.valueOf(identity.user()).equals(s.get("approverId"))&&s.get("status").equals("pending")));return out;
    }
    static Map<String,Object> validate(Map<String,Object> body,Map<String,Object> attendance){
        String type=text(body,"type",20,true);if(!TYPES.containsKey(type))throw new BizException("请选择有效的申请类型");
        var data=new LinkedHashMap<String,Object>();data.put("type",type);data.put("reason",text(body,"reason",4000,true));
        if(Set.of("leave","travel","overtime").contains(type)){
            var start=time(body,"startAt");var end=time(body,"endAt");if(!end.isAfter(start)||ChronoUnit.DAYS.between(start,end)>366)throw new BizException("请选择一年以内且结束晚于开始的日期范围");data.put("startAt",start.toString());data.put("endAt",end.toString());
            if(type.equals("leave")){String leave=text(body,"leaveKind",20,true);oneOf(leave,"annual","sick","personal","compensatory");data.put("leaveKind",leave);var allocation=leaveDays(start,end,attendance);if(allocation.values().stream().mapToDouble(Number::doubleValue).sum()<=0)throw new BizException("请假区间内没有工作时间");data.put("allocations",allocation);data.put("days",Math.round(allocation.values().stream().mapToDouble(Number::doubleValue).sum()*100)/100.0);}
        }
        if(Set.of("expense","travel","purchase").contains(type))data.put("amount",decimal(body.get("amount"),0.01,100000000,"申请金额"));
        if(type.equals("travel"))data.put("destination",text(body,"destination",255,true));
        if(type.equals("expense")){String expenseKind=text(body,"expenseKind",50,true);oneOf(expenseKind,"hotel","transport","cityTransport","hospitality");data.put("expenseKind",expenseKind);data.put("currency","CNY");}
        if(type.equals("purchase"))data.put("items",text(body,"items",4000,true));
        if(type.equals("seal")){data.put("sealType",text(body,"sealType",100,true));data.put("documentName",text(body,"documentName",255,true));double copies=decimal(body.get("copies"),1,10000,"份数");if(copies!=Math.rint(copies))throw new BizException("份数必须为整数");data.put("copies",(int)copies);}
        return data;
    }
    static void validateExpense(Map<String,Object> data,Map<String,Object> rules){
        String kind=Objects.toString(data.get("expenseKind"),"");oneOf(kind,"hotel","transport","cityTransport","hospitality");
        double amount=decimal(data.get("amount"),0.01,100000000,"报销金额"),limit=decimal(rules.get(kind),0,100000,"单次报销标准");
        String label=Map.of("hotel","住宿","transport","差旅交通","cityTransport","市内交通","hospitality","业务招待").get(kind);
        if(amount>limit)throw new BizException(label+"单次报销金额不能超过 "+java.math.BigDecimal.valueOf(limit).stripTrailingZeros().toPlainString()+" 元（CNY），请调整金额或联系管理员更新报销标准");
    }
    static Map<String,Double> leaveDays(LocalDateTime start,LocalDateTime end,Map<String,Object> rules){
        LocalTime am=LocalTime.parse(String.valueOf(rules.get("startTime"))),pm=LocalTime.parse(String.valueOf(rules.get("endTime")));
        var byYear=new LinkedHashMap<String,Double>();double workingMinutes=ChronoUnit.MINUTES.between(am,pm)-60;if(workingMinutes<=0)workingMinutes=ChronoUnit.MINUTES.between(am,pm);
        for(LocalDate day=start.toLocalDate();!day.isAfter(end.toLocalDate());day=day.plusDays(1)){
            if(day.getDayOfWeek()==DayOfWeek.SATURDAY||day.getDayOfWeek()==DayOfWeek.SUNDAY)continue;
            var a=start.isAfter(day.atTime(am))?start:day.atTime(am);var b=end.isBefore(day.atTime(pm))?end:day.atTime(pm);double minutes=Math.max(0,ChronoUnit.MINUTES.between(a,b));
            var lunchA=a.isAfter(day.atTime(12,0))?a:day.atTime(12,0);var lunchB=b.isBefore(day.atTime(13,0))?b:day.atTime(13,0);minutes-=Math.max(0,ChronoUnit.MINUTES.between(lunchA,lunchB));if(minutes>0)byYear.merge(String.valueOf(day.getYear()),minutes/workingMinutes,Double::sum);
        }return byYear;
    }
}
