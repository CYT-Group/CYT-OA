package com.cyt.center.module.oa.service.seal;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.integration.WecomApprovalClient;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.approval.service.WecomApprovalBridge;
import com.cyt.center.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.cyt.center.module.oa.controller.admin.seal.vo.apply.*;
import com.cyt.center.module.oa.dal.dataobject.seal.*;
import com.cyt.center.module.oa.dal.mysql.seal.OaSealApplyMapper;
import com.cyt.center.module.oa.dal.redis.no.OaNoRedisDAO;
import com.cyt.center.module.oa.enums.seal.*;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.approval.enums.ErrorCodeConstants.WECOM_CANCEL_IN_WECOM;
import static com.cyt.center.module.oa.enums.ErrorCodeConstants.*;

/**
 * 用印申请 Service 实现类。审批出口在企业微信（经 common-api 网关），
 * 不再起 OA 内部的 Warm-Flow 流程；审批链用企微模板自带的审批流，结论由「同步审批状态」回读。
 *
 * @author 芋道源码
 */
@Service
@Validated
public class OaSealApplyServiceImpl implements OaSealApplyService {

    @Resource
    private OaSealApplyMapper sealApplyMapper;
    @Resource
    @Lazy
    private OaSealService sealService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private WecomApprovalBridge bridge;
    @Resource
    private CenterProperties properties;
    @Resource
    private OaNoRedisDAO noRedisDAO;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSealApply(OaSealApplySaveReqVO reqVO, Long userId) {
        // 1.1 校验印章和申请内容，草稿不占用预约时段
        OaSealDO seal = sealService.validateSealExists(reqVO.getSealId());
        // 1.2 申请人与所属部门取自平台账号，不接受客户端指定
        adminUserApi.validateUser(userId);
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        // 1.3 生成申请单号，并校验唯一性
        String no = noRedisDAO.generate(OaNoRedisDAO.SEAL_APPLY_NO_PREFIX);
        if (sealApplyMapper.selectByNo(no) != null) {
            throw exception(SEAL_APPLY_NO_DUPLICATE);
        }

        // 2. 保存草稿及印章快照，审批状态与业务状态分别维护
        OaSealApplyDO apply = BeanUtils.toBean(reqVO, OaSealApplyDO.class).setId(null)
                .setNo(no).setUserId(userId).setDeptId(user.getDeptId())
                .setSealNo(seal.getNo()).setSealName(seal.getName()).setKeeperUserId(seal.getKeeperUserId())
                .setSealType(seal.getType()).setKeeperDeptId(seal.getKeeperDeptId())
                .setStatus(BpmProcessInstanceStatusEnum.NOT_START.getStatus()).setUseStatus(OaSealUseStatusEnum.PENDING.getStatus());
        sealApplyMapper.insert(apply);
        return apply.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSealApply(OaSealApplySaveReqVO reqVO, Long userId) {
        // 1.1 校验申请存在及归属
        OaSealApplyDO apply = validateSealApplyExists(reqVO.getId());
        validateOwner(apply, userId);
        // 1.2 仅未提交的草稿允许修改
        validateDraft(apply);
        // 1.3 重新校验申请内容及印章
        OaSealDO seal = sealService.validateSealExists(reqVO.getSealId());

        // 2. 更新白名单字段，申请人、流程及用印状态不允许通用更新
        OaSealApplyDO update = BeanUtils.toBean(reqVO, OaSealApplyDO.class)
                .setSealNo(seal.getNo()).setSealName(seal.getName()).setKeeperUserId(seal.getKeeperUserId())
                .setSealType(seal.getType()).setKeeperDeptId(seal.getKeeperDeptId());
        if (ObjUtil.notEqual(update.getType(), OaSealApplyTypeEnum.CONTRACT.getType())) {
            update.setContractPrice(null).setContractParty(null);
        }
        if (ObjUtil.notEqual(update.getMode(), OaSealUseModeEnum.BORROW.getMode())) {
            update.setExpectedReturnTime(null);
        }
        sealApplyMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSealApply(Long id, Long userId) {
        // 1.1 校验申请归属
        OaSealApplyDO apply = validateSealApplyExists(id);
        validateOwner(apply, userId);
        // 1.2 已提交申请保留审批记录
        validateDraft(apply);

        // 2. 删除草稿
        sealApplyMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitSealApply(Long id, Long userId) {
        // 1.1 查询申请并校验归属
        OaSealApplyDO apply = validateSealApplyExists(id);
        validateOwner(apply, userId);
        // 1.2 校验草稿状态
        validateDraft(apply);
        // 1.3 校验关联印章存在
        OaSealDO seal = sealService.validateSealExists(apply.getSealId());
        // 1.4 校验借用时段冲突
        validateTimeConflict(apply);

        // 2. 更新审批状态，借用申请在审批中参与时段冲突检查
        sealApplyMapper.updateById(new OaSealApplyDO().setId(id).setStatus(BpmProcessInstanceStatusEnum.RUNNING.getStatus())
                .setSealNo(seal.getNo()).setSealName(seal.getName()).setKeeperUserId(seal.getKeeperUserId())
                .setSealType(seal.getType()).setKeeperDeptId(seal.getKeeperDeptId()));

        // 3. 发起企微审批，审批链用模板自带的审批流，返回企微单号
        return bridge.submit("seal", properties.getWecom().getSealTemplate(),
                id, bridge.wecomUserid(userId), List.of(),
                approvalFields(apply), approvalSummary(apply));
    }

    @Override
    public OaSealApplyDO syncSealApplyStatus(Long id, Long userId) {
        // 1.1 校验申请存在及归属
        OaSealApplyDO apply = validateSealApplyExists(id);
        validateOwner(apply, userId);

        // 2. 回读企微，状态有结论才动本地
        WecomApprovalClient.Approval approval = bridge.sync("seal", id, null, null);
        Integer status = WecomApprovalBridge.toStatus(approval);
        if (status != null) {
            updateSealApplyStatus(id, status);
        }
        return sealApplyMapper.selectById(id);
    }

    /** 审批单表单值：键用网关绑定里的业务别名；用印类型按枚举中文名匹配企微单选控件。 */
    private Map<String, Object> approvalFields(OaSealApplyDO apply) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("reason", apply.getReason());
        fields.put("document_title", apply.getDocumentTitle());
        fields.put("document_count", apply.getDocumentCount());
        OaSealApplyTypeEnum type = OaSealApplyTypeEnum.valueOf(apply.getType());
        fields.put("seal_type", type == null ? "" : type.getName());
        if (ObjUtil.equal(apply.getType(), OaSealApplyTypeEnum.CONTRACT.getType())
                && apply.getContractPrice() != null) {
            fields.put("contract_price", apply.getContractPrice());
        }
        return fields;
    }

    private List<String> approvalSummary(OaSealApplyDO apply) {
        return List.of(
                WecomApprovalBridge.brief("用印申请"),
                WecomApprovalBridge.brief("印章：" + apply.getSealName()),
                WecomApprovalBridge.brief("事由：" + apply.getReason()));
    }

    @Override
    public void cancelSealApply(Long id, Long userId) {
        // 1.1 查询申请，台账删除不影响历史申请撤销
        OaSealApplyDO apply = validateSealApplyExists(id);
        // 1.2 校验本人申请
        validateOwner(apply, userId);
        // 1.3 审批单在企业微信，撤销要在企微侧操作后回来同步
        throw exception(WECOM_CANCEL_IN_WECOM);
    }

    /**
     * 接收 BPM 内部审批结果，仅校验申请存在并更新状态
     *
     * @param id 申请编号
     * @param status 审批结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSealApplyStatus(Long id, Integer status) {
        // 1. 校验申请存在
        validateSealApplyExists(id);

        // 2. 更新审批状态，通过仅表示允许用印
        sealApplyMapper.updateById(new OaSealApplyDO().setId(id).setStatus(status));
    }

    @Override
    public OaSealApplyDO getSealApply(Long id) {
        return sealApplyMapper.selectById(id);
    }

    @Override
    public PageResult<OaSealApplyDO> getSealApplyPage(Long userId, OaSealApplyPageReqVO reqVO) {
        return sealApplyMapper.selectPage(userId, reqVO);
    }

    /**
     * 校验借用时段冲突，时间边界使用闭区间
     *
     * @param apply 待提交的申请
     */
    private void validateTimeConflict(OaSealApplyDO apply) {
        // 1. 现场用印及未填写完整借用时间的申请不检查时段冲突
        if (ObjUtil.notEqual(apply.getMode(), OaSealUseModeEnum.BORROW.getMode())
                || apply.getExpectedUseTime() == null || apply.getExpectedReturnTime() == null) {
            return;
        }
        if (apply.getExpectedUseTime().isAfter(apply.getExpectedReturnTime())) {
            throw exception(SEAL_APPLY_TIME_INVALID);
        }
        // 2. 审批中或审批通过的外借申请占用预计时段，不依赖用印登记状态
        if (CollUtil.isNotEmpty(sealApplyMapper.selectListBySealIdAndBorrowTimeConflict(
                apply.getSealId(), apply.getId(), apply.getExpectedUseTime(), apply.getExpectedReturnTime()))) {
            throw exception(SEAL_APPLY_TIME_CONFLICT);
        }
    }

    /**
     * 校验申请存在
     *
     * @param id 申请编号
     * @return 用印申请
     */
    private OaSealApplyDO validateSealApplyExists(Long id) {
        OaSealApplyDO apply = sealApplyMapper.selectById(id);
        if (apply == null) {
            throw exception(SEAL_APPLY_NOT_EXISTS);
        }
        return apply;
    }

    /**
     * 校验申请归属
     *
     * @param apply 用印申请
     * @param userId 申请人
     */
    private void validateOwner(OaSealApplyDO apply, Long userId) {
        if (ObjUtil.notEqual(apply.getUserId(), userId)) {
            throw exception(SEAL_APPLY_NOT_OWNER);
        }
    }

    /**
     * 校验申请未提交
     *
     * @param apply 用印申请
     */
    private void validateDraft(OaSealApplyDO apply) {
        if (ObjUtil.notEqual(apply.getStatus(), BpmProcessInstanceStatusEnum.NOT_START.getStatus())) {
            throw exception(SEAL_APPLY_STATUS_INVALID);
        }
    }

    @Override
    public Long getSealApplyCountBySealId(Long id) {
        return sealApplyMapper.selectCountBySealId(id);
    }

}
