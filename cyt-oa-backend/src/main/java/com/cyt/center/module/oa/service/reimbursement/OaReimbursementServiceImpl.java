package com.cyt.center.module.oa.service.reimbursement;

import cn.hutool.core.util.ObjUtil;
import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.integration.WecomApprovalClient;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.approval.service.WecomApprovalBridge;
import com.cyt.center.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.cyt.center.module.oa.controller.admin.reimbursement.vo.OaReimbursementPageReqVO;
import com.cyt.center.module.oa.controller.admin.reimbursement.vo.OaReimbursementSaveReqVO;
import com.cyt.center.module.oa.controller.admin.reimbursement.vo.OaReimbursementSubmitReqVO;
import com.cyt.center.module.oa.dal.dataobject.reimbursement.OaReimbursementDO;
import com.cyt.center.module.oa.dal.mysql.reimbursement.OaReimbursementMapper;
import com.cyt.center.module.system.api.dict.DictDataApi;
import com.cyt.center.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.getSumValue;
import static com.cyt.center.module.oa.enums.DictTypeConstants.EXPENSE_TYPE;
import static com.cyt.center.module.oa.enums.ErrorCodeConstants.*;

/**
 * 费用报销 Service 实现类。审批出口在企业微信（经 common-api 网关），
 * 不再起 OA 内部的 Warm-Flow 流程；审批链用企微模板自带的审批流，结论由「同步审批状态」回读。
 *
 * @author 芋道源码
 */
@Service
@Validated
public class OaReimbursementServiceImpl implements OaReimbursementService {

