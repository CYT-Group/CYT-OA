package com.cyt.center.module.portal.service.esports;

import com.cyt.center.module.portal.controller.app.esports.vo.EsportsBookingCreateReqVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsBookingRespVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsSettingsRespVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsSettingsUpdateReqVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsUserRespVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 电竞室用例（迁自 Portal 的 application/esports.py + routers/esports.py）。
 *
 * 设置驱动的多级审批预约：开放规则（单行）、独占时段冲突检测、审批环节快照、流水。
 * 通知（wecom_sync）依赖 OA 尚未就绪的企微推送通道，本切片不接，随推送切片落地。
 */
public interface PortalEsportsService {

    EsportsSettingsRespVO readSettings(Long userId);

    EsportsSettingsRespVO updateSettings(Long userId, EsportsSettingsUpdateReqVO reqVO);

    List<EsportsUserRespVO> listApprovers(Long userId);

    List<EsportsUserRespVO> listParticipants(Long userId);

    List<EsportsBookingRespVO> listBookings(Long userId, LocalDate dateFrom, LocalDate dateTo,
                                            String scope, String status, String q);

    EsportsBookingRespVO bookingDetail(Long userId, Long bookingId);

    EsportsBookingRespVO createBooking(Long userId, EsportsBookingCreateReqVO reqVO);

    EsportsBookingRespVO actOnBooking(Long userId, Long bookingId, String operation,
                                      String remark, Integer expectedStageIndex);

}
