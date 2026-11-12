package com.cyt.center.module.portal.dal.postgresql.branddoc;

import com.cyt.center.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PortalBrandDocumentRepository {
    private final JdbcTemplate jdbc;
    private static final String COLUMNS = "id,brand_code AS \"brandCode\",doc_type AS \"docType\",title,file_url AS \"fileUrl\","
            + "file_size AS \"fileSize\",status,updated_by AS \"updatedBy\",created_at AS \"createdAt\",updated_at AS \"updatedAt\"";

    public Map<String, Object> get(long id, boolean lock) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT " + COLUMNS + " FROM portal_brand_docs WHERE id=?"
                + (lock ? " FOR UPDATE" : ""), id);
        if (rows.isEmpty()) throw new BizException("品牌手册不存在");
        return rows.getFirst();
    }

    public List<Map<String, Object>> page(String q, String brand, String status, int offset, int size) {
        List<Object> args = new ArrayList<>();
        String where = where(q, brand, status, args);
        args.add(size); args.add(offset);
        return jdbc.queryForList("SELECT " + COLUMNS + " FROM portal_brand_docs" + where
                + " ORDER BY updated_at DESC,id DESC LIMIT ? OFFSET ?", args.toArray());
    }

    public long count(String q, String brand, String status) {
        List<Object> args = new ArrayList<>();
        return jdbc.queryForObject("SELECT COUNT(*) FROM portal_brand_docs" + where(q, brand, status, args), Long.class, args.toArray());
    }

    private String where(String q, String brand, String status, List<Object> args) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1");
        if (q != null && !q.isBlank()) { sql.append(" AND title LIKE ?"); args.add("%" + q.trim() + "%"); }
        if (brand != null && !brand.isBlank()) { sql.append(" AND brand_code=?"); args.add(brand); }
        if (status != null && !status.isBlank()) { sql.append(" AND status=?"); args.add(status); }
        return sql.toString();
    }

    public Long slot(String brand, String type) {
        List<Long> rows = jdbc.queryForList("SELECT id FROM portal_brand_docs WHERE brand_code=? AND doc_type=? FOR UPDATE", Long.class, brand, type);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public long insert(String brand, String type, String title, String file, int size, String actor) {
        var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(c -> {
            var p = c.prepareStatement("INSERT INTO portal_brand_docs(brand_code,doc_type,title,file_url,file_size,status,updated_by) VALUES(?,?,?,?,?,'published',?)", new String[]{"id"});
            p.setString(1,brand); p.setString(2,type); p.setString(3,title); p.setString(4,file); p.setInt(5,size); p.setString(6,actor);
            return p;
        }, key);
        return key.getKey().longValue();
    }

    public void replace(long id, String title, String file, long size, String actor) {
        jdbc.update("UPDATE portal_brand_docs SET title=?,file_url=?,file_size=?,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", title,file,size,actor,id);
    }

    public void version(long id, String file, int size, String actor) {
        jdbc.update("INSERT INTO portal_brand_doc_versions(doc_id,file_url,file_size,uploaded_by) VALUES(?,?,?,?)", id,file,size,actor);
    }

    public List<Map<String, Object>> versions(long id) {
        return jdbc.queryForList("SELECT id,doc_id AS \"docId\",file_url AS \"fileUrl\",file_size AS \"fileSize\",uploaded_by AS \"uploadedBy\",created_at AS \"createdAt\" FROM portal_brand_doc_versions WHERE doc_id=? ORDER BY created_at DESC,id DESC", id);
    }

    public Map<String, Object> version(long id, long versionId) {
        return versions(id).stream().filter(v -> ((Number)v.get("id")).longValue() == versionId).findFirst()
                .orElseThrow(() -> new BizException("目标版本不属于当前文档"));
    }

    public void update(long id, String title, String status, String actor) {
        jdbc.update("UPDATE portal_brand_docs SET title=?,status=?,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", title,status,actor,id);
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM portal_brand_doc_versions WHERE doc_id=?", id);
        jdbc.update("DELETE FROM portal_brand_docs WHERE id=?", id);
    }
}
