package com.cyt.center.module.portal.dal.postgresql.signoff;

import com.cyt.center.module.portal.domain.signoff.PortalSignoffDocumentOption;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

/** 统计人员可跨部门核对签收，但只读取已发布快照的六项元数据。 */
@Repository
@RequiredArgsConstructor
public class PortalSignoffDocumentRepository {
    private final JdbcTemplate jdbc;

    public List<PortalSignoffDocumentOption> options() {
        return jdbc.query("""
                SELECT id,title,version,requires_signoff,published_at,offline_at
                FROM portal_hr_documents
                WHERE published_at IS NOT NULL AND requires_signoff=1 AND deleted_at IS NULL
                ORDER BY (offline_at IS NOT NULL),title,id
                """, (rs, index) -> {
            var published = rs.getTimestamp("published_at");
            var offline = rs.getTimestamp("offline_at");
            return new PortalSignoffDocumentOption(rs.getLong("id"),rs.getString("title"),
                    rs.getString("version"),rs.getBoolean("requires_signoff"),
                    published == null ? null : published.toLocalDateTime(),
                    offline == null ? null : offline.toLocalDateTime());
        });
    }
}
