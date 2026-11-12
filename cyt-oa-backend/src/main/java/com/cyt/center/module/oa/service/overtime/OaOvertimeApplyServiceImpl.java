package com.cyt.center.module.oa.service.overtime;

import cn.hutool.core.util.ObjUtil;
import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.integration.WecomApprovalClient;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.date.LocalDateTimeUtils;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.approval.service.WecomApprovalBridge;
import com.cyt.center.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.cyt.center.module.oa.controller.admin.overtime.vo.OaOvertimeApplyPageReqVO;
import com.cyt.center.module.oa.controller.admin.overtime.vo.OaOvertimeApplySaveReqVO;
import com.cyt.center.module.oa.controller.admin.overtime.vo.OaOvertimeApplySubmitReqVO;
import com.cyt.center.module.oa.dal.dataobject.overtime.OaOvertimeApplyDO;
import com.cyt.center.module.oa.dal.postgresql.overtime.OaOvertimeApplyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.oa.enums.ErrorCodeConstants.*;

/**
 * 加班申请 Service 实现类。审批出口在企业微信（经 common-api 网关），
 * 不再起 OA 内部的 Warm-Flow 流程；审批链用企微模板自带的审批流，结论由「同步审批状态」回读。
 *
 * @author 芋道源码
 */
@Service
@Validated
public class OaOvertimeApplyServiceImpl implements OaOvertimeApplyService {

