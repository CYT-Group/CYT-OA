package com.cyt.center.module.hrm.service.employee.info;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.hrm.controller.admin.employee.vo.employee.HrmEmployeeSupplementReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.dal.dataobject.employee.employment.HrmEmployeeChangeRecordDO;
import com.cyt.center.module.hrm.dal.postgresql.employee.info.HrmEmployeeMapper;
import com.cyt.center.module.hrm.dal.postgresql.employee.employment.HrmEmployeeChangeRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HrmEmployeeSupplementService {
    private final HrmEmployeeMapper employees;
    private final HrmEmployeeChangeRecordMapper changes;

    @Transactional(rollbackFor = Exception.class)
    public void update(HrmEmployeeSupplementReqVO cmd) {
        HrmEmployeeDO old = employees.selectByIdForUpdate(cmd.id());
        if (old == null) throw new BizException("员工档案不存在");
        boolean unconfirmed = old.getEntryStatus() == null || old.getEntryStatus() == 0;
        if (!unconfirmed && (!Objects.equals(old.getEntryStatus(), cmd.entryStatus())
                || !Objects.equals(old.getType(), cmd.type()) || !Objects.equals(old.getStatus(), cmd.status())
                || !Objects.equals(old.getEntryTime(), cmd.entryTime()))) {
            throw new BizException("已登记的任职信息请通过入转调离流程变更");
        }
        if (unconfirmed && cmd.entryStatus() != null && cmd.entryStatus() != 0) {
            if (cmd.entryStatus() != 1 && cmd.entryStatus() != 2) throw new BizException("首次登记只能选择在职或待入职");
            if (cmd.entryTime() == null || cmd.type() == null || cmd.status() == null) {
                throw new BizException("登记任职前请补全入职日期、聘用形式及员工类型");
            }
            if (cmd.entryStatus() == 1 && cmd.entryTime().isAfter(LocalDateTime.now())) {
                throw new BizException("在职员工入职日期不能晚于当前时间");
            }
        }
        if (cmd.status() != null && cmd.type() != null
                && (cmd.status() <= 2 ? cmd.type() != 1 : cmd.type() != 2)) {
            throw new BizException("员工类型与聘用形式不一致");
        }
        if (cmd.jobNumber() != null && !cmd.jobNumber().isBlank()) {
            HrmEmployeeDO same = employees.selectByJobNumber(cmd.jobNumber());
            if (same != null && !same.getId().equals(cmd.id())) throw new BizException("工号已被其他员工使用");
        }
        HashSet<Long> visited = new HashSet<>();
        visited.add(cmd.id());
        Long cursor = cmd.leaderEmployeeId();
        while (cursor != null) {
            if (!visited.add(cursor)) throw new BizException("直属上级不能是本人或形成循环汇报关系");
            HrmEmployeeDO manager = employees.selectByIdForUpdate(cursor);
            if (manager == null || Integer.valueOf(4).equals(manager.getEntryStatus())) throw new BizException("直属上级不存在或已离职");
            cursor = manager.getLeaderEmployeeId();
        }
        employees.update(null, new LambdaUpdateWrapper<HrmEmployeeDO>().eq(HrmEmployeeDO::getId, cmd.id())
                .set(HrmEmployeeDO::getJobNumber, cmd.jobNumber()).set(HrmEmployeeDO::getLeaderEmployeeId, cmd.leaderEmployeeId())
                .set(HrmEmployeeDO::getEntryTime, cmd.entryTime()).set(HrmEmployeeDO::getBirthday, cmd.birthday())
                .set(HrmEmployeeDO::getEntryStatus, cmd.entryStatus()).set(HrmEmployeeDO::getType, cmd.type())
                .set(HrmEmployeeDO::getStatus, cmd.status()).set(HrmEmployeeDO::getPostLevel, cmd.postLevel())
                .set(HrmEmployeeDO::getWorkCity, cmd.workCity()).set(HrmEmployeeDO::getWorkAddress, cmd.workAddress())
                .set(HrmEmployeeDO::getRemark, cmd.remark()));
        LocalDateTime now = LocalDateTime.now();
        changes.insert(new HrmEmployeeChangeRecordDO().setEmployeeId(cmd.id()).setType(11)
                .setOldDeptId(old.getDeptId()).setNewDeptId(old.getDeptId())
                .setOldPostName(old.getPostName()).setNewPostName(old.getPostName())
                .setOldPostLevel(old.getPostLevel()).setNewPostLevel(cmd.postLevel())
                .setOldWorkAddress(old.getWorkAddress()).setNewWorkAddress(cmd.workAddress())
                .setOldLeaderEmployeeId(old.getLeaderEmployeeId()).setNewLeaderEmployeeId(cmd.leaderEmployeeId())
                .setEffectTime(now).setAppliedTime(now).setRemark(unconfirmed ? "HR 完善档案及首次任职登记" : "HR 维护本地档案补充信息"));
    }
}
