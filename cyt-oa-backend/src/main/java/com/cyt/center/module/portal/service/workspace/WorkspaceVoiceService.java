package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.me.mapper.VoiceMapper;
import com.cyt.center.module.me.model.entity.Voice;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import static com.cyt.center.module.portal.service.workspace.WorkspaceValidation.*;

@Service @RequiredArgsConstructor
public class WorkspaceVoiceService {
    private final PortalWorkspaceStore store;
    private final WorkspaceIdentity identity;
    private final JdbcTemplate jdbc;
    private final VoiceMapper voices;
    public List<Map<String,Object>> list(String channel,boolean manage){
        oneOf(channel,"all","suggestion","ceo","anon","survey");
        if(manage){identity.require("can_manage_voice");if(channel.equals("ceo")&&!identity.admin())throw AuthException.forbidden("CEO 信箱仅对超级管理员开放");}else identity.personal();
        var args=new ArrayList<Object>();String where=manage?"1=1":"user_id=?";if(!manage)args.add(identity.user());
        if(manage&&!identity.admin())where+=" AND channel<>'ceo'";
        if(!channel.equals("all")){where+=" AND channel=?";args.add(channel);}
        return jdbc.queryForList("SELECT * FROM voices WHERE "+where+" ORDER BY id DESC LIMIT 1000",args.toArray()).stream().map(row->{
            long id=((Number)row.get("id")).longValue();boolean anonymous=Boolean.TRUE.equals(row.get("anonymous"))||(row.get("anonymous") instanceof Number n&&n.intValue()!=0);
            var out=new LinkedHashMap<String,Object>();out.put("id",String.valueOf(id));out.put("channel",row.get("channel"));out.put("anonymous",anonymous);out.put("author",anonymous?"匿名":identity.name(((Number)row.get("user_id")).longValue()));out.put("content",row.get("content"));out.put("status",row.get("status"));out.put("createdAt",String.valueOf(row.get("created_at")));
            var meta=store.byKey("voice-reply",String.valueOf(id));out.put("reply",meta==null?"":meta.data().getOrDefault("reply",""));out.put("moderation",meta==null?"pending":meta.data().getOrDefault("moderation","pending"));out.put("repliedAt",meta==null?null:meta.updatedAt());return (Map<String,Object>)out;
        }).toList();
    }
    @Transactional
    public long submit(Map<String,Object> body){
        identity.personal();String channel=text(body,"channel",20,true);oneOf(channel,"suggestion","ceo","anon");String content=text(body,"content",2000,true);
        var row=new Voice();row.setUserId(identity.user());row.setChannel(channel);row.setContent(content);row.setAnonymous(channel.equals("anon")||Boolean.TRUE.equals(body.get("anonymous")));row.setStatus("open");row.setCreatedAt(LocalDateTime.now());voices.insert(row);return row.getId();
    }
    @Transactional
    public void reply(long id,Map<String,Object> body){
        identity.require("can_manage_voice");var rows=jdbc.queryForList("SELECT * FROM voices WHERE id=? FOR UPDATE",id);if(rows.isEmpty())throw new BizException("发声记录不存在");var row=rows.getFirst();
        if(row.get("channel").equals("ceo")&&!identity.admin())throw AuthException.forbidden("CEO 信箱仅对超级管理员开放");
        String status=text(body,"status",20,true);oneOf(status,"open","handling","done","closed");String reply=text(body,"reply",2000,status.equals("done"));String moderation=text(body,"moderation",20,true);oneOf(moderation,"pending","approved","rejected");
        var meta=store.byKey("voice-reply",String.valueOf(id));var data=Map.<String,Object>of("reply",reply,"moderation",moderation);
        if(meta==null)store.insert("voice-reply",String.valueOf(id),identity.user(),identity.dept(),id,"发声处理","active",data);else store.update(meta,meta.version(),meta.title(),"active",data,id);
        jdbc.update("UPDATE voices SET status=? WHERE id=?",status,id);
        if(!reply.isBlank())identity.notify(((Number)row.get("user_id")).longValue(),"voice-reply",id,"你的发声收到回复","/me/voice?channel="+row.get("channel"));
    }
}