    @Resource
    private OaOvertimeApplyMapper overtimeApplyMapper;
    @Resource
    private WecomApprovalBridge bridge;
    @Resource
    private CenterProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOvertimeApply(OaOvertimeApplySaveReqVO saveReqVO) {
        // TODO DONE @AI：天数计算复用 LocalDateTimeUtils，仍保留一位小数并四舍五入。
        // 1. 转换申请内容并计算天数
        OaOvertimeApplyDO overtimeApply = BeanUtils.toBean(saveReqVO, OaOvertimeApplyDO.class)
                .setDays(LocalDateTimeUtils.getDaysBetween(saveReqVO.getStartTime(), saveReqVO.getEndTime(), 1));

        // 2. 保存草稿
        overtimeApply.setId(null).setStatus(BpmProcessInstanceStatusEnum.NOT_START.getStatus());
        overtimeApplyMapper.insert(overtimeApply);
        return overtimeApply.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOvertimeApply(OaOvertimeApplySaveReqVO saveReqVO, Long userId) {
        // 1. 校验申请可修改
        validateOvertimeApplyEditable(saveReqVO.getId(), userId);

        // 2. 更新加班申请
        OaOvertimeApplyDO overtimeApply = BeanUtils.toBean(saveReqVO, OaOvertimeApplyDO.class)
                .setDays(LocalDateTimeUtils.getDaysBetween(saveReqVO.getStartTime(), saveReqVO.getEndTime(), 1));
        overtimeApplyMapper.updateById(overtimeApply);
    }

    /**
     * 提交加班：把单据推到企业微信生成审批单。这里故意不开事务——出单是一次最长 30 秒的
     * 外部调用，占着数据库连接没有意义；幂等靠桥里的固定请求号，网关 conflict 挡回时认回原单。
     */
    @Override
    public void submitOvertimeApply(OaOvertimeApplySubmitReqVO submitReqVO, Long userId) {
        // 1. 校验申请可提交
        OaOvertimeApplyDO overtimeApply = validateOvertimeApplyEditable(submitReqVO.getId(), userId);

        // 2. 发起企微审批，审批链用模板自带的审批流
        bridge.submit("overtime", properties.getWecom().getOvertimeTemplate(),
                overtimeApply.getId(), bridge.wecomUserid(userId), List.of(),
                approvalFields(overtimeApply), approvalSummary(overtimeApply));

        // 3. 状态改为审批中
        overtimeApplyMapper.updateById(new OaOvertimeApplyDO().setId(overtimeApply.getId())
                .setStatus(BpmProcessInstanceStatusEnum.RUNNING.getStatus()));
    }

    @Override
    public OaOvertimeApplyDO syncOvertimeApplyStatus(Long id, Long userId) {
        // 1. 校验本人已提交的申请
        OaOvertimeApplyDO overtimeApply = validateOvertimeApplyExists(id);
        if (ObjUtil.notEqual(overtimeApply.getCreator(), userId.toString())) {
            throw exception(APPLY_ACCESS_DENIED);
        }

        // 2. 回读企微，状态有结论才动本地
        WecomApprovalClient.Approval approval = bridge.sync("overtime", id, null, null);
        Integer status = WecomApprovalBridge.toStatus(approval);
        if (status != null) {
            updateOvertimeApplyStatus(id, status);
        }
        return overtimeApplyMapper.selectById(id);
    }

    @Override
    public OaOvertimeApplyDO getOvertimeApply(Long id) {
        return overtimeApplyMapper.selectById(id);
    }

    @Override
    public PageResult<OaOvertimeApplyDO> getOvertimeApplyPage(Long userId, OaOvertimeApplyPageReqVO pageReqVO) {
        return overtimeApplyMapper.selectPage(userId, pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOvertimeApplyStatus(Long id, Integer status) {
        // 1. 校验申请存在
        validateOvertimeApplyExists(id);

        // 2. 更新审批状态
        overtimeApplyMapper.updateById(new OaOvertimeApplyDO().setId(id).setStatus(status));
    }

    /** 审批单表单值：键用网关绑定里的业务别名，控件 ID 与中文字段名由网关的 field_aliases 负责翻译。 */
    private Map<String, Object> approvalFields(OaOvertimeApplyDO overtimeApply) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("reason", overtimeApply.getReason());
        BigDecimal days = overtimeApply.getDays();
        fields.put("time", WecomApprovalBridge.durationField(overtimeApply.getStartTime(),
                overtimeApply.getEndTime(), days == null ? 0 : days.multiply(BigDecimal.valueOf(86400)).longValue()));
        return fields;
    }

    private List<String> approvalSummary(OaOvertimeApplyDO overtimeApply) {
        return List.of(
                WecomApprovalBridge.brief("加班申请"),
                WecomApprovalBridge.brief("时长：" + (overtimeApply.getDays() == null ? "0"
                        : overtimeApply.getDays().stripTrailingZeros().toPlainString()) + " 天"),
                WecomApprovalBridge.brief("事由：" + overtimeApply.getReason()));
    }

    /**
     * 校验申请可修改或提交
     *
     * @param id 申请编号
     * @param userId 当前用户编号
     * @return 本人的未提交申请
     */
    private OaOvertimeApplyDO validateOvertimeApplyEditable(Long id, Long userId) {
        // 1. 查询申请
        OaOvertimeApplyDO overtimeApply = overtimeApplyMapper.selectById(id);
        if (overtimeApply == null) {
            throw exception(APPLY_NOT_EXISTS);
        }
        // 2. 校验本人归属
        if (ObjUtil.notEqual(overtimeApply.getCreator(), userId.toString())) {
            throw exception(APPLY_ACCESS_DENIED);
        }
        // 3. 只有未提交草稿允许修改或提交
        if (ObjUtil.notEqual(overtimeApply.getStatus(), BpmProcessInstanceStatusEnum.NOT_START.getStatus())) {
            throw exception(APPLY_STATUS_INVALID);
        }
        return overtimeApply;
    }

    /**
     * 校验加班申请存在
     *
     * @param id 申请编号
     * @return 申请
     */
    private OaOvertimeApplyDO validateOvertimeApplyExists(Long id) {
        OaOvertimeApplyDO overtimeApply = overtimeApplyMapper.selectById(id);
        if (overtimeApply == null) {
            throw exception(APPLY_NOT_EXISTS);
        }
        return overtimeApply;
    }

}
