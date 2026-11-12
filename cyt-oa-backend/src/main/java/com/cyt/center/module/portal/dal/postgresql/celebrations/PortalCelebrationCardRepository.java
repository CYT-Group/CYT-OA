package com.cyt.center.module.portal.dal.postgresql.celebrations;

import com.cyt.center.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PortalCelebrationCardRepository {
    private final JdbcTemplate jdbc;
    public long insert(Map<String,Object> row) {
        var key=new GeneratedKeyHolder();jdbc.update(c->{var p=c.prepareStatement("INSERT INTO portal_celebration_cards(token,kind,recipient_username,recipient_name,title,message,years,format,image_key,created_by) VALUES(?,?,?,?,?,?,?,?,?,?)",new String[]{"id"});
            String[] keys={"token","kind","recipient_username","recipient_name","title","message","years","format","image_key","created_by"};for(int i=0;i<keys.length;i++)p.setObject(i+1,row.get(keys[i]));return p;},key);return key.getKey().longValue();
    }
    public Map<String,Object> get(long id) {var rows=jdbc.queryForList("SELECT * FROM portal_celebration_cards WHERE id=?",id);if(rows.isEmpty())throw new BizException("贺卡不存在");return rows.getFirst();}
    public Map<String,Object> token(String token) {if(token==null||!token.matches("[A-Za-z0-9_-]{32}"))throw new BizException("贺卡不存在");var rows=jdbc.queryForList("SELECT * FROM portal_celebration_cards WHERE token=?",token);if(rows.isEmpty())throw new BizException("贺卡不存在");return rows.getFirst();}
    public List<Map<String,Object>> page(int offset,int size) {return jdbc.queryForList("SELECT * FROM portal_celebration_cards ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",size,offset);}
    public long count() {return jdbc.queryForObject("SELECT COUNT(*) FROM portal_celebration_cards",Long.class);}
    public void sent(long id,boolean ok,String error) {jdbc.update("UPDATE portal_celebration_cards SET sent_ok=?,error_msg=?,sent_at=CURRENT_TIMESTAMP WHERE id=?",ok,error,id);}
    public void image(long id,String key) {jdbc.update("UPDATE portal_celebration_cards SET image_key=? WHERE id=?",key,id);}
}
