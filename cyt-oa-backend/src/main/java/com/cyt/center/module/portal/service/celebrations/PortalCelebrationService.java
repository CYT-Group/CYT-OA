package com.cyt.center.module.portal.service.celebrations;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.integration.WecomGatewayClient;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.portal.dal.mysql.celebrations.PortalCelebrationRepository;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PortalCelebrationService {
    private final PortalCelebrationRepository repository;
    private final WecomGatewayClient gateway;
    private final PortalOperatorResolver operators;
    private final PortalAudit audit;
    private final ObjectMapper json;
    private final PortalCelebrationCardService cards;
    @org.springframework.beans.factory.annotation.Value("${cyt.portal.celebrations.scheduler-enabled:false}")
    private boolean schedulerEnabled;
    public static final Map<String,String> DEFAULTS=Map.of(
        "birthday_title","生日快乐，{name}！","birthday_body","{name}，今天是你的生日，公司全体伙伴祝你生日快乐，天天开心！",
        "anniversary_title","入职 {years} 周年快乐！","anniversary_body","{name}，今天是你入职满 {years} 年的日子，感谢一路同行，未来继续并肩！",
        "monthly_title","{month} 月员工庆祝预告（{count} 项）","monthly_intro","{year} 年 {month} 月生日与入职周年清单：",
        "daily_title","今日员工庆祝清单（{count} 项）","daily_intro","{date} 生日与入职周年：");

    public List<Map<String,Object>> employees(String q) {
        return repository.employees(q).stream().map(r -> {
            Map<String,Object> v=new LinkedHashMap<>(r);
            LocalDate birth=date(v.get("birthday")),hire=date(v.get("hireDate"));
            v.put("birthday",flag(v.get("birthdayOverride"))?v.get("birthdayMonthDay"):birth==null?null:String.format("%02d-%02d",birth.getMonthValue(),birth.getDayOfMonth()));
            v.put("hireDate",hire==null?null:hire.toString());v.put("optOut",flag(v.get("optOut")));
            v.remove("wecomUserid");v.remove("birthdayOverride");v.remove("birthdayMonthDay"); return v;
        }).toList();
    }

    public List<Map<String,Object>> calendar(String month) {
        YearMonth ym;
        try { ym=YearMonth.parse(month); } catch(Exception ex) { throw new BizException("月份须为 YYYY-MM"); }
        if(ym.getYear()<1900 || ym.getYear()>2200) throw new BizException("月份超出查询范围");
        List<Map<String,Object>> out=new ArrayList<>();
        for(var u:repository.employees(null)) {
            if(!flag(u.get("enabled"))) continue;
            for(String kind:List.of("birthday","anniversary")) {
                LocalDate original=kind.equals("birthday")&&flag(u.get("birthdayOverride"))
                        ?(u.get("birthdayMonthDay")==null?null:LocalDate.parse("2000-"+u.get("birthdayMonthDay")))
                        :date(u.get(kind.equals("birthday")?"birthday":"hireDate"));
                if(original==null || original.getMonthValue()!=ym.getMonthValue()) continue;
                int years=ym.getYear()-original.getYear();
                if(kind.equals("anniversary") && years<1) continue;
                LocalDate occurs=ym.atDay(Math.min(original.getDayOfMonth(),ym.lengthOfMonth()));
                Map<String,Object> item=new LinkedHashMap<>();
                for(String key:List.of("userId","username","name","department")) item.put(key,u.get(key));
                item.put("kind",kind);item.put("date",occurs.toString());item.put("monthDay",occurs.toString().substring(5));
                item.put("optOut",flag(u.get("optOut")));
                item.put("years",kind.equals("anniversary")?years:null);out.add(item);
            }
        }
        out.sort(Comparator.comparing(v -> v.get("date").toString()));return out;
    }

    public Map<String,Object> settings() {
        Map<String,Object> result=new LinkedHashMap<>(repository.settings());
        for(String key:List.of("enabled","birthdayEnabled","birthdayCardEnabled")) result.put(key,flag(result.get(key)));
        result.put("templates",templates(result.get("templates")));result.put("templateDefaults",DEFAULTS);result.put("runs",repository.runs());
        result.put("schedulerEnabled",schedulerEnabled);
        return result;
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> settings(Long actor,boolean enabled,int hour,int minute,boolean birthday,boolean card,Map<String,String> patch) {
        if(hour<0 || hour>23 || minute<0 || minute>59) throw new BizException("发送时刻不合法");
        Map<String,String> templates=new LinkedHashMap<>(templates(repository.settings().get("templates")));
        if(patch!=null) for(var e:patch.entrySet()) {
            if(!DEFAULTS.containsKey(e.getKey())) throw new BizException("未知的庆祝文案模板");
            if(e.getValue()==null) templates.remove(e.getKey());
            else {
                String value=e.getValue().trim();int cap=e.getKey().endsWith("_title")?100:2000;
                if(value.isBlank() || value.length()>cap) throw new BizException("庆祝文案不能为空且不能超过 "+cap+" 字");
                templates.put(e.getKey(),value);
            }
        }
        repository.settings(enabled,hour,minute,birthday,card,write(templates),operators.resolve(actor).username());
        audit.record(actor,"celebration_settings","portal_celebration_settings",1,null,"enabled="+enabled,null);
        return settings();
    }

    @Transactional(rollbackFor=Exception.class)
    public void preference(Long actor,long userId,boolean optOut) {
        requireEmployee(userId);repository.preference(userId,optOut);
        audit.record(actor,"celebration_preference","portal_celebration_preferences",userId,null,"optOut="+optOut,null);
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> employee(Long actor,long userId,String birthday,String hireDate,boolean optOut) {
        requireEmployee(userId);LocalDate hire=null;
        if(birthday!=null&&!birthday.isBlank()) {try { if(!birthday.matches("\\d{2}-\\d{2}"))throw new Exception();LocalDate.parse("2000-"+birthday); }catch(Exception ex){throw new BizException("生日须为有效的 MM-DD");} }
        else birthday=null;
        if(hireDate!=null&&!hireDate.isBlank()) {try{hire=LocalDate.parse(hireDate);if(hire.isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai"))))throw new Exception();}catch(Exception ex){throw new BizException("入职日期须为有效 YYYY-MM-DD 且不能晚于今天");}}
        repository.employee(userId,birthday,hire,optOut);
        audit.record(actor,"celebration_employee_edit","hrm_employee",userId,null,"日期及庆祝偏好已维护",null);
        return employees(null).stream().filter(r->((Number)r.get("userId")).longValue()==userId).findFirst().orElseThrow();
    }

    /** 只表示网关已接收，不表示员工已读。失败同样保留发送记录。 */
    public Map<String,Object> send(Long actor,long userId,String kind,String title,String message) {
        if(!List.of("birthday","anniversary","custom").contains(kind)) throw new BizException("未知的庆祝类型");
        if(title==null || title.isBlank() || title.length()>100 || message==null || message.isBlank() || message.length()>2000)
            throw new BizException("标题为 1 至 100 字，祝福为 1 至 2000 字");
        return deliver(actor,userId,kind,title,message,false);
    }

    private Map<String,Object> deliver(Long actor,long userId,String kind,String title,String message,boolean hrDigest) {
        Map<String,Object> user=requireEmployee(userId);
        if(!flag(user.get("enabled")) || !hrDigest&&flag(user.get("optOut"))) throw new BizException("该员工已停用或选择不参与庆祝");
        String userid=(String)user.get("wecomUserid");
        if(userid==null || userid.isBlank()) throw new BizException("员工尚未绑定企业微信");
        boolean ok=false;String error=null;
        try {
            for(String chunk:chunks(title+"\n"+message)) {
                var response=gateway.call("wecom/messages/send-text",Map.of("content",chunk,"to_users",List.of(userid)),Duration.ofSeconds(30));
                if(response.has("errcode") && response.path("errcode").asInt()!=0) throw new BizException("企业微信拒绝了消息");
                if(!response.path("invaliduser").asText("").isBlank()) throw new BizException("企业微信未找到收件人");
            }
            ok=true;
        } catch(RuntimeException ex) { error="企微发送失败，请检查 OA 网关授权、应用可见范围和发送记录"; }
        repository.log(userId,kind,title,ok,error);
        audit.record(actor,"celebration_send","portal_celebration_push_log",userId,null,"kind="+kind+",ok="+ok,null);
        Map<String,Object> out=new LinkedHashMap<>();out.put("sentOk",ok);out.put("errorMsg",error);return out;
    }

    public PageResult<Map<String,Object>> logs(int page,int size) {
        int p=Math.max(1,page),s=Math.max(1,Math.min(100,size));
        return new PageResult<>(repository.logCount(),(long)p,(long)s,repository.logs((p-1)*s,s));
    }

    /** 调度器显式启用后才会调用；唯一日期+kind 抢占挡住多实例和重复触发。 */
    public Map<String,Object> runDue() {
        return runDue(false);
    }
    public Map<String,Object> runDue(boolean force) {
        var settings=repository.settings();
        ZonedDateTime now=ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        if(!flag(settings.get("enabled")) || !force&&now.toLocalTime().isBefore(LocalTime.of(((Number)settings.get("sendHour")).intValue(),((Number)settings.get("sendMinute")).intValue())))
            return Map.of("skipped",true);
        LocalDate day=now.toLocalDate();
        Map<String,String> texts=new LinkedHashMap<>(DEFAULTS);texts.putAll(templates(settings.get("templates")));
        List<Map<String,Object>> all=calendar(YearMonth.from(day).toString());
        Map<String,Object> result=new LinkedHashMap<>();result.put("date",day.toString());
        if(day.getDayOfMonth()==1&&repository.claim(day,"monthly_digest")) {
            Map<String,Object> monthly=digest(day,all,texts,"monthly");
            repository.finish(day,"monthly_digest",((Number)monthly.get("failed")).intValue()==0?"ok":"partial",write(monthly));result.put("monthlyDigest",monthly);
        }
        if(!repository.claim(day,"daily")) {result.put("skipped",true);return result;}
        int sent=0,failed=0;
        List<Map<String,Object>> today=all.stream().filter(e->day.toString().equals(e.get("date"))).toList();
        for(var e:today) {
            if(Boolean.TRUE.equals(e.get("optOut")))continue;
            String kind=(String)e.get("kind");if(kind.equals("birthday") && !flag(settings.get("birthdayEnabled"))) continue;
            String name=Objects.toString(e.get("name"),"");String years=Objects.toString(e.get("years"),"");
            String title=texts.get(kind+"_title").replace("{name}",name).replace("{years}",years);
            String body=texts.get(kind+"_body").replace("{name}",name).replace("{years}",years);
            try { if(Boolean.TRUE.equals(send(null,((Number)e.get("userId")).longValue(),kind,title,body).get("sentOk"))) sent++;else failed++; }
            catch(RuntimeException ex) { failed++; }
            if(kind.equals("birthday")&&flag(settings.get("birthdayCardEnabled"))) {
                try {var card=cards.create(null,((Number)e.get("userId")).longValue(),name,kind,title,body.length()>500?body.substring(0,500):body,null,"image",true,null);if(Boolean.TRUE.equals(card.get("sentOk")))sent++;else failed++;}
                catch(RuntimeException ex){failed++;}
            }
        }
        Map<String,Object> digest=today.isEmpty()?Map.of("sent",0,"failed",0):digest(day,today,texts,"daily");
        sent+=((Number)digest.get("sent")).intValue();failed+=((Number)digest.get("failed")).intValue();
        Map<String,Object> detail=Map.of("sent",sent,"failed",failed,"entries",today.size(),"hrDigest",digest);
        repository.finish(day,"daily",failed==0?"ok":sent==0?"failed":"partial",write(detail));result.put("daily",detail);return result;
    }

    private Map<String,Object> digest(LocalDate day,List<Map<String,Object>> entries,Map<String,String> templates,String type) {
        Map<String,String> values=Map.of("year",Integer.toString(day.getYear()),"month",Integer.toString(day.getMonthValue()),"date",day.toString(),"count",Integer.toString(entries.size()));
        String title=render(templates.get(type+"_title"),values);StringBuilder body=new StringBuilder(render(templates.get(type.equals("monthly")?"monthly_intro":"daily_intro"),values));
        for(var e:entries) body.append("\n").append(e.get("monthDay")).append(" ").append(e.get("name")).append(" · ").append(Objects.toString(e.get("department"),""))
                .append(" · ").append(e.get("kind").equals("birthday")?"生日":"入职 "+e.get("years")+" 周年").append(Boolean.TRUE.equals(e.get("optOut"))?"（不参与祝福）":"");
        int sent=0,failed=0;List<Long> hr=repository.hrRecipients();
        for(Long user:hr) try{if(Boolean.TRUE.equals(deliver(null,user,type.equals("monthly")?"monthly_digest":"daily_list",title,body.toString(),true).get("sentOk")))sent++;else failed++;}catch(RuntimeException ex){failed++;}
        if(hr.isEmpty())failed++;
        return Map.of("entries",entries.size(),"recipients",hr.size(),"sent",sent,"failed",failed);
    }
    private String render(String template,Map<String,String> variables){for(var e:variables.entrySet())template=template.replace("{"+e.getKey()+"}",e.getValue());return template;}
    /** 企微文本上限为2048字节；按Unicode码点分段，不能从UTF-8或代理对中间截断。 */
    static List<String> chunks(String value){List<String> out=new ArrayList<>();StringBuilder part=new StringBuilder();int bytes=0;for(int i=0;i<value.length();){int cp=value.codePointAt(i);String unit=new String(Character.toChars(cp));i+=Character.charCount(cp);int n=unit.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;if(bytes+n>1900){out.add(part.toString());part.setLength(0);bytes=0;}part.append(unit);bytes+=n;}if(!part.isEmpty())out.add(part.toString());return out;}

    private Map<String,Object> requireEmployee(long id) { return repository.employees(null).stream().filter(v -> ((Number)v.get("userId")).longValue()==id).findFirst().orElseThrow(() -> new BizException("员工不存在")); }
    private Map<String,String> templates(Object value) {
        if(value==null) return Map.of();
        try { return json.readValue(value instanceof byte[] b?new String(b,java.nio.charset.StandardCharsets.UTF_8):value.toString(),new TypeReference<>(){}); }
        catch(Exception ex) { throw new BizException("庆祝文案配置损坏，请联系管理员"); }
    }
    private String write(Object value) { try { return json.writeValueAsString(value); } catch(Exception ex) { throw new BizException("庆祝数据序列化失败"); } }
    public static boolean flag(Object value) { return Boolean.TRUE.equals(value) || value instanceof Number n && n.intValue()!=0; }
    static LocalDate date(Object value) { if(value==null) return null;if(value instanceof java.sql.Timestamp t) return t.toLocalDateTime().toLocalDate();if(value instanceof java.sql.Date d) return d.toLocalDate();if(value instanceof LocalDate d) return d;if(value instanceof LocalDateTime d) return d.toLocalDate();return LocalDate.parse(value.toString().substring(0,10)); }
}
