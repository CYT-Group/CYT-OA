package com.cyt.center.module.oa.service.attendance;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.oa.enums.attendance.OaAttendanceTypeEnum;
import java.time.LocalDateTime;
import com.cyt.center.module.oa.controller.admin.attendance.vo.OaAttendanceMonthReportReqVO;
import com.cyt.center.module.oa.controller.admin.attendance.vo.OaAttendanceMonthReportRespVO;
import com.cyt.center.module.oa.controller.admin.attendance.vo.OaAttendancePageReqVO;
import com.cyt.center.module.oa.controller.admin.attendance.vo.OaAttendanceUpdateReqVO;
import com.cyt.center.module.oa.controller.admin.attendance.vo.OaAttendanceWeekReportReqVO;
import com.cyt.center.module.oa.controller.admin.attendance.vo.OaAttendanceWeekReportRespVO;
import com.cyt.center.module.oa.dal.dataobject.attendance.OaAttendanceDO;

import java.util.List;

/**
 * OA 考勤 Service 接口
 *
 * @author 芋道源码
 */
public interface OaAttendanceService {

    /**
     * 审批通过后新增请假或出差考勤明细
     *
     * @param userId 申请人编号
     * @param type 请假或出差类型
     * @param startTime 申请开始时间
     */
    void createApplyAttendance(Long userId, OaAttendanceTypeEnum type, LocalDateTime startTime);

    /**
     * 执行当前用户打卡
     *
     * @param userId 用户编号
     * @param attendanceIp 考勤 IP
     * @return 考勤记录编号
     */
    Long clockAttendance(Long userId, String attendanceIp);

    /**
     * 更新考勤记录
     *
     * @param updateReqVO 修改信息
     * @param operatorId 当前操作用户编号
     */
    void updateAttendance(OaAttendanceUpdateReqVO updateReqVO, Long operatorId);

    /**
     * 删除考勤记录
     *
     * @param id 考勤记录编号
     * @param operatorId 当前操作用户编号
     */
    void deleteAttendance(Long id, Long operatorId);

    /**
     * 获得考勤记录
     *
     * @param id 考勤记录编号
     * @param operatorId 当前操作用户编号
     * @return 考勤记录
     */
    OaAttendanceDO getAttendance(Long id, Long operatorId);

    /**
     * 获得考勤分页列表
     *
     * @param pageReqVO 分页条件
     * @param operatorId 当前操作用户编号
     * @return 考勤分页
     */
    PageResult<OaAttendanceDO> getAttendancePage(OaAttendancePageReqVO pageReqVO, Long operatorId);

    /**
     * 获得指定用户的考勤分页列表
     *
     * @param pageReqVO 分页条件
     * @param userId 用户编号
     * @return 考勤分页
     */
    PageResult<OaAttendanceDO> getMyAttendancePage(OaAttendancePageReqVO pageReqVO, Long userId);

    /**
     * 获得用户当天考勤列表
     *
     * @param userId 用户编号
     * @return 当天考勤列表
     */
    List<OaAttendanceDO> getTodayAttendanceList(Long userId);

    /**
     * 获得考勤周报
     *
     * @param reqVO 查询条件
     * @param operatorId 当前操作用户编号
     * @return 考勤周报
     */
    List<OaAttendanceWeekReportRespVO> getAttendanceWeekReport(OaAttendanceWeekReportReqVO reqVO, Long operatorId);

    /**
     * 获得考勤月报
     *
     * @param reqVO 查询条件
     * @param operatorId 当前操作用户编号
     * @return 考勤月报
     */
    List<OaAttendanceMonthReportRespVO> getAttendanceMonthReport(OaAttendanceMonthReportReqVO reqVO, Long operatorId);

}
