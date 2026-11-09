package com.cyt.center.module.hrm.controller.admin.portal.attendance;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.attendance.vo.leave.HrmAttendanceLeaveCancelReqVO;
import com.cyt.center.module.hrm.controller.admin.attendance.vo.leave.HrmAttendanceLeaveCreateReqVO;
import com.cyt.center.module.hrm.controller.admin.attendance.vo.leave.HrmAttendanceLeaveRespVO;
import com.cyt.center.module.hrm.dal.dataobject.attendance.record.HrmAttendanceLeaveDO;
import com.cyt.center.module.hrm.service.attendance.record.HrmAttendanceLeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - HRM 员工端请假")
@RestController
@RequestMapping("/hrm/portal/attendance/leave")
@Validated
public class HrmPortalAttendanceLeaveController {

    @Resource
    private HrmAttendanceLeaveService attendanceLeaveService;

    @GetMapping("/list")
    @Operation(summary = "获得我的请假申请列表")
    @SaCheckPermission("hrm:portal:query")
    public Result<List<HrmAttendanceLeaveRespVO>> getAttendanceLeaveList() {
        List<HrmAttendanceLeaveDO> list = attendanceLeaveService.getMyAttendanceLeaveList(getLoginUserId());
        return success(BeanUtils.toBean(list, HrmAttendanceLeaveRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建我的请假申请")
    @SaCheckPermission("hrm:portal:attendance:leave")
    public Result<Long> createAttendanceLeave(
            @Valid @RequestBody HrmAttendanceLeaveCreateReqVO reqVO) {
        return success(attendanceLeaveService.createLeave(getLoginUserId(), reqVO));
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消我的请假申请")
    @SaCheckPermission("hrm:portal:attendance:leave")
    public Result<Boolean> cancelAttendanceLeave(
            @Valid @RequestBody HrmAttendanceLeaveCancelReqVO reqVO) {
        attendanceLeaveService.cancelLeave(getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/sync")
    @Operation(summary = "从企业微信回读我的请假审批状态")
    @SaCheckPermission("hrm:portal:attendance:leave")
    public Result<HrmAttendanceLeaveRespVO> syncAttendanceLeave(@RequestParam("id") Long id) {
        HrmAttendanceLeaveDO record = attendanceLeaveService.syncLeaveApprovalStatus(id, getLoginUserId());
        return success(BeanUtils.toBean(record, HrmAttendanceLeaveRespVO.class));
    }

}
