package com.cyt.center.module.portal.dal.postgresql.signoff;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PortalSignoffNotificationRepository {
    private final JdbcTemplate jdbc;
    public Map<String,String> recipients() {
        Map<String,String> out=new java.util.HashMap<>();
        jdbc.query("SELECT username,wecom_userid FROM sys_user WHERE enabled=1 AND wecom_userid IS NOT NULL AND wecom_userid<>''",(org.springframework.jdbc.core.RowCallbackHandler)r->out.put(r.getString(1),r.getString(2)));
        return out;
    }
    public void log(Long actor,Long documentId,int targets,int sent,int failed) {
        jdbc.update("INSERT INTO portal_signoff_notify_log(actor_id,document_id,target_count,sent_count,failed_count) VALUES(?,?,?,?,?)",actor,documentId,targets,sent,failed);
    }
}
