package com.cyt.center.module.hrm.dal.mysql.employee.info;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 企微对齐专用的物理删除，不经过员工档案默认的逻辑删除。 */
@Mapper
public interface DirectoryArchivePurgeMapper {
    String IDS = "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>";
    Set<String> DETAIL_TABLES = Set.of("hrm_employee_contact", "hrm_employee_file",
            "hrm_employee_personal_note", "hrm_employee_certificate", "hrm_employee_education_experience",
            "hrm_employee_work_experience", "hrm_employee_training_experience", "hrm_employee_contract",
            "hrm_employee_quit_info", "hrm_employee_salary_card", "hrm_employee_change_record", "hrm_employee_wecom_sync");

    @Select("<script>SELECT id FROM hrm_employee WHERE user_id IN " + IDS + "</script>")
    List<Long> selectIdsByUserIds(@Param("ids") List<Long> userIds);

    @Select("SELECT table_name FROM information_schema.columns WHERE table_schema=DATABASE() AND column_name='employee_id'")
    List<String> existingEmployeeTables();

    @Update("<script>UPDATE hrm_employee SET leader_employee_id=NULL WHERE leader_employee_id IN " + IDS + "</script>")
    int clearManagerReferences(@Param("ids") List<Long> employeeIds);

    @DeleteProvider(type = Sql.class, method = "deleteDetails")
    int deleteDetails(@Param("table") String table, @Param("ids") List<Long> employeeIds);

    @Delete("<script>DELETE FROM hrm_employee WHERE id IN " + IDS + "</script>")
    int deleteArchives(@Param("ids") List<Long> employeeIds);

    final class Sql {
        public static String deleteDetails(Map<String, Object> params) {
            String table = (String) params.get("table");
            if (!DETAIL_TABLES.contains(table)) throw new IllegalArgumentException("不支持的员工档案明细表");
            return "<script>DELETE FROM " + table + " WHERE employee_id IN " + IDS + "</script>";
        }
    }
}
