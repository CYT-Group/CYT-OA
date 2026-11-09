package com.cyt.center.module.hrm.controller.admin.attendance;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.attendance.vo.group.HrmAttendanceGroupPageReqVO;
import com.cyt.center.module.hrm.controller.admin.attendance.vo.group.HrmAttendanceGroupRespVO;
import com.cyt.center.module.hrm.controller.admin.attendance.vo.group.HrmAttendanceGroupSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.service.attendance.config.HrmAttendanceGroupService;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertList;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertSetByFlatMap;

@Tag(name = "管理后台 - HRM 考勤组")
@RestController
@RequestMapping("/hrm/attendance/group")
@Validated
public class HrmAttendanceGroupController {

    @Resource
    private HrmAttendanceGroupService attendanceGroupService;
    @Resource
    private HrmEmployeeService employeeService;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建考勤组")
    @SaCheckPermission("hrm:attendance:group:create")
    public Result<Long> createAttendanceGroup(@Valid @RequestBody HrmAttendanceGroupSaveReqVO createReqVO) {
        return success(attendanceGroupService.createAttendanceGroup(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改考勤组")
    @SaCheckPermission("hrm:attendance:group:update")
    public Result<Boolean> updateAttendanceGroup(@Valid @RequestBody HrmAttendanceGroupSaveReqVO updateReqVO) {
        attendanceGroupService.updateAttendanceGroup(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除考勤组")
    @Parameter(name = "id", description = "考勤组编号", required = true, example = "1024")
    @SaCheckPermission("hrm:attendance:group:delete")
    public Result<Boolean> deleteAttendanceGroup(@RequestParam("id") Long id) {
        attendanceGroupService.deleteAttendanceGroup(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得考勤组")
    @Parameter(name = "id", description = "考勤组编号", required = true, example = "1024")
    @SaCheckPermission("hrm:attendance:group:query")
    public Result<HrmAttendanceGroupRespVO> getAttendanceGroup(@RequestParam("id") Long id) {
        return success(buildAttendanceGroupRespVO(attendanceGroupService.getAttendanceGroup(id)));
    }

    @GetMapping("/my")
    @Operation(summary = "获得员工所在考勤组")
    @Parameter(name = "employeeId", description = "员工编号", required = true, example = "1024")
    @SaCheckPermission("hrm:attendance:group:query")
    public Result<HrmAttendanceGroupRespVO> getMyAttendanceGroup(@RequestParam("employeeId") Long employeeId) {
        return success(buildAttendanceGroupRespVO(attendanceGroupService.getMyAttendanceGroup(employeeId)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得考勤组分页")
    @SaCheckPermission("hrm:attendance:group:query")
    public Result<PageResult<HrmAttendanceGroupRespVO>> getAttendanceGroupPage(
            @Validated HrmAttendanceGroupPageReqVO pageReqVO) {
        PageResult<HrmAttendanceGroupDO> pageResult = attendanceGroupService.getAttendanceGroupPage(pageReqVO);
        return success(new PageResult<>(
                buildAttendanceGroupRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得考勤组精简列表")
    @SaCheckPermission("hrm:attendance:group:query")
    public Result<List<HrmAttendanceGroupRespVO>> getAttendanceGroupSimpleList() {
        return success(convertList(attendanceGroupService.getAttendanceGroupList(),
                group -> new HrmAttendanceGroupRespVO().setId(group.getId()).setName(group.getName())));
    }

    // ==================== 拼接 VO ====================

    private HrmAttendanceGroupRespVO buildAttendanceGroupRespVO(HrmAttendanceGroupDO group) {
        if (group == null) {
            return null;
        }
        return buildAttendanceGroupRespVOList(Collections.singletonList(group)).get(0);
    }

    private List<HrmAttendanceGroupRespVO> buildAttendanceGroupRespVOList(List<HrmAttendanceGroupDO> list) {
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSetByFlatMap(list, HrmAttendanceGroupDO::getDeptIds, Collection::stream));
        Map<Long, HrmEmployeeDO> employeeMap = employeeService.getEmployeeMap(
                convertSetByFlatMap(list, HrmAttendanceGroupDO::getEmployeeIds, Collection::stream));
        return convertList(list, group -> BeanUtils.toBean(group, HrmAttendanceGroupRespVO.class)
                .setDeptNames(convertList(group.getDeptIds(),
                        deptId -> deptMap.containsKey(deptId) ? deptMap.get(deptId).getName() : null))
                .setEmployeeNames(convertList(group.getEmployeeIds(),
                        employeeId -> employeeMap.containsKey(employeeId)
                                ? employeeMap.get(employeeId).getName() : null)));
    }

}
