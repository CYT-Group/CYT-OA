package com.cyt.center.module.hrm.service.attendance.record;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.integration.WecomApprovalClient;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.approval.service.WecomApprovalBridge;
import com.cyt.center.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.cyt.center.module.hrm.controller.admin.attendance.vo.leave.HrmAttendanceLeaveCancelReqVO;
import com.cyt.center.module.hrm.controller.admin.attendance.vo.leave.HrmAttendanceLeaveCreateReqVO;
import com.cyt.center.module.hrm.controller.admin.attendance.vo.leave.HrmAttendanceLeavePageReqVO;
import com.cyt.center.module.hrm.controller.admin.employee.vo.employee.HrmEmployeeListReqVO;
import com.cyt.center.module.hrm.dal.dataobject.attendance.record.HrmAttendanceLeaveDO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.dal.mysql.attendance.record.HrmAttendanceLeaveMapper;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertList;
import static com.cyt.center.module.approval.enums.ErrorCodeConstants.WECOM_CANCEL_IN_WECOM;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.ATTENDANCE_LEAVE_NOT_EXISTS;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.ATTENDANCE_LEAVE_TIME_CONFLICT;

/**
 * HRM 考勤请假 Service 实现类。审批出口在企业微信（经 common-api 网关），
 * 不再起 OA 内部的 Warm-Flow 流程；审批链用企微模板自带的审批流，结论由「同步审批状态」回读。
 *
 * @author 芋道源码
 */
@Service
@Validated
public class HrmAttendanceLeaveServiceImpl implements HrmAttendanceLeaveService {

    public static final String LEAVE_PROCESS_KEY = "hrm_attendance_leave";

    @Resource
    private HrmAttendanceLeaveMapper attendanceLeaveMapper;
    @Resource
    private HrmEmployeeService employeeService;
    @Resource
    private WecomApprovalBridge bridge;
    @Resource
    private CenterProperties properties;

    @Override
    @SuppressWarnings("ExtractMethodRecommender")
    @Transactional(rollbackFor = Exception.class)
    public Long createLeave(Long userId, HrmAttendanceLeaveCreateReqVO reqVO) {
        // 1.1 校验当前用户已关联员工
        HrmEmployeeDO employee = employeeService.validateEmployeeBySelf(userId);
        // 1.2 锁定员工，避免同一员工并发创建重叠请假
        employeeService.validateEmployeeExistsForUpdate(employee.getId());
        // 1.3 校验请假时间未与审批中、已通过的申请重叠
        Long conflictCount = attendanceLeaveMapper.selectCountByEmployeeIdAndTimeRangeAndApprovalStatusIn(
                employee.getId(), new LocalDateTime[]{reqVO.getStartTime(), reqVO.getEndTime()},
                Arrays.asList(BpmProcessInstanceStatusEnum.RUNNING.getStatus(), BpmProcessInstanceStatusEnum.APPROVE.getStatus()));
        if (conflictCount > 0) {
            throw exception(ATTENDANCE_LEAVE_TIME_CONFLICT);
        }

        // 2. 创建请假记录
        HrmAttendanceLeaveDO attendanceLeave = BeanUtils.toBean(reqVO, HrmAttendanceLeaveDO.class)
                .setEmployeeId(employee.getId()).setApprovalStatus(BpmProcessInstanceStatusEnum.RUNNING.getStatus());
        attendanceLeaveMapper.insert(attendanceLeave);

        // 3. 发起企微审批，审批链用模板自带的审批流；出单失败随事务回滚，记录不会留半截
        bridge.submit("hrm-leave", properties.getWecom().getHrmLeaveTemplate(),
                attendanceLeave.getId(), bridge.wecomUserid(userId), List.of(),
                approvalFields(attendanceLeave), approvalSummary(attendanceLeave));
        return attendanceLeave.getId();
    }

