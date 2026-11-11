package com.cyt.center.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyt.center.module.system.model.entity.SysUserDept;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface SysUserDeptMapper extends BaseMapper<SysUserDept> {

    @Insert("<script>INSERT INTO sys_user_dept (user_id, dept_id) VALUES "
            + "<foreach collection='rows' item='row' separator=','>(#{row.userId}, #{row.deptId})</foreach></script>")
    int insertBindings(@Param("rows") List<SysUserDept> rows);

    /**
     * 部门树只要人数，不需要把整张关系表拉回 Java 内存再逐行累加。
     * 返回 Map 的 key 为 SQL 列名 dept_id / member_count（Map 结果不走驼峰映射），
     * 数值类型由 JDBC 驱动给出，调用方按 Number 读取。
     */
    @Select("SELECT dept_id, COUNT(*) AS member_count FROM sys_user_dept GROUP BY dept_id")
    List<Map<String, Object>> countByDept();
}
