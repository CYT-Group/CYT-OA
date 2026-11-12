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
import com.cyt.center.module.oa.controller.admin.travel.vo.reimbursement.*;
import com.cyt.center.module.oa.dal.dataobject.travel.OaTravelReimbursementDO;
import com.cyt.center.module.oa.dal.postgresql.travel.OaTravelReimbursementMapper;
import com.cyt.center.module.oa.dal.redis.no.OaNoRedisDAO;
import com.cyt.center.module.system.api.dict.DictDataApi;
import com.cyt.center.module.system.api.dict.dto.DictDataRespDTO;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.approval.enums.ErrorCodeConstants.WECOM_CANCEL_IN_WECOM;
import static com.cyt.center.module.oa.enums.DictTypeConstants.EXPENSE_TYPE;
import static com.cyt.center.module.oa.enums.ErrorCodeConstants.*;

/**
 * 出差报销 Service 实现类。审批出口在企业微信（经 common-api 网关），
 * 不再起 OA 内部的 Warm-Flow 流程；审批链用企微模板自带的审批流，结论由「同步审批状态」回读。
 *
 * @author 芋道源码
 */
@Service
@Validated
public class OaTravelReimbursementServiceImpl implements OaTravelReimbursementService {

    @Resource
    private OaTravelReimbursementMapper travelReimbursementMapper;

    @Resource
    private OaNoRedisDAO noRedisDAO;

    @Resource
    private OaTravelApplyService travelApplyService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private WecomApprovalBridge bridge;
    @Resource
    private CenterProperties properties;
    @Resource
    private DictDataApi dictDataApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTravelReimbursement(OaTravelReimbursementSaveReqVO reqVO, Long userId) {
        // 1.1 校验所选申请
        if (reqVO.getTravelApplyId() != null) {
            travelApplyService.validateApprovedTravelApply(reqVO.getTravelApplyId(), userId);
        }
        // 1.2 查询并校验申请人
        AdminUserRespDTO user = adminUserApi.validateUser(userId);
        // 1.3 生成单号并校验唯一性
        String no = noRedisDAO.generate(OaNoRedisDAO.TRAVEL_REIMBURSEMENT_NO_PREFIX);
        if (travelReimbursementMapper.selectByNo(no) != null) {
            throw exception(TRAVEL_NO_DUPLICATE);
        }

        // 2. 创建草稿
        OaTravelReimbursementDO record = buildTravelReimbursement(reqVO)
                .setNo(no)
                .setDeptId(user.getDeptId()).setStatus(BpmProcessInstanceStatusEnum.NOT_START.getStatus())
                .setPayStatus(false);
        travelReimbursementMapper.insert(record);
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTravelReimbursement(OaTravelReimbursementSaveReqVO reqVO, Long userId) {
        // 1.1 查询并校验单据存在
        OaTravelReimbursementDO record = validateTravelReimbursementExists(reqVO.getId());
        // 1.2 校验本人归属
        validateTravelReimbursementOwner(record, userId);
        // 1.3 校验可编辑状态
        validateTravelReimbursementEditable(record);
        // 1.4 校验所选申请
        if (reqVO.getTravelApplyId() != null) {
            travelApplyService.validateApprovedTravelApply(reqVO.getTravelApplyId(), userId);
        }

        // 2. 更新单据
        travelReimbursementMapper.updateById(buildTravelReimbursement(reqVO));
    }

    /**
     * 转换草稿业务字段，统一计算天数和报销总金额
     *
     * @param reqVO 单据内容
     * @return 单据业务字段
     */
    private OaTravelReimbursementDO buildTravelReimbursement(OaTravelReimbursementSaveReqVO reqVO) {
        OaTravelReimbursementDO record = BeanUtils.toBean(reqVO, OaTravelReimbursementDO.class);
        record.setDays(LocalDateTimeUtils.getDaysBetweenCeiling(reqVO.getStartTime(), reqVO.getEndTime()));
        BigDecimal totalPrice = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(record.getItems())) {
            for (OaTravelReimbursementDO.Item item : record.getItems()) {
                if (item.getPrice() != null) {
                    totalPrice = totalPrice.add(item.getPrice());
                }
            }
        }
        record.setTotalPrice(record.getItems() == null ? null : totalPrice.setScale(2, RoundingMode.HALF_UP));
        return record;
    }

    /**
     * 提交出差报销：把单据推到企业微信生成审批单。这里故意不开事务——出单是一次最长 30 秒的
     * 外部调用，占着数据库连接没有意义；幂等靠桥里的固定请求号，网关 conflict 挡回时认回原单。
     */
    @Override
    public void submitTravelReimbursement(OaTravelReimbursementSubmitReqVO submitReqVO, Long userId) {
        // 1.1 查询并校验单据存在
        OaTravelReimbursementDO record = validateTravelReimbursementExists(submitReqVO.getId());
        // 1.2 校验本人归属
        validateTravelReimbursementOwner(record, userId);
        // 1.3 校验可提交状态
        validateTravelReimbursementEditable(record);
        // 1.4 校验所选申请仍可关联
        if (record.getTravelApplyId() != null) {
            travelApplyService.validateApprovedTravelApply(record.getTravelApplyId(), userId);
        }

        // 2. 发起企微审批，审批链用模板自带的审批流
        bridge.submit("travel-reimbursement", properties.getWecom().getTravelReimbursementTemplate(),
                record.getId(), bridge.wecomUserid(userId), List.of(),
                approvalFields(record), approvalSummary(record));

        // 3. 状态改为审批中
        travelReimbursementMapper.updateById(new OaTravelReimbursementDO().setId(record.getId())
                .setStatus(BpmProcessInstanceStatusEnum.RUNNING.getStatus()));
    }

