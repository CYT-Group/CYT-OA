package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.core.exception.BizException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.util.*;

/** 业务表单的持久化层；kind 和 SQL 条件只由服务端声明，调用方必须先完成数据权限校验。 */
@Repository
@RequiredArgsConstructor
public class PortalWorkspaceStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public record Row(long id, String kind, String key, long ownerId, Long deptId, Long parentId,
                      String title, String status, Map<String,Object> data, int version,
                      String createdAt, String updatedAt) {}

    public Row get(String kind, long id, boolean lock) {
        var rows = jdbc.queryForList("SELECT * FROM portal_workspace_records WHERE kind=? AND id=?" + (lock ? " FOR UPDATE" : ""), kind, id);
        if (rows.isEmpty()) throw new BizException("记录不存在或已删除，请刷新页面");
        return row(rows.getFirst());
    }
    public Row byKey(String kind, String key) {
        var rows = jdbc.queryForList("SELECT * FROM portal_workspace_records WHERE kind=? AND record_key=?",kind,key);
        return rows.isEmpty() ? null : row(rows.getFirst());
    }
    public List<Row> list(String kind) {
        return jdbc.queryForList("SELECT * FROM portal_workspace_records WHERE kind=? ORDER BY id DESC LIMIT 2000",kind).stream().map(this::row).toList();
    }
    /** 条件只由同包业务代码构造，值全部绑定；先在数据库限定所有权和业务范围。 */
    List<Row> select(String kind,String condition,List<Object> values){
        var args=new ArrayList<Object>();args.add(kind);args.addAll(values);
        return jdbc.queryForList("SELECT * FROM portal_workspace_records WHERE kind=? AND ("+condition+") ORDER BY id DESC",args.toArray()).stream().map(this::row).toList();
    }
    public long insert(String kind, String key, long owner, Long dept, Long parent, String title, String status, Map<String,Object> data) {
        var holder = new GeneratedKeyHolder();
        jdbc.update(c -> {
            var p = c.prepareStatement("INSERT INTO portal_workspace_records(kind,record_key,owner_id,dept_id,parent_id,title,status,payload) VALUES(?,?,?,?,?,?,?,?)", new String[]{"id"});
            p.setString(1,kind);p.setString(2,key);p.setLong(3,owner);p.setObject(4,dept);p.setObject(5,parent);
            p.setString(6,title);p.setString(7,status);p.setString(8,encode(data));return p;
        },holder);
        return Objects.requireNonNull(holder.getKey()).longValue();
    }
    public void update(Row old, int expectedVersion, String title, String status, Map<String,Object> data, Long parent) {
        int changed = jdbc.update("UPDATE portal_workspace_records SET title=?,status=?,payload=?,parent_id=?,version=version+1 WHERE id=? AND kind=? AND version=?",
                title,status,encode(data),parent,old.id(),old.kind(),expectedVersion);
        if (changed != 1) throw new BizException("记录已被其他人修改，请刷新后重试");
    }
    public void delete(Row row) { jdbc.update("DELETE FROM portal_workspace_records WHERE id=? AND kind=?",row.id(),row.kind()); }
    public String encode(Object value) {
        try {return json.writeValueAsString(value);} catch (Exception e) {throw new BizException("资料格式不正确");}
    }
    public Map<String,Object> decode(String value) {
        try {return json.readValue(value,new TypeReference<LinkedHashMap<String,Object>>(){});} catch(Exception e){throw new BizException("已保存资料无法读取，请联系管理员");}
    }
    public Map<String,Object> view(Row r) {
        var out = new LinkedHashMap<String,Object>();
        out.put("id",String.valueOf(r.id()));out.put("kind",r.kind());out.put("ownerId",String.valueOf(r.ownerId()));out.put("deptId",r.deptId()==null?null:String.valueOf(r.deptId()));
        out.put("parentId",r.parentId()==null?null:String.valueOf(r.parentId()));out.put("title",r.title());out.put("status",r.status());out.put("data",r.data());out.put("version",r.version());out.put("createdAt",r.createdAt());out.put("updatedAt",r.updatedAt());return out;
    }
    private Row row(Map<String,Object> m) {
        return new Row(((Number)m.get("id")).longValue(),(String)m.get("kind"),(String)m.get("record_key"),((Number)m.get("owner_id")).longValue(),number(m.get("dept_id")),number(m.get("parent_id")),(String)m.get("title"),(String)m.get("status"),decode(String.valueOf(m.get("payload"))),((Number)m.get("version")).intValue(),String.valueOf(m.get("created_at")),String.valueOf(m.get("updated_at")));
    }
    static Long number(Object n) { return n == null ? null : ((Number)n).longValue(); }
}
