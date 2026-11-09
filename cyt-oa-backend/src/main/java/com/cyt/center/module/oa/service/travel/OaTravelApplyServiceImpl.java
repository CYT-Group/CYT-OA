package com.cyt.center.module.oa.service.travel;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.integration.WecomApprovalClient;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.framework.common.util.object.ObjectUtils;
import com.cyt.center.framework.common.util.date.LocalDateTimeUtils;
import com.cyt.center.module.approval.service.WecomApprovalBridge;
import com.cyt.center.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.cyt.center.module.oa.controller.admin.travel.vo.apply.*;
import com.cyt.center.module.oa.dal.dataobject.travel.OaTravelApplyDO;
import com.cyt.center.module.oa.dal.mysql.travel.OaTravelApplyMapper;
import com.cyt.center.module.oa.dal.redis.no.OaNoRedisDAO;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import com.cyt.center.module.oa.service.attendance.OaAttendanceService;
import com.cyt.center.module.oa.enums.attendance.OaAttendanceTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.*;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.approval.enums.ErrorCodeConstants.WECOM_CANCEL_IN_WECOM;
import static com.cyt.center.module.oa.enums.ErrorCodeConstants.*;

/**
 * 出差申请 Service 实现类。审批出口在企业微信（经 common-api 网关），
 * 不再起 OA 内部的 Warm-Flow 流程；审批链用企微模板自带的审批流，结论由「同步审批状态」回读。
 *
 * @author 芋道源码
 */
@Service
@Validated
public class OaTravelApplyServiceImpl implements OaTravelApplyService {

    @Resource
    @Lazy
    private OaAttendanceService attendanceService;

    @Resource
    private OaTravelApplyMapper travelApplyMapper;

    @Resource
    private OaNoRedisDAO noRedisDAO;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private WecomApprovalBridge bridge;
    @Resource
    private CenterProperties properties;