    @Override
    public OaTravelReimbursementDO syncTravelReimbursementStatus(Long id, Long userId) {
        // 1.1 校验单据存在
        OaTravelReimbursementDO record = validateTravelReimbursementExists(id);
        // 1.2 校验本人归属
        validateTravelReimbursementOwner(record, userId);

        // 2. 回读企微，状态有结论才动本地
        WecomApprovalClient.Approval approval = bridge.sync("travel-reimbursement", id, null, null);
        Integer status = WecomApprovalBridge.toStatus(approval);
        if (status != null) {
            updateTravelReimbursementStatus(id, status);
        }
        return travelReimbursementMapper.selectById(id);
    }

    /** 审批单表单值：键用网关绑定里的业务别名；明细控件按 Table 逐行展开。 */
    private Map<String, Object> approvalFields(OaTravelReimbursementDO record) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("reason", record.getReason());
        fields.put("time", WecomApprovalBridge.durationField(record.getStartTime(), record.getEndTime(),
                (record.getDays() == null ? 0 : record.getDays()) * 86400L));
        fields.put("total_price", record.getTotalPrice());
        List<Map<String, Object>> rows = new ArrayList<>();
        if (CollUtil.isNotEmpty(record.getItems())) {
            for (OaTravelReimbursementDO.Item item : record.getItems()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("expense_type", expenseTypeName(item.getExpenseType()));
                row.put("description", item.getDescription());
                row.put("price", item.getPrice());
                rows.add(row);
            }
        }
        fields.put("items", rows);
        return fields;
    }

    private String expenseTypeName(Integer type) {
        if (type == null) {
            return "";
        }
        return dictDataApi.getDictDataList(EXPENSE_TYPE).stream()
                .filter(d -> Objects.equals(d.getValue(), type.toString()))
                .map(DictDataRespDTO::getLabel)
                .findFirst().orElse(type.toString());
    }

    private List<String> approvalSummary(OaTravelReimbursementDO record) {
        return List.of(
                WecomApprovalBridge.brief("差旅报销 ¥" + (record.getTotalPrice() == null ? "0"
                        : record.getTotalPrice().stripTrailingZeros().toPlainString())),
                WecomApprovalBridge.brief("天数：" + (record.getDays() == null ? 0 : record.getDays()) + " 天"),
                WecomApprovalBridge.brief("事由：" + record.getReason()));
    }

    @Override
    public void cancelTravelReimbursement(Long id, Long userId) {
        // 1.1 校验单据存在
        OaTravelReimbursementDO record = validateTravelReimbursementExists(id);
        // 1.2 校验本人归属
        validateTravelReimbursementOwner(record, userId);
        // 1.3 审批单在企业微信，撤销要在企微侧操作后回来同步
        throw exception(WECOM_CANCEL_IN_WECOM);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTravelReimbursement(Long id, Long userId) {
        // 1.1 校验单据存在
        OaTravelReimbursementDO record = validateTravelReimbursementExists(id);
        // 1.2 校验本人归属
        validateTravelReimbursementOwner(record, userId);
        // 1.3 校验可删除状态
        validateTravelReimbursementEditable(record);

        // 2. 删除本人单据
        travelReimbursementMapper.deleteById(id);
    }

    @Override
    public OaTravelReimbursementDO getTravelReimbursement(Long id) {
        return travelReimbursementMapper.selectById(id);
    }

    @Override
    public PageResult<OaTravelReimbursementDO> getTravelReimbursementPage(Long userId, OaTravelReimbursementPageReqVO reqVO) {
        return travelReimbursementMapper.selectPage(userId, reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTravelReimbursementStatus(Long id, Integer status) {
        // 1. 校验报销单存在
        OaTravelReimbursementDO record = validateTravelReimbursementExists(id);
        // 重复结果不再触发关联申请的报销状态更新
        if (ObjUtil.equal(record.getStatus(), status)) {
            return;
        }

        // 2. 更新审批状态
        travelReimbursementMapper.updateById(new OaTravelReimbursementDO().setId(id).setStatus(status));

        // 3. 审批通过时标记关联申请已报销，不修改支付状态
        if (ObjUtil.equal(status, BpmProcessInstanceStatusEnum.APPROVE.getStatus()) && record.getTravelApplyId() != null) {
            travelApplyService.updateTravelApplyReimburseStatus(record.getTravelApplyId());
        }
    }

    /**
     * 校验单据存在
     *
     * @param id 单据编号
     * @return 单据
     */
    private OaTravelReimbursementDO validateTravelReimbursementExists(Long id) {
        OaTravelReimbursementDO record = travelReimbursementMapper.selectById(id);
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
    private void validateTravelReimbursementOwner(OaTravelReimbursementDO record, Long userId) {
        if (ObjUtil.notEqual(record.getCreator(), userId.toString())) {
            throw exception(TRAVEL_ACCESS_DENIED);
        }
    }

    /**
     * 校验可编辑状态
     *
     * @param record 单据
     */
    private void validateTravelReimbursementEditable(OaTravelReimbursementDO record) {
        if (ObjectUtils.notEqualsAny(record.getStatus(), BpmProcessInstanceStatusEnum.NOT_START.getStatus(),
                BpmProcessInstanceStatusEnum.REJECT.getStatus(), BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            throw exception(TRAVEL_STATUS_INVALID);
        }
    }

}
