package com.cyt.center.module.hrm.service.employee.info;

import com.cyt.center.core.exception.BizException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/** 数据库只保存运行状态和必要的处理明细，不保存上游完整档案或调用凭据。 */
@Repository
@RequiredArgsConstructor
public class WecomRosterSyncRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public void insert(WecomRosterSyncService.Run run) {
        jdbc.update("INSERT INTO hrm_wecom_roster_sync_run "
                        + "(id,status,stage,actor,message,active_slot,created_at) VALUES (?,?,?,?,?,1,?)",
                run.id(), run.status(), run.stage(), run.actor(), run.message(), run.createdAt());
    }

    public void save(WecomRosterSyncService.Run run) {
        jdbc.update("UPDATE hrm_wecom_roster_sync_run SET status=?,stage=?,message=?,"
                        + "directory_json=?,result_json=?,active_slot=?,finished_at=? WHERE id=?",
                run.status(), run.stage(), run.message(), encode(run.directory()), encode(run.result()),
                "running".equals(run.status()) ? 1 : null, run.finishedAt(), run.id());
    }

    public WecomRosterSyncService.Run find(String id) {
        List<WecomRosterSyncService.Run> rows = jdbc.query(
                "SELECT * FROM hrm_wecom_roster_sync_run WHERE id=?", mapper(), id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public WecomRosterSyncService.Run latest() {
        List<WecomRosterSyncService.Run> rows = jdbc.query(
                "SELECT * FROM hrm_wecom_roster_sync_run ORDER BY created_at DESC,id DESC LIMIT 1", mapper());
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public int settleInterrupted() {
        return jdbc.update("UPDATE hrm_wecom_roster_sync_run SET status='failed',"
                + "message='服务重启，同步中断；已完成的步骤保留，请重新同步',active_slot=NULL,finished_at=? "
                + "WHERE status='running'", LocalDateTime.now());
    }

    private RowMapper<WecomRosterSyncService.Run> mapper() {
        return (row, index) -> {
            Timestamp finished = row.getTimestamp("finished_at");
            return new WecomRosterSyncService.Run(row.getString("id"), row.getString("status"),
                    row.getString("stage"), row.getString("actor"), row.getString("message"),
                    decode(row.getString("directory_json"), WecomRosterSyncService.Directory.class),
                    decode(row.getString("result_json"), WecomHrSyncService.Result.class),
                    row.getTimestamp("created_at").toLocalDateTime(),
                    finished == null ? null : finished.toLocalDateTime());
        };
    }

    private String encode(Object value) {
        if (value == null) return null;
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException error) { throw new BizException("花名册同步结果保存失败"); }
    }

    private <T> T decode(String value, Class<T> type) {
        if (value == null || value.isBlank()) return null;
        try { return json.readValue(value, type); }
        catch (JsonProcessingException error) { throw new BizException("花名册同步记录无法读取，请联系管理员"); }
    }
}