    @Override
    public HrmAttendanceLeaveDO syncLeaveApprovalStatus(Long id, Long userId) {
        // 1. 校验请假申请属于当前员工
        HrmEmployeeDO employee = employeeService.validateEmployeeBySelf(userId);
        HrmAttendanceLeaveDO attendanceLeave = validateAttendanceLeaveExists(id);
        if (ObjUtil.notEqual(attendanceLeave.getEmployeeId(), employee.getId())) {
            throw exception(ATTENDANCE_LEAVE_NOT_EXISTS);
        }

        // 2. 回读企微，状态有结论才动本地
        WecomApprovalClient.Approval approval = bridge.sync("hrm-leave", id, null, null);
        Integer status = WecomApprovalBridge.toStatus(approval);
        if (status != null) {
            updateLeaveApprovalStatus(id, null, status,
                    approval.statusLabel() == null ? "" : approval.statusLabel());
        }
        return attendanceLeaveMapper.selectById(id);
    }

    /** 审批单表单值：键用网关绑定里的业务别名，请假时长按实际秒数写入 Vacation 控件。 */
    private Map<String, Object> approvalFields(HrmAttendanceLeaveDO attendanceLeave) {
        Map<String, Object> fields = new LinkedHashMap<>();
        Map<String, Object> vacation = WecomApprovalClient.vacationField(attendanceLeave.getType(),
                attendanceLeave.getStartTime(), attendanceLeave.getEndTime(),
                attendanceLeave.getDay() == null ? 0 : attendanceLeave.getDay().intValue());
        if (attendanceLeave.getDay() != null) {
            vacation.put("duration_seconds",
                    attendanceLeave.getDay().multiply(BigDecimal.valueOf(86400)).longValue());
        }
        fields.put("vacation", vacation);
        fields.put("reason", attendanceLeave.getReason());
        return fields;
    }

    private List<String> approvalSummary(HrmAttendanceLeaveDO attendanceLeave) {
        return List.of(
                WecomApprovalBridge.brief("类型：" + attendanceLeave.getType()),
                WecomApprovalBridge.brief("天数：" + (attendanceLeave.getDay() == null ? "0"
                        : attendanceLeave.getDay().stripTrailingZeros().toPlainString()) + " 天"),
                WecomApprovalBridge.brief("事由：" + attendanceLeave.getReason()));
    }

    @Override
    public void cancelLeave(Long userId, HrmAttendanceLeaveCancelReqVO reqVO) {
        // 1. 校验请假申请属于当前员工
        HrmEmployeeDO employee = employeeService.validateEmployeeBySelf(userId);
        HrmAttendanceLeaveDO attendanceLeave = validateAttendanceLeaveExists(reqVO.getId());
        if (ObjUtil.notEqual(attendanceLeave.getEmployeeId(), employee.getId())) {
            throw exception(ATTENDANCE_LEAVE_NOT_EXISTS);
        }
        // 2. 审批单在企业微信，撤销要在企微侧操作后回来同步
        throw exception(WECOM_CANCEL_IN_WECOM);
    }

    @Override
    public List<HrmAttendanceLeaveDO> getMyAttendanceLeaveList(Long userId) {
        return attendanceLeaveMapper.selectListByEmployeeId(employeeService.validateEmployeeBySelf(userId).getId());
    }

    @Override
    public HrmAttendanceLeaveDO getAttendanceLeave(Long id) {
        return attendanceLeaveMapper.selectById(id);
    }

    @Override
    public PageResult<HrmAttendanceLeaveDO> getAttendanceLeavePage(HrmAttendanceLeavePageReqVO reqVO) {
        // 1. 解析员工筛选条件
        Collection<Long> employeeIds = resolveEmployeeIds(reqVO.getEmployeeId(), reqVO.getDeptIds(),
                reqVO.getEmployeeKeyword());
        if (employeeIds != null && CollUtil.isEmpty(employeeIds)) {
            return PageResult.empty();
        }
        reqVO.setEmployeeIds(employeeIds);

        // 2. 查询请假分页
        return attendanceLeaveMapper.selectPage(reqVO);
    }

