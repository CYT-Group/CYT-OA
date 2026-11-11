package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static com.cyt.center.module.portal.service.workspace.WorkspaceValidation.*;

@Service @RequiredArgsConstructor
public class WorkspaceSurveyService {
    private final PortalWorkspaceStore store;
    private final WorkspaceIdentity identity;
    private final JdbcTemplate jdbc;
    public List<Map<String,Object>> list(boolean manage){
        if(manage)identity.require("can_manage_voice");else identity.personal();
        return store.list("survey").stream().filter(r->manage||eligible(r,identity.user()))
                .map(r->view(r,manage)).toList();
    }
    @Transactional
    public Map<String,Object> save(Long id,Map<String,Object> body){
        identity.require("can_manage_voice");var old=id==null?null:store.get("survey",id,true);
        if(old!=null&&!old.status().equals("draft"))throw new BizException("已发布问卷不能修改题目，请新建问卷");
        var data=validate(body);String title=text(body,"title",255,true);long saved;
        if(old==null)saved=store.insert("survey",null,identity.user(),identity.dept(),null,title,"draft",data);
        else{store.update(old,version(body),title,"draft",data,null);saved=old.id();}
        return view(store.get("survey",saved,false),true);
    }
    @Transactional
    public Map<String,Object> publish(long id){
        identity.require("can_manage_voice");var row=store.get("survey",id,true);
        if(!row.status().equals("draft"))throw new BizException("只有草稿问卷可以发布");
        var data=new LinkedHashMap<>(row.data());
        var audience=new LinkedHashSet<Long>();String scope=(String)data.get("scope");
        if(scope.equals("all"))audience.addAll(identity.allUsers());
        else if(scope.equals("department"))for(long dept:ids(data.get("targetIds"),1000))audience.addAll(identity.deptUsers(dept));
        else for(long user:ids(data.get("targetIds"),1000)){if(!identity.active(user))throw new BizException("受访人员中含停用账号，请重新选择");audience.add(user);}
        if(audience.isEmpty())throw new BizException("发放范围内没有有效人员");
        if(!time(data,"endAt").isAfter(LocalDateTime.now()))throw new BizException("问卷结束时间必须晚于当前时间");
        data.put("audience",audience.stream().map(String::valueOf).toList());
        store.update(row,row.version(),row.title(),"published",data,null);
        for(long recipient:audience)identity.notify(recipient,"survey",id,"待填写问卷："+row.title(),"/me/voice?channel=survey");
        return view(store.get("survey",id,false),true);
    }
    @Transactional
    public void close(long id){identity.require("can_manage_voice");var row=store.get("survey",id,true);if(!row.status().equals("published"))throw new BizException("该问卷未在发布中");store.update(row,row.version(),row.title(),"closed",row.data(),null);}
    @Transactional
    public int remind(long id){
        identity.require("can_manage_voice");var row=store.get("survey",id,true);
        if(!active(row))throw new BizException("只能提醒正在收集的问卷");
        var answered=new HashSet<>(jdbc.queryForList("SELECT user_id FROM portal_workspace_answers WHERE survey_id=?",Long.class,id));int count=0;
        for(long user:ids(row.data().get("audience"),10000))if(!answered.contains(user)){identity.notify(user,"survey-remind-"+LocalDate.now(),id,"问卷填写提醒："+row.title(),"/me/voice?channel=survey");count++;}return count;
    }
    @Transactional
    public void answer(long id,Map<String,Object> answers){
        identity.personal();var survey=store.get("survey",id,true);
        if(!eligible(survey,identity.user())||!active(survey))throw AuthException.forbidden("问卷未开始、已结束或不在发放范围内");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM portal_workspace_answers WHERE survey_id=? AND user_id=?",Long.class,id,identity.user())>0)throw new BizException("你已提交过此问卷");
        var clean=validateAnswers(survey.data(),answers);
        jdbc.update("INSERT INTO portal_workspace_answers(survey_id,user_id,answers) VALUES(?,?,?)",id,identity.user(),store.encode(clean));
    }
    public Map<String,Object> results(long id){
        identity.require("can_manage_voice");var survey=store.get("survey",id,false);
        var rows=jdbc.queryForList("SELECT answers,created_at FROM portal_workspace_answers WHERE survey_id=? ORDER BY id",id);
        var answers=rows.stream().map(r->store.decode(String.valueOf(r.get("answers")))).toList();
        var result=new ArrayList<Map<String,Object>>();
        for(var q:objects(survey.data().get("questions"),30,"题目")){
            String key=(String)q.get("id"),type=(String)q.get("type");var item=new LinkedHashMap<>(q);var counts=new LinkedHashMap<String,Integer>();var texts=new ArrayList<String>();double sum=0;int n=0;
            for(var answer:answers){Object v=answer.get(key);if(v==null)continue;if(type.equals("text")){if(!v.toString().isBlank())texts.add(v.toString());}else if(v instanceof List<?> list){for(Object choice:list)counts.merge(choice.toString(),1,Integer::sum);}else{counts.merge(v.toString(),1,Integer::sum);if(type.equals("rating")){sum+=((Number)v).doubleValue();n++;}}}
            item.put("counts",counts);item.put("texts",texts);item.put("average",n==0?null:Math.round(sum/n*100)/100.0);result.add(item);
        }
        return Map.of("survey",view(survey,true),"questions",result,"responses",rows.size());
    }
    boolean active(PortalWorkspaceStore.Row row){var now=LocalDateTime.now();return row.status().equals("published")&&!now.isBefore(time(row.data(),"startAt"))&&now.isBefore(time(row.data(),"endAt"));}
    boolean eligible(PortalWorkspaceStore.Row row,long user){return !row.status().equals("draft")&&ids(row.data().getOrDefault("audience",List.of()),10000).contains(user);}
    Map<String,Object> view(PortalWorkspaceStore.Row row,boolean manage){
        var out=store.view(row);var data=new LinkedHashMap<>(row.data());var audience=ids(data.getOrDefault("audience",List.of()),10000);data.remove("audience");if(!manage){data.remove("targetIds");out.remove("ownerId");}
        out.put("data",data);out.put("active",active(row));out.put("audienceCount",audience.size());
        out.put("responseCount",jdbc.queryForObject("SELECT COUNT(*) FROM portal_workspace_answers WHERE survey_id=?",Long.class,row.id()));
        out.put("answered",jdbc.queryForObject("SELECT COUNT(*) FROM portal_workspace_answers WHERE survey_id=? AND user_id=?",Long.class,row.id(),identity.user())>0);return out;
    }
    static Map<String,Object> validate(Map<String,Object> body){
        var start=time(body,"startAt");var end=time(body,"endAt");if(!end.isAfter(start))throw new BizException("问卷结束时间须晚于开始时间");
        String scope=text(body,"scope",20,true);oneOf(scope,"all","department","users");var targets=ids(body.getOrDefault("targetIds",List.of()),1000);if(!scope.equals("all")&&targets.isEmpty())throw new BizException("请选择发放范围");
        var questions=objects(body.get("questions"),30,"题目");if(questions.isEmpty())throw new BizException("问卷至少需要一道题目");var normalized=new ArrayList<Map<String,Object>>();var seen=new HashSet<String>();
        for(var q:questions){String key=text(q,"id",40,true);if(!key.matches("[a-zA-Z0-9_-]+")||!seen.add(key))throw new BizException("题目标识不能重复");String type=text(q,"type",20,true);oneOf(type,"rating","single","multiple","text");String title=text(q,"title",255,true);boolean required=!Boolean.FALSE.equals(q.get("required"));var options=new ArrayList<String>();
            if(type.equals("single")||type.equals("multiple")){if(!(q.get("options") instanceof List<?> list)||list.size()<2||list.size()>20)throw new BizException("选择题需要 2 至 20 个选项");for(Object value:list){if(!(value instanceof String s)||s.isBlank()||s.length()>200||options.contains(s.trim()))throw new BizException("选项不能为空或重复");options.add(s.trim());}}
            normalized.add(Map.of("id",key,"title",title,"type",type,"required",required,"options",options));
        }
        return new LinkedHashMap<>(Map.of("description",text(body,"description",4000,false),"startAt",start.toString(),"endAt",end.toString(),"scope",scope,"targetIds",targets.stream().map(String::valueOf).toList(),"anonymous",!Boolean.FALSE.equals(body.get("anonymous")),"questions",normalized));
    }
    static Map<String,Object> validateAnswers(Map<String,Object> survey,Map<String,Object> answers){
        var clean=new LinkedHashMap<String,Object>();var allowed=new HashSet<String>();
        for(var q:objects(survey.get("questions"),30,"题目")){
            String key=(String)q.get("id"),type=(String)q.get("type");allowed.add(key);Object value=answers.get(key);boolean empty=value==null||(value instanceof String s&&s.isBlank())||(value instanceof List<?> l&&l.isEmpty());
            if(empty){if(Boolean.TRUE.equals(q.get("required")))throw new BizException("请回答："+q.get("title"));continue;}
            switch(type){case "rating"->{double n=decimal(value,1,5,"评分");if(n!=Math.rint(n))throw new BizException("评分必须为整数");clean.put(key,(int)n);}case "text"->{if(!(value instanceof String s)||s.length()>4000)throw new BizException("文本答案最多 4000 字");clean.put(key,s.trim());}case "single"->{if(!(value instanceof String s)||!((List<?>)q.get("options")).contains(s))throw new BizException("请选择有效选项");clean.put(key,s);}case "multiple"->{if(!(value instanceof List<?> list)||list.isEmpty()||new HashSet<>(list).size()!=list.size()||!((List<?>)q.get("options")).containsAll(list))throw new BizException("请选择有效选项");clean.put(key,list);}default->throw new BizException("题型无效");}
        }
        if(!allowed.containsAll(answers.keySet()))throw new BizException("答案包含未知题目");return clean;
    }
}
