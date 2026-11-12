package com.cyt.center.module.oa.service.supply;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.cyt.center.framework.common.enums.CommonStatusEnum;
import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.integration.WecomApprovalClient;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.framework.common.util.object.ObjectUtils;
import com.cyt.center.module.approval.service.WecomApprovalBridge;
import com.cyt.center.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.cyt.center.module.oa.controller.admin.supply.vo.apply.*;
import com.cyt.center.module.oa.controller.admin.supply.vo.issue.*;
import com.cyt.center.module.oa.dal.dataobject.supply.*;
import com.cyt.center.module.oa.dal.postgresql.supply.*;
import com.cyt.center.module.oa.dal.redis.no.OaNoRedisDAO;
import com.cyt.center.module.oa.enums.supply.*;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.*;
import static com.cyt.center.module.approval.enums.ErrorCodeConstants.WECOM_CANCEL_IN_WECOM;
import static com.cyt.center.module.oa.enums.ErrorCodeConstants.*;

/**
 * 用品领用申请 Service 实现类。审批出口在企业微信（经 common-api 网关），
 * 不再起 OA 内部的 Warm-Flow 流程；审批链用企微模板自带的审批流，结论由「同步审批状态」回读。
 *
 * @author 芋道源码
 */
@Service
@Validated
public class OaSupplyApplyServiceImpl implements OaSupplyApplyService {

    @Resource
    private OaSupplyApplyMapper supplyApplyMapper;
    @Resource
    private OaSupplyApplyItemMapper supplyApplyItemMapper;
    @Resource
    private OaSupplyItemService supplyItemService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private WecomApprovalBridge bridge;
    @Resource
    private CenterProperties properties;
    @Resource
    private OaNoRedisDAO noRedisDAO;

    @Override
    public Long getUnreturnedSupplyItemCount(Long itemId) {
        return supplyApplyItemMapper.selectCountByItemIdAndUnreturned(itemId, OaSupplyManageTypeEnum.BORROWABLE.getType());
    }

