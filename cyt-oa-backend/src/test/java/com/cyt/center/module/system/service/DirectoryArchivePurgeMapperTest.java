package com.cyt.center.module.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.cyt.center.module.hrm.dal.mysql.employee.info.DirectoryArchivePurgeMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DirectoryArchivePurgeMapperTest {
    @Test void deletesPhysicalRowsIncludingPreviouslySoftDeletedArchives() {
        var config = new MybatisConfiguration();
        config.addMapper(DirectoryArchivePurgeMapper.class);
        String prefix = DirectoryArchivePurgeMapper.class.getName() + ".";
        var params = Map.of("ids", List.of(7L, 8L));
        var query = config.getMappedStatement(prefix + "selectIdsByUserIds").getBoundSql(params);
        assertFalse(query.getSql().contains("deleted"));
        var deletion = config.getMappedStatement(prefix + "deleteArchives").getBoundSql(params);
        assertTrue(deletion.getSql().startsWith("DELETE FROM hrm_employee WHERE id IN"));
        assertEquals(2, deletion.getParameterMappings().size());
        var details = config.getMappedStatement(prefix + "deleteDetails")
                .getBoundSql(Map.of("table", "hrm_employee_wecom_sync", "ids", List.of(42L)));
        assertTrue(details.getSql().startsWith("DELETE FROM hrm_employee_wecom_sync WHERE employee_id IN"));
        assertEquals(1, details.getParameterMappings().size());
        var employmentDocuments = config.getMappedStatement(prefix + "deleteDetails")
                .getBoundSql(Map.of("table", "hrm_employment_document", "ids", List.of(42L)));
        assertTrue(employmentDocuments.getSql().startsWith("DELETE FROM hrm_employment_document WHERE employee_id IN"));
    }
    @Test void detailSqlRejectsTablesOutsideTheExplicitArchiveList() {
        assertThrows(IllegalArgumentException.class, () -> DirectoryArchivePurgeMapper.Sql.deleteDetails(
                Map.of("table", "sys_user", "ids", List.of(7L))));
    }
}