    @Override
    public Map<Long, Integer> getApprovedTravelDaysMap(Collection<Long> userIds, LocalDateTime[] startTime) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return travelApplyMapper.selectDaysMapByCreatorsAndStatusAndStartTime(
                userIds, BpmProcessInstanceStatusEnum.APPROVE.getStatus(), startTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTravelApply(OaTravelApplySaveReqVO reqVO, Long userId) {
        // 1.1 查询并校验申请人
        AdminUserRespDTO user = adminUserApi.validateUser(userId);
        // 1.2 生成单号并校验唯一性
        String no = noRedisDAO.generate(OaNoRedisDAO.TRAVEL_APPLY_NO_PREFIX);
        if (travelApplyMapper.selectByNo(no) != null) {
            throw exception(TRAVEL_NO_DUPLICATE);
        }

        // 2. 创建草稿
        OaTravelApplyDO record = BeanUtils.toBean(reqVO, OaTravelApplyDO.class)
                .setDays(LocalDateTimeUtils.getDaysBetweenCeiling(reqVO.getStartTime(), reqVO.getEndTime())).setNo(no)
                .setDeptId(user.getDeptId()).setStatus(BpmProcessInstanceStatusEnum.NOT_START.getStatus())
                .setReimburseStatus(false);
        travelApplyMapper.insert(record);
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTravelApply(OaTravelApplySaveReqVO reqVO, Long userId) {
        // 1.1 查询并校验单据存在
        OaTravelApplyDO record = validateTravelApplyExists(reqVO.getId());
        // 1.2 校验本人归属
        validateTravelApplyOwner(record, userId);
        // 1.3 校验可编辑状态
        validateTravelApplyEditable(record);

        // 2. 更新单据
        OaTravelApplyDO update = BeanUtils.toBean(reqVO, OaTravelApplyDO.class)
                .setDays(LocalDateTimeUtils.getDaysBetweenCeiling(reqVO.getStartTime(), reqVO.getEndTime()));
        travelApplyMapper.updateById(update);
    }

    /**
     * 提交出差：把单据推到企业微信生成审批单。这里故意不开事务——出单是一次最长 30 秒的
     * 外部调用，占着数据库连接没有意义；幂等靠桥里的固定请求号，网关 conflict 挡回时认回原单。
     */
    @Override
    public void submitTravelApply(OaTravelApplySubmitReqVO submitReqVO, Long userId) {
        // 1.1 查询并校验单据存在
        OaTravelApplyDO record = validateTravelApplyExists(submitReqVO.getId());
        // 1.2 校验本人归属
        validateTravelApplyOwner(record, userId);
        // 1.3 校验可提交状态
        validateTravelApplyEditable(record);

        // 2. 发起企微审批，审批链用模板自带的审批流
        bridge.submit("travel", properties.getWecom().getTravelTemplate(),
                record.getId(), bridge.wecomUserid(userId), List.of(),
                approvalFields(record), approvalSummary(record));

        // 3. 状态改为审批中
        travelApplyMapper.updateById(new OaTravelApplyDO().setId(record.getId())
                .setStatus(BpmProcessInstanceStatusEnum.RUNNING.getStatus()));
    }

    @Override
    public OaTravelApplyDO syncTravelApplyStatus(Long id, Long userId) {
        // 1.1 校验单据存在
        OaTravelApplyDO record = validateTravelApplyExists(id);
        // 1.2 校验本人归属
        validateTravelApplyOwner(record, userId);

        // 2. 回读企微，状态有结论才动本地
        WecomApprovalClient.Approval approval = bridge.sync("travel", id, null, null);
        Integer status = WecomApprovalBridge.toStatus(approval);
        if (status != null) {
            updateTravelApplyStatus(id, status);
        }
        return travelApplyMapper.selectById(id);
    }

    /** 审批单表单值：键用网关绑定里的业务别名，控件 ID 与中文字段名由网关的 field_aliases 负责翻译。 */
    private Map<String, Object> approvalFields(OaTravelApplyDO record) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("reason", record.getReason());
        fields.put("time", WecomApprovalBridge.durationField(record.getStartTime(), record.getEndTime(),
                (record.getDays() == null ? 0 : record.getDays()) * 86400L));
        if (record.getCompanion() != null && !record.getCompanion().isBlank()) {
            fields.put("companion", record.getCompanion());
        }
        if (record.getEstimatedPrice() != null) {
            fields.put("estimated_price", record.getEstimatedPrice());
        }
        return fields;
    }

    private List<String> approvalSummary(OaTravelApplyDO record) {
        return List.of(
                WecomApprovalBridge.brief("出差申请"),
                WecomApprovalBridge.brief("天数：" + (record.getDays() == null ? 0 : record.getDays()) + " 天"),
                WecomApprovalBridge.brief("事由：" + record.getReason()));
    }

    @Override
    public void cancelTravelApply(Long id, Long userId) {
        // 1.1 校验单据存在
        OaTravelApplyDO record = validateTravelApplyExists(id);
        // 1.2 校验本人归属
        validateTravelApplyOwner(record, userId);
        // 1.3 审批单在企业微信，撤销要在企微侧操作后回来同步
        throw exception(WECOM_CANCEL_IN_WECOM);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTravelApply(Long id, Long userId) {
        // 1.1 校验单据存在
        OaTravelApplyDO record = validateTravelApplyExists(id);
        // 1.2 校验本人归属
        validateTravelApplyOwner(record, userId);
        // 1.3 校验可删除状态
        validateTravelApplyEditable(record);

        // 2. 删除本人单据
        travelApplyMapper.deleteById(id);
    }

    @Override
    public OaTravelApplyDO getTravelApply(Long id) {
        return travelApplyMapper.selectById(id);
    }

    @Override
    public PageResult<OaTravelApplyDO> getTravelApplyPage(Long userId, OaTravelApplyPageReqVO reqVO) {
        return travelApplyMapper.selectPage(userId, reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTravelApplyStatus(Long id, Integer status) {
        // 1. 校验申请存在
        OaTravelApplyDO apply = validateTravelApplyExists(id);

        // 2. 更新审批状态
        travelApplyMapper.updateById(new OaTravelApplyDO().setId(id).setStatus(status));

        // 3. 审批通过后生成考勤明细
        if (ObjUtil.equal(status, BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            attendanceService.createApplyAttendance(Long.valueOf(apply.getCreator()),
                    OaAttendanceTypeEnum.TRAVEL, apply.getStartTime());
        }
    }

    @Override
    public List<OaTravelApplyDO> getApprovedTravelApplyList(Long userId) {
        return travelApplyMapper.selectListByCreatorAndStatus(userId, BpmProcessInstanceStatusEnum.APPROVE.getStatus());
    }

    @Override
    public OaTravelApplyDO validateApprovedTravelApply(Long id, Long userId) {
        // 1. 校验申请存在
        OaTravelApplyDO record = validateTravelApplyExists(id);
        // 2. 校验申请归属
        validateTravelApplyOwner(record, userId);
        // 3. 校验申请已通过审批
        if (ObjUtil.notEqual(record.getStatus(), BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            throw exception(TRAVEL_APPLY_NOT_APPROVED);
        }
        // 4. 已报销的行程不再创建或提交报销单
        if (Boolean.TRUE.equals(record.getReimburseStatus())) {
            throw exception(TRAVEL_APPLY_ALREADY_REIMBURSED);
        }
        return record;
    }

    @Override
    public List<OaTravelApplyDO> getTravelApplyList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return travelApplyMapper.selectByIds(ids);
    }

    @Override
    public void updateTravelApplyReimburseStatus(Long id) {
        travelApplyMapper.updateById(new OaTravelApplyDO().setId(id)
                .setReimburseStatus(true));
    }

    /**
     * 校验单据存在
     *
     * @param id 单据编号
     * @return 单据
     */
    private OaTravelApplyDO validateTravelApplyExists(Long id) {
        OaTravelApplyDO record = travelApplyMapper.selectById(id);
        if (record == null) {
            throw exception(TRAVEL_NOT_EXISTS);
        }
        return record;
    }

    /**
     * 校验本人单据
     *
     * @param record 单据
     * @param userId 用户编号
     */
    private void validateTravelApplyOwner(OaTravelApplyDO record, Long userId) {
        if (ObjUtil.notEqual(record.getCreator(), userId.toString())) {
            throw exception(TRAVEL_ACCESS_DENIED);
        }
    }

    /**
     * 校验可编辑状态
     *
     * @param record 单据
     */
    private void validateTravelApplyEditable(OaTravelApplyDO record) {
        if (ObjectUtils.notEqualsAny(record.getStatus(), BpmProcessInstanceStatusEnum.NOT_START.getStatus(),
                BpmProcessInstanceStatusEnum.REJECT.getStatus(), BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            throw exception(TRAVEL_STATUS_INVALID);
        }
    }

}