    @Override
    public List<HrmAttendanceLeaveDO> getAttendanceLeaveList(HrmAttendanceLeavePageReqVO reqVO) {
        // 1. 解析员工筛选条件
        Collection<Long> employeeIds = resolveEmployeeIds(reqVO.getEmployeeId(), reqVO.getDeptIds(),
                reqVO.getEmployeeKeyword());
        if (employeeIds != null && CollUtil.isEmpty(employeeIds)) {
            return Collections.emptyList();
        }
        reqVO.setEmployeeIds(employeeIds);

        // 2. 查询请假列表
        return attendanceLeaveMapper.selectList(reqVO);
    }

    @Override
    public List<HrmAttendanceLeaveDO> getAttendanceLeaveListByEmployeeId(Long employeeId) {
        return attendanceLeaveMapper.selectListByEmployeeId(employeeId);
    }

    @Override
    public List<HrmAttendanceLeaveDO> getEffectiveAttendanceLeaveListByEmployeeId(Long employeeId) {
        return attendanceLeaveMapper.selectListByEmployeeIdAndApprovalStatus(employeeId, BpmProcessInstanceStatusEnum.APPROVE.getStatus());
    }

    @Override
    public List<HrmAttendanceLeaveDO> getEffectiveAttendanceLeaveListByEmployeeIdsAndTimeRange(
            Collection<Long> employeeIds, LocalDateTime[] leaveTimes) {
        if (CollUtil.isEmpty(employeeIds)) {
            return Collections.emptyList();
        }
        return attendanceLeaveMapper.selectListByEmployeeIdsAndTimeRangeAndApprovalStatus(
                employeeIds, leaveTimes, BpmProcessInstanceStatusEnum.APPROVE.getStatus());
    }

    @Override
    public void updateLeaveApprovalStatus(
            Long id, String processInstanceId, Integer status, String reason) {
        // 1. 仅处理当前审批流程的结束状态
        if (!BpmProcessInstanceStatusEnum.isProcessEndStatus(status)) {
            return;
        }
        HrmAttendanceLeaveDO attendanceLeave = validateAttendanceLeaveExists(id);
        if ((attendanceLeave.getProcessInstanceId() != null
                && ObjUtil.notEqual(attendanceLeave.getProcessInstanceId(), processInstanceId))
                || ObjUtil.notEqual(attendanceLeave.getApprovalStatus(), BpmProcessInstanceStatusEnum.RUNNING.getStatus())) {
            return;
        }

        // 2. 仅允许当前审批中的流程写入一次终态
        attendanceLeaveMapper.updateApprovalResultByIdAndProcessInstanceIdAndApprovalStatus(
                id, processInstanceId, BpmProcessInstanceStatusEnum.RUNNING.getStatus(),
                new HrmAttendanceLeaveDO().setProcessInstanceId(processInstanceId)
                        .setApprovalStatus(status).setApprovalTime(LocalDateTime.now()).setApprovalReason(reason));
    }

    private HrmAttendanceLeaveDO validateAttendanceLeaveExists(Long id) {
        HrmAttendanceLeaveDO attendanceLeave = attendanceLeaveMapper.selectById(id);
        if (attendanceLeave == null) {
            throw exception(ATTENDANCE_LEAVE_NOT_EXISTS);
        }
        return attendanceLeave;
    }

    private Collection<Long> resolveEmployeeIds(
            Long employeeId, List<Long> deptIds, String employeeKeyword) {
        if (employeeId == null && CollUtil.isEmpty(deptIds) && StrUtil.isBlank(employeeKeyword)) {
            return null;
        }
        HrmEmployeeListReqVO employeeReqVO = new HrmEmployeeListReqVO()
                .setIds(employeeId == null ? null : Collections.singletonList(employeeId))
                .setSearch(employeeKeyword).setDeptIds(deptIds);
        return convertList(employeeService.getEmployeeList(employeeReqVO), HrmEmployeeDO::getId);
    }

}
