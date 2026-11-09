package com.cyt.center.module.hrm.controller.admin.employee;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.collection.MapUtils;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.changerecord.HrmEmployeeChangeRecordRespVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.employment.HrmEmployeeChangeRecordDO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.service.employee.employment.HrmEmployeeChangeRecordService;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertSetByFlatMap;

@Tag(name = "管理后台 - HRM 员工异动记录")
@RestController
@RequestMapping("/hrm/employee/change-record")
@Validated
public class HrmEmployeeChangeRecordController {

    @Resource
    private HrmEmployeeChangeRecordService changeRecordService;
    @Resource
    private HrmEmployeeService employeeService;
    @Resource
    private DeptApi deptApi;

    @GetMapping("/list")
    @Operation(summary = "获得员工异动记录列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:query")
    public Result<List<HrmEmployeeChangeRecordRespVO>> getEmployeeChangeRecordList(
            @RequestParam("employeeId") Long employeeId) {
        return success(buildEmployeeChangeRecordRespVOList(
                changeRecordService.getEmployeeChangeRecordListByEmployeeId(employeeId)));
    }

    // ==================== 拼接 VO ====================

    private List<HrmEmployeeChangeRecordRespVO> buildEmployeeChangeRecordRespVOList(
            List<HrmEmployeeChangeRecordDO> changeRecords) {
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSetByFlatMap(changeRecords, changeRecord -> Stream.of(changeRecord.getOldDeptId(), changeRecord.getNewDeptId())));
        Map<Long, HrmEmployeeDO> leaderEmployeeMap = employeeService.getEmployeeMap(
                convertSetByFlatMap(changeRecords, changeRecord -> Stream.of(changeRecord.getOldLeaderEmployeeId(), changeRecord.getNewLeaderEmployeeId())));
        return BeanUtils.toBean(changeRecords, HrmEmployeeChangeRecordRespVO.class, changeRecord -> {
            MapUtils.findAndThen(deptMap, changeRecord.getOldDeptId(),
                    dept -> changeRecord.setOldDeptName(dept.getName()));
            MapUtils.findAndThen(deptMap, changeRecord.getNewDeptId(),
                    dept -> changeRecord.setNewDeptName(dept.getName()));
            MapUtils.findAndThen(leaderEmployeeMap, changeRecord.getOldLeaderEmployeeId(),
                    leader -> changeRecord.setOldLeaderEmployeeName(leader.getName()));
            MapUtils.findAndThen(leaderEmployeeMap, changeRecord.getNewLeaderEmployeeId(),
                    leader -> changeRecord.setNewLeaderEmployeeName(leader.getName()));
        });
    }

}
