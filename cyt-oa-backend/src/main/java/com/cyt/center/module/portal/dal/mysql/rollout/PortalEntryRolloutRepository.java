package com.cyt.center.module.portal.dal.mysql.rollout;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PortalEntryRolloutRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public record Settings(
            Map<String, Object> entries,
            long revision,
            String updatedBy,
            LocalDateTime updatedAt) {}

    public Settings read(boolean lock) {
        var rows =
                jdbc.query(
                        "SELECT entries,revision,updated_by,updated_at FROM portal_entry_rollout"
                            + " WHERE id=1"
                                + (lock ? " FOR UPDATE" : ""),
                        (rs, index) -> {
                            try {
                                Map<String, Object> entries =
                                        mapper.readValue(
                                                rs.getString("entries"), new TypeReference<>() {});
                                var time = rs.getTimestamp("updated_at");
                                return new Settings(
                                        entries,
                                        rs.getLong("revision"),
                                        rs.getString("updated_by"),
                                        time == null ? null : time.toLocalDateTime());
                            } catch (java.io.IOException error) {
                                throw new IllegalStateException("入口上线设置无法读取", error);
                            }
                        });
        return rows.isEmpty() ? new Settings(Map.of(), 0, null, null) : rows.getFirst();
    }

    public void initialize() {
        jdbc.update(
                "INSERT IGNORE INTO portal_entry_rollout(id,entries,revision)"
                    + " VALUES(1,JSON_OBJECT(),0)");
    }

    public boolean update(Map<String, Boolean> entries, long expectedRevision, String actor) {
        try {
            return jdbc.update(
                            "UPDATE portal_entry_rollout SET"
                                + " entries=?,revision=revision+1,updated_by=?,updated_at=CURRENT_TIMESTAMP(6)"
                                + " WHERE id=1 AND revision=?",
                            mapper.writeValueAsString(entries),
                            actor,
                            expectedRevision)
                    == 1;
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalStateException(error);
        }
    }
}