    @Resource
    private OaReimbursementMapper reimbursementMapper;
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
    public Long createReimbursement(OaReimbursementSaveReqVO saveReqVO) {
        // 1. 校验证明人存在
        adminUserApi.validateUser(saveReqVO.getWitnessUserId());

        // 2. 转换申请内容、汇总金额和票据数，保存草稿
        OaReimbursementDO reimbursement = buildReimbursement(saveReqVO)
                .setStatus(BpmProcessInstanceStatusEnum.NOT_START.getStatus());
        reimbursementMapper.insert(reimbursement);
        return reimbursement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReimbursement(OaReimbursementSaveReqVO saveReqVO, Long userId) {
        // 1.1 校验申请可修改
        validateReimbursementEditable(saveReqVO.getId(), userId);
        // 1.2 校验证明人存在
        adminUserApi.validateUser(saveReqVO.getWitnessUserId());

        // 2. 更新费用报销
        OaReimbursementDO reimbursement = buildReimbursement(saveReqVO);
        reimbursementMapper.updateById(reimbursement);
    }

    /**
     * 提交报销：把单据推到企业微信生成审批单。这里故意不开事务——出单是一次最长 30 秒的
     * 外部调用，占着数据库连接没有意义；幂等靠桥里的固定请求号，网关 conflict 挡回时认回原单。
     */
    @Override
    public void submitReimbursement(OaReimbursementSubmitReqVO submitReqVO, Long userId) {
        // 1. 校验申请可提交
        OaReimbursementDO reimbursement = validateReimbursementEditable(submitReqVO.getId(), userId);

        // 2. 发起企微审批，审批链用模板自带的审批流
        bridge.submit("reimbursement", properties.getWecom().getReimbursementTemplate(),
                reimbursement.getId(), bridge.wecomUserid(userId), List.of(),
                approvalFields(reimbursement), approvalSummary(reimbursement));

        // 3. 状态改为审批中
        reimbursementMapper.updateById(new OaReimbursementDO().setId(reimbursement.getId())
                .setStatus(BpmProcessInstanceStatusEnum.RUNNING.getStatus()));
    }

    @Override
    public OaReimbursementDO syncReimbursementStatus(Long id, Long userId) {
        // 1. 校验本人已提交的申请
        OaReimbursementDO reimbursement = validateReimbursementExists(id);
        if (ObjUtil.notEqual(reimbursement.getCreator(), userId.toString())) {
            throw exception(APPLY_ACCESS_DENIED);
        }

        // 2. 回读企微，状态有结论才动本地
        WecomApprovalClient.Approval approval = bridge.sync("reimbursement", id, null, null);
        Integer status = WecomApprovalBridge.toStatus(approval);
        if (status != null) {
            updateReimbursementStatus(id, status);
        }
        return reimbursementMapper.selectById(id);
    }

    /** 审批单表单值：键用网关绑定里的业务别名；明细控件按 Table 逐行展开。 */
    private Map<String, Object> approvalFields(OaReimbursementDO reimbursement) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("reason", reimbursement.getReason());
        fields.put("total_price", reimbursement.getTotalPrice());
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        for (OaReimbursementDO.Item item : reimbursement.getItems()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("expense_type", expenseTypeName(item.getExpenseType()));
            row.put("description", item.getDescription());
            row.put("price", item.getPrice());
            rows.add(row);
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
                .map(com.cyt.center.module.system.api.dict.dto.DictDataRespDTO::getLabel)
                .findFirst().orElse(type.toString());
    }

    private List<String> approvalSummary(OaReimbursementDO reimbursement) {
        return List.of(
                WecomApprovalBridge.brief("报销 ¥" + (reimbursement.getTotalPrice() == null ? "0"
                        : reimbursement.getTotalPrice().stripTrailingZeros().toPlainString())),
                WecomApprovalBridge.brief("票据 " + (reimbursement.getInvoiceCount() == null ? 0
                        : reimbursement.getInvoiceCount()) + " 张"),
                WecomApprovalBridge.brief("事由：" + reimbursement.getReason()));
    }

    @Override
    public OaReimbursementDO getReimbursement(Long id) {
        return reimbursementMapper.selectById(id);
    }

    @Override
    public PageResult<OaReimbursementDO> getReimbursementPage(Long userId, OaReimbursementPageReqVO pageReqVO) {
        return reimbursementMapper.selectPage(userId, pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReimbursementStatus(Long id, Integer status) {
        // 1. 校验申请存在
        validateReimbursementExists(id);

        // 2. 更新审批状态
        reimbursementMapper.updateById(new OaReimbursementDO().setId(id).setStatus(status));
    }

    /**
     * 构造申请及汇总字段
     *
     * @param saveReqVO 申请内容
     * @return 申请及计算后的汇总字段
     */
    private OaReimbursementDO buildReimbursement(OaReimbursementSaveReqVO saveReqVO) {
        // 1. 转换申请及报销明细
        OaReimbursementDO reimbursement = BeanUtils.toBean(saveReqVO, OaReimbursementDO.class);
        reimbursement.setItems(BeanUtils.toBean(saveReqVO.getItems(), OaReimbursementDO.Item.class));
        // 2. 汇总金额和票据数
        reimbursement.setTotalPrice(getSumValue(reimbursement.getItems(), OaReimbursementDO.Item::getPrice, BigDecimal::add, BigDecimal.ZERO))
                .setInvoiceCount(getSumValue(reimbursement.getItems(), OaReimbursementDO.Item::getInvoiceCount, Math::addExact, 0));
        return reimbursement;
    }

    /**
     * 校验申请可修改或提交
     *
     * @param id 申请编号
     * @param userId 当前用户编号
     * @return 本人的未提交申请
     */
    private OaReimbursementDO validateReimbursementEditable(Long id, Long userId) {
        // 1. 查询申请
        OaReimbursementDO reimbursement = reimbursementMapper.selectById(id);
        if (reimbursement == null) {
            throw exception(APPLY_NOT_EXISTS);
        }
        // 2. 校验本人归属
        if (ObjUtil.notEqual(reimbursement.getCreator(), userId.toString())) {
            throw exception(APPLY_ACCESS_DENIED);
        }
        // 3. 只有未提交草稿允许修改或提交
        if (ObjUtil.notEqual(reimbursement.getStatus(), BpmProcessInstanceStatusEnum.NOT_START.getStatus())) {
            throw exception(APPLY_STATUS_INVALID);
        }
        return reimbursement;
    }

    /**
     * 校验费用报销存在
     *
     * @param id 申请编号
     * @return 申请
     */
    private OaReimbursementDO validateReimbursementExists(Long id) {
        OaReimbursementDO reimbursement = reimbursementMapper.selectById(id);
        if (reimbursement == null) {
            throw exception(APPLY_NOT_EXISTS);
        }
        return reimbursement;
    }

}
