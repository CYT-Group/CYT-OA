package com.cyt.center.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import com.cyt.center.module.system.model.entity.SysUser;
import com.cyt.center.module.hrm.dal.dataobject.employee.employment.HrmEmployeeChangeRecordDO;
import com.cyt.center.module.hrm.dal.mysql.employee.employment.HrmEmployeeChangeRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

/** 只同步企微通讯录事实；任职、合同、生日等人事信息由各自流程维护。 */
@Service
@RequiredArgsConstructor
public class EmployeeDirectoryArchiveService {
    private final HrmEmployeeMapper employees;
    private final HrmEmployeeChangeRecordMapper changes;

    /** 一轮只查询一次档案；没有变化的身份不重复写库。调用方持有快照事务。 */
    public void syncIdentities(List<SysUser> users) {
        if (users.isEmpty()) return;
        Map<Long, HrmEmployeeDO> existing = new HashMap<>();
        for (HrmEmployeeDO employee : employees.selectList(new LambdaQueryWrapper<HrmEmployeeDO>()
                .in(HrmEmployeeDO::getUserId, users.stream().map(SysUser::getId).toList()))) {
            if (existing.put(employee.getUserId(), employee) != null)
                throw new BizException("同一账号存在多份有效员工档案，请先处理关联冲突");
        }
        for (SysUser user : users) syncIdentity(user, existing.get(user.getId()));
    }

    public void syncIdentity(SysUser user) {
        List<HrmEmployeeDO> matches = employees.selectList(new LambdaQueryWrapper<HrmEmployeeDO>()
                .eq(HrmEmployeeDO::getUserId, user.getId()));
        if (matches.size() > 1) {
            throw new BizException("同一账号存在多份有效员工档案，请先处理关联冲突");
        }
        syncIdentity(user, matches.isEmpty() ? null : matches.getFirst());
    }

    private void syncIdentity(SysUser user, HrmEmployeeDO previous) {
        if (previous == null) {
            // 0 明确表示待完善；企微激活状态不能证明已入职，也不能推算入职日期。
            employees.insert(new HrmEmployeeDO().setUserId(user.getId()).setName(user.getName())
                    .setMobile(user.getMobile()).setEmail(user.getEmail()).setDeptId(user.getMainDeptId())
                    .setPostName(user.getPosition()).setEntryStatus(0));
        } else {
            if (Objects.equals(previous.getName(), user.getName())
                    && Objects.equals(previous.getMobile(), user.getMobile())
                    && Objects.equals(previous.getEmail(), user.getEmail())
                    && Objects.equals(previous.getDeptId(), user.getMainDeptId())
                    && Objects.equals(previous.getPostName(), user.getPosition())) return;
            if (!Objects.equals(previous.getDeptId(), user.getMainDeptId())
                    || !Objects.equals(previous.getPostName(), user.getPosition())) {
                LocalDateTime now = LocalDateTime.now();
                changes.insert(new HrmEmployeeChangeRecordDO().setEmployeeId(previous.getId()).setType(10)
                        .setOldDeptId(previous.getDeptId()).setNewDeptId(user.getMainDeptId())
                        .setOldPostName(previous.getPostName()).setNewPostName(user.getPosition())
                        .setOldLeaderEmployeeId(previous.getLeaderEmployeeId()).setNewLeaderEmployeeId(previous.getLeaderEmployeeId())
                        .setEffectTime(now).setAppliedTime(now).setRemark("企微通讯录同步；任职状态与直属上级保持原值"));
            }
            employees.update(null, new LambdaUpdateWrapper<HrmEmployeeDO>()
                    .eq(HrmEmployeeDO::getId, previous.getId())
                    .set(HrmEmployeeDO::getName, user.getName())
                    .set(HrmEmployeeDO::getMobile, user.getMobile())
                    .set(HrmEmployeeDO::getEmail, user.getEmail())
                    .set(HrmEmployeeDO::getDeptId, user.getMainDeptId())
                    .set(HrmEmployeeDO::getPostName, user.getPosition()));
        }
    }
}
