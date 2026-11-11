package com.cyt.center.module.portal.service.celebrations;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.integration.WecomGatewayClient;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.portal.dal.mysql.celebrations.PortalCelebrationCardRepository;
import com.cyt.center.module.portal.dal.mysql.celebrations.PortalCelebrationRepository;
import com.cyt.center.module.portal.service.admin.PortalRows;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.cyt.center.module.portal.service.support.PortalDocumentStorage;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import java.net.URI;
import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PortalCelebrationCardService {
    private final PortalCelebrationCardRepository repository;
    private final PortalCelebrationRepository employees;
    private final PortalCelebrationCardRenderer renderer;
    private final PortalDocumentStorage storage;
    private final PortalOperatorResolver operators;
    private final WecomGatewayClient gateway;
    private final PortalAudit audit;

    public Map<String,Object> create(Long actor,Long userId,String recipientName,String kind,String title,String message,Integer years,String format,boolean send,String avatarData) {
        if(!Set.of("birthday","anniversary","custom").contains(kind)||!Set.of("html","image").contains(format))throw new BizException("贺卡类型或格式无效");
        if(years!=null&&(years<1||years>100))throw new BizException("周年数须为1至100");
        Map<String,Object> employee=userId==null?null:employee(userId);
        String name=employee==null?recipientName:Objects.toString(employee.get("name"),employee.get("username").toString());
        if(name==null||name.isBlank()||name.length()>100||title==null||title.isBlank()||title.length()>100||message==null||message.isBlank()||message.length()>500)throw new BizException("收件人和标题为1至100字，祝福为1至500字");
        if(send&&userId==null)throw new BizException("发送贺卡须选择已绑定的员工");
        String key=format.equals("image")?storage.store("cards","jpg",renderer.render(name,kind,title,message,years,avatarData)):null;
        byte[] secret=new byte[24];new SecureRandom().nextBytes(secret);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        Map<String,Object> row=new LinkedHashMap<>();row.put("token",token);row.put("kind",kind);row.put("recipient_username",employee==null?null:employee.get("username"));row.put("recipient_name",name.trim());row.put("title",title.trim());row.put("message",message.trim());row.put("years",kind.equals("anniversary")?years:null);row.put("format",format);row.put("image_key",key);row.put("created_by",actor==null?"internal:celebrations":operators.resolve(actor).username());
        long id=repository.insert(row);audit.record(actor,"celebration_card_create","portal_celebration_cards",id,null,"kind="+kind+",format="+format,null);
        return send?send(actor,id):out(repository.get(id));
    }
    public PageResult<Map<String,Object>> page(int page,int size) {int p=Math.max(1,page),s=Math.min(100,Math.max(1,size));return new PageResult<>(repository.count(),(long)p,(long)s,repository.page((p-1)*s,s).stream().map(this::out).toList());}
    public Map<String,Object> send(Long actor,long id) {
        var card=repository.get(id);Map<String,Object> employee=employeeByUsername((String)card.get("recipient_username"));
        if(employee==null)throw new BizException("贺卡未绑定有效员工，请重新创建");
        if(!PortalCelebrationService.flag(employee.get("enabled"))||PortalCelebrationService.flag(employee.get("optOut")))throw new BizException("员工已停用或不参与庆祝");
        String userid=Objects.toString(employee.get("wecomUserid"),"");if(userid.isBlank())throw new BizException("员工未绑定企业微信");
        String url=(String)out(card).get("url");boolean ok=false;String error=null;
        if(!url.startsWith("https://")&&!url.startsWith("http://"))error="请配置 PORTAL_PUBLIC_BASE_URL 为员工可访问的 OA 公共地址后发送贺卡链接";
        else try {var result=gateway.call("wecom/messages/send-text",Map.of("content",card.get("title")+"\n"+url,"to_users",List.of(userid)),java.time.Duration.ofSeconds(30));if(!result.path("invaliduser").asText("").isBlank())throw new BizException("收件人不在应用可见范围");ok=true;}catch(RuntimeException ex){error="企微发送失败，请检查网关授权与收件人可见范围";}
        repository.sent(id,ok,error);employees.log(((Number)employee.get("userId")).longValue(),"celebration_card",(String)card.get("title"),ok,error);audit.record(actor,"celebration_card_send","portal_celebration_cards",id,null,"sentOk="+ok,null);return out(repository.get(id));
    }
    public byte[] image(long id) {return imageRow(repository.get(id));}
    public byte[] publicImage(String token) {return imageRow(repository.token(token));}
    private byte[] imageRow(Map<String,Object> row) {
        String key=(String)row.get("image_key");if(key==null||key.isBlank()){byte[] image=renderer.render((String)row.get("recipient_name"),(String)row.get("kind"),(String)row.get("title"),(String)row.get("message"),row.get("years")==null?null:((Number)row.get("years")).intValue(),null);key=storage.store("cards","jpg",image);repository.image(((Number)row.get("id")).longValue(),key);}return storage.read(key);
    }
    public Map<String,Object> publicCard(String token) {var row=repository.token(token);Map<String,Object> out=new LinkedHashMap<>();for(String key:List.of("kind","recipient_name","title","message","years","format"))out.put(key,row.get(key));return PortalRows.camel(out);}
    public String html(String token) {
        var card=publicCard(token);String name=escape(card.get("recipientName")),title=escape(card.get("title")),body=escape(card.get("message")).replace("\n","<br>");
        String image="/api/portal/celebrations/cards/"+token+"/image";
        String contents="image".equals(card.get("format"))?"<img class=card-image src='"+image+"' alt='"+title+"'>":"<div class=eyebrow>CYT · WITH YOU</div><div class=ornament>✦</div><p class=name>致 "+name+"</p><h1>"+title+"</h1><div class=line></div><p class=message>"+body+"</p><p class=sign>畅韵科技 · 与你同行</p>";
        return "<!doctype html><html lang=zh-CN><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>"+title+"</title><style>body{margin:0;background:#f2effb;color:#302a4e;font-family:system-ui,'Microsoft Yahei',sans-serif;min-height:100vh;display:grid;place-items:center}main{box-sizing:border-box;width:min(92vw,560px);margin:28px auto;background:#fffaf5;border:1px solid #e8def2;border-radius:24px;box-shadow:0 20px 70px #44306b12;text-align:center;overflow:hidden}.text{padding:48px 32px}.eyebrow{font-size:11px;letter-spacing:4px;color:#8b7f9f}.ornament{font-size:48px;color:#c5a170;margin:30px 0 20px}.name{font-size:17px;color:#8b7f9f}h1{font-size:clamp(23px,5vw,34px);line-height:1.5;overflow-wrap:anywhere}.line{width:48px;height:2px;background:#c5a170;margin:24px auto}.message{font-size:16px;line-height:2;white-space:normal;overflow-wrap:anywhere}.sign{font-size:12px;color:#9b91aa;margin-top:40px}.card-image{display:block;width:100%;height:auto}footer{text-align:center;font-size:11px;color:#a49bb1;margin:18px}</style><main class='"+("image".equals(card.get("format"))?"":"text")+"'>"+contents+"</main></html>";
    }
    private String escape(Object value){return HtmlUtils.htmlEscape(Objects.toString(value,""),"UTF-8");}
    private Map<String,Object> out(Map<String,Object> row) {Map<String,Object> out=PortalRows.camel(row);out.remove("recipientUsername");out.remove("imageKey");var user=employeeByUsername((String)row.get("recipient_username"));out.put("userId",user==null?null:user.get("userId"));out.put("sentOk",PortalCelebrationService.flag(row.get("sent_ok")));String path="/api/portal/celebrations/cards/"+row.get("token");String base=base();out.put("url",base+path+"/page");out.put("imageUrl",base+path+"/image");return out;}
    private String base() {String base=System.getenv("PORTAL_PUBLIC_BASE_URL");if(base==null||base.isBlank())return "";try{URI u=URI.create(base);if(!Set.of("http","https").contains(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null)throw new Exception();return base.replaceAll("/+$","");}catch(Exception ex){throw new BizException("PORTAL_PUBLIC_BASE_URL 配置无效");}}
    private Map<String,Object> employee(long userId){return employees.employees(null).stream().filter(r->((Number)r.get("userId")).longValue()==userId).findFirst().orElseThrow(()->new BizException("员工不存在"));}
    private Map<String,Object> employeeByUsername(String username){if(username==null)return null;return employees.employees(null).stream().filter(r->username.equals(r.get("username"))).findFirst().orElse(null);}
}