    @Override
    public Long getPendingSupplyItemCount(Long itemId) {
        return supplyApplyItemMapper.selectCountByItemIdAndStatus(itemId, OaSupplyApplyItemStatusEnum.PENDING.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSupplyApply(OaSupplyApplySaveReqVO createReqVO, Long userId) {
        // 1.1 校验领用物品有效性，必填信息由保存 VO 校验
        List<OaSupplyApplyItemDO> applyItems = BeanUtils.toBean(createReqVO.getItems(), OaSupplyApplyItemDO.class);
        Map<Long, OaSupplyItemDO> items = validateSupplyApplyItems(applyItems);
        // 1.2 查询申请人部门
        AdminUserRespDTO user = adminUserApi.validateUser(userId);
        // 1.3 生成并校验申请单号
        String no = noRedisDAO.generate(OaNoRedisDAO.SUPPLY_APPLY_NO_PREFIX);
        if (supplyApplyMapper.selectByNo(no) != null) {
            throw exception(SUPPLY_APPLY_NO_DUPLICATE);
        }

        // 2.1 保存草稿，创建人由公共审计字段填写
        OaSupplyApplyDO apply = BeanUtils.toBean(createReqVO, OaSupplyApplyDO.class).setId(null)
                .setNo(no).setDeptId(user.getDeptId()).setStatus(BpmProcessInstanceStatusEnum.NOT_START.getStatus());
        supplyApplyMapper.insert(apply);
        // 2.2 保存领用明细
        saveSupplyApplyItems(apply.getId(), applyItems, items);
        return apply.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplyApply(OaSupplyApplySaveReqVO updateReqVO, Long userId) {
        // 1.1 校验本人可编辑申请
        OaSupplyApplyDO apply = validateSupplyApplyExists(updateReqVO.getId());
        validateSupplyApplyOwner(apply, userId);
        validateSupplyApplyEditable(apply);
        // 1.2 校验新的领用明细
        List<OaSupplyApplyItemDO> applyItems = BeanUtils.toBean(updateReqVO.getItems(), OaSupplyApplyItemDO.class);
        Map<Long, OaSupplyItemDO> items = validateSupplyApplyItems(applyItems);

        // 2.1 更新申请内容
        supplyApplyMapper.updateById(BeanUtils.toBean(updateReqVO, OaSupplyApplyDO.class));
        // 2.2 重新保存草稿明细
        supplyApplyItemMapper.deleteByApplyId(apply.getId());
        saveSupplyApplyItems(apply.getId(), applyItems, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplyApply(Long id, Long userId) {
        // 1.1 校验申请归属
        OaSupplyApplyDO apply = validateSupplyApplyExists(id);
        validateSupplyApplyOwner(apply, userId);
        // 1.2 校验申请可编辑
        validateSupplyApplyEditable(apply);

        // 2. 删除申请及明细
        supplyApplyItemMapper.deleteByApplyId(id);
        supplyApplyMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitSupplyApply(Long id, Long userId) {
        // 1.1 校验申请归属及状态
        OaSupplyApplyDO apply = validateSupplyApplyExists(id);
        validateSupplyApplyOwner(apply, userId);
        validateSupplyApplyEditable(apply);
        // 1.2 校验领用物品仍然有效，必填信息已在保存时校验，提交不扣减库存
        List<OaSupplyApplyItemDO> items = supplyApplyItemMapper.selectListByApplyId(id);
        validateSupplyApplyItems(items);

        // 2. 更新审批中状态
        int count = supplyApplyMapper.updateStatusAndClearProcessInstanceId(id, apply.getStatus(), BpmProcessInstanceStatusEnum.RUNNING.getStatus());
        if (count == 0) {
            throw exception(SUPPLY_APPLY_STATUS_INVALID);
        }

        // 3. 发起企微审批，审批链用模板自带的审批流，返回企微单号
        return bridge.submit("supply", properties.getWecom().getSupplyTemplate(),
                id, bridge.wecomUserid(userId), List.of(),
                approvalFields(apply, items), approvalSummary(apply, items));
    }

    @Override
    public OaSupplyApplyDO syncSupplyApplyStatus(Long id, Long userId) {
        // 1.1 校验申请归属
        OaSupplyApplyDO apply = validateSupplyApplyExists(id);
        validateSupplyApplyOwner(apply, userId);

        // 2. 回读企微，状态有结论才动本地
        WecomApprovalClient.Approval approval = bridge.sync("supply", id, null, null);
        Integer status = WecomApprovalBridge.toStatus(approval);
        if (status != null) {
            updateSupplyApplyStatus(id, null, status);
        }
        return supplyApplyMapper.selectById(id);
    }

    /** 审批单表单值：键用网关绑定里的业务别名；明细控件按 Table 逐行展开。 */
    private Map<String, Object> approvalFields(OaSupplyApplyDO apply, List<OaSupplyApplyItemDO> items) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("reason", apply.getReason());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (OaSupplyApplyItemDO item : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("item_name", item.getItemName());
            row.put("quantity", item.getApplyQuantity());
            rows.add(row);
        }
        fields.put("items", rows);
        return fields;
    }

    private List<String> approvalSummary(OaSupplyApplyDO apply, List<OaSupplyApplyItemDO> items) {
        int total = 0;
        for (OaSupplyApplyItemDO item : items) {
            total += item.getApplyQuantity() == null ? 0 : item.getApplyQuantity();
        }
        return List.of(
                WecomApprovalBridge.brief("用品领用"),
                WecomApprovalBridge.brief("共 " + total + " 件"),
                WecomApprovalBridge.brief("事由：" + apply.getReason()));
    }

    @Override
    public void cancelSupplyApply(Long id, Long userId) {
        // 1.1 校验申请归属
        OaSupplyApplyDO apply = validateSupplyApplyExists(id);
        validateSupplyApplyOwner(apply, userId);
        // 1.2 审批单在企业微信，撤销要在企微侧操作后回来同步
        throw exception(WECOM_CANCEL_IN_WECOM);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplyApplyStatus(Long id, String processInstanceId, Integer status) {
        // 1. 校验审批结果属于当前申请
        OaSupplyApplyDO apply = validateSupplyApplyExists(id);
        // 忽略不属于当前流程的审批结果
        if (apply.getProcessInstanceId() != null && ObjUtil.notEqual(apply.getProcessInstanceId(), processInstanceId)) {
            return;
        }

        // 2.1 保存审批状态
        supplyApplyMapper.updateById(new OaSupplyApplyDO().setId(id).setStatus(status));
        // 2.2 审批通过后允许发放，重复回调不重置已发放明细
        if (ObjUtil.equal(status, BpmProcessInstanceStatusEnum.APPROVE.getStatus()) && ObjUtil.notEqual(apply.getStatus(), status)) {
            supplyApplyItemMapper.updateStatusByApplyId(id, OaSupplyApplyItemStatusEnum.PENDING.getStatus());
        }
    }

    @Override
    public OaSupplyApplyDO getSupplyApply(Long id) {
        return supplyApplyMapper.selectById(id);
    }

    @Override
    public PageResult<OaSupplyApplyDO> getSupplyApplyPage(OaSupplyApplyPageReqVO reqVO, Long userId) {
        return supplyApplyMapper.selectPage(reqVO, userId);
    }

    @Override
    public List<OaSupplyApplyDO> getSupplyApplyList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return supplyApplyMapper.selectByIds(ids);
    }

    @Override
    public List<OaSupplyApplyItemDO> getSupplyApplyItemList(Long applyId) {
        return supplyApplyItemMapper.selectListByApplyId(applyId);
    }


    /**
     * 校验用品申请存在
     *
     * @param id 编号
     * @return 用品申请
     */
    private OaSupplyApplyDO validateSupplyApplyExists(Long id) {
        OaSupplyApplyDO apply = supplyApplyMapper.selectById(id);
        if (apply == null) {
            throw exception(SUPPLY_APPLY_NOT_EXISTS);
        }
        return apply;
    }

    /**
     * 校验申请归属
     *
     * @param apply 申请
     * @param userId 操作人编号
     */
    private void validateSupplyApplyOwner(OaSupplyApplyDO apply, Long userId) {
        if (ObjUtil.notEqual(apply.getCreator(), userId.toString())) {
            throw exception(SUPPLY_APPLY_ACCESS_DENIED);
        }
    }

    /**
     * 校验申请可编辑
     *
     * @param apply 申请
     */
    private void validateSupplyApplyEditable(OaSupplyApplyDO apply) {
        // 草稿、驳回和取消的申请允许编辑
        if (ObjectUtils.notEqualsAny(apply.getStatus(), BpmProcessInstanceStatusEnum.NOT_START.getStatus(),
                BpmProcessInstanceStatusEnum.REJECT.getStatus(), BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            throw exception(SUPPLY_APPLY_STATUS_INVALID);
        }
    }

    /**
     * 校验领用物品有效且不重复
     *
     * @param items 领用明细
     * @return 物品映射
     */
    private Map<Long, OaSupplyItemDO> validateSupplyApplyItems(List<OaSupplyApplyItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = convertSet(items, OaSupplyApplyItemDO::getItemId);
        Map<Long, OaSupplyItemDO> itemMap = supplyItemService.getSupplyItemMap(ids);
        if (ids.size() != items.size() || itemMap.size() != ids.size()) {
            throw exception(SUPPLY_APPLY_ITEM_INVALID);
        }
        for (OaSupplyApplyItemDO item : items) {
            // TODO DONE @AI：用品启用状态使用 CommonStatusEnum，避免魔法值
            if (ObjUtil.notEqual(itemMap.get(item.getItemId()).getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
                throw exception(SUPPLY_APPLY_ITEM_INVALID);
            }
        }
        return itemMap;
    }

    /**
     * 保存申请明细
     *
     * @param applyId 申请编号
     * @param applyItems 领用明细
     * @param items 物品映射
     */
    private void saveSupplyApplyItems(Long applyId, List<OaSupplyApplyItemDO> applyItems,
                                      Map<Long, OaSupplyItemDO> items) {
        if (CollUtil.isEmpty(applyItems)) {
            return;
        }
        for (OaSupplyApplyItemDO applyItem : applyItems) {
            OaSupplyItemDO item = items.get(applyItem.getItemId());
            applyItem.setApplyId(applyId).setItemName(item.getName()).setModel(item.getModel())
                    .setUnit(item.getUnit()).setManageType(item.getManageType())
                    .setIssuedQuantity(0).setReturnedQuantity(0).setStatus(OaSupplyApplyItemStatusEnum.APPLYING.getStatus());
            supplyApplyItemMapper.insert(applyItem);
        }
    }

}
