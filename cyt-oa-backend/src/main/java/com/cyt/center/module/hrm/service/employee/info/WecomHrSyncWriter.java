package com.cyt.center.module.hrm.service.employee.info;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.integration.WecomHrDates;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.*;
import com.cyt.center.module.hrm.dal.mysql.employee.info.*;
import com.cyt.center.module.hrm.dal.dataobject.employee.employment.HrmEmployeeChangeRecordDO;
import com.cyt.center.module.hrm.dal.mysql.employee.employment.HrmEmployeeChangeRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WecomHrSyncWriter {
    private final HrmEmployeeMapper employees;
    private final HrmEmployeeWecomSyncMapper states;
    private final HrmEmployeeChangeRecordMapper changes;

    public record Conflict(Long employeeId, String name, String field, String currentValue, String upstreamValue, String reason) {
        public Conflict(Long employeeId, String name, String field, String currentValue, String upstreamValue) {
            this(employeeId, name, field, currentValue, upstreamValue, "本地和企微字段存在差异");
        }
    }
    public record Written(int updatedFields, List<Conflict> conflicts) { }

    /** 只读预检：升级遗漏时在读取上游和写员工档案之前明确失败。 */
    public void requireSchema() {
        if (states.countSyncTable() != 1)
            throw new BizException("企微花名册同步尚未完成数据库升级：请管理员执行 db/wecom-hr-sync-schema.sql，本次未写入");
    }

    @Transactional(rollbackFor = Exception.class)
    public Written apply(Long employeeId, Long userId, Map<String, LocalDate> dates,
                         Map<String, String> selectedExpectedValues) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.putAll(dates);
        return write(employeeId, userId, values, selectedExpectedValues);
    }

    @Transactional(rollbackFor = Exception.class)
    public Written applyProfile(Long employeeId, Long userId, WecomHrDates.Profile profile) {
        return write(employeeId, userId, profile.values(), null);
    }

    private Written write(Long employeeId, Long userId, Map<String, Object> values,
                          Map<String, String> selectedExpectedValues) {
        HrmEmployeeDO employee = employees.selectByIdForUpdate(employeeId);
        if (employee == null || !Objects.equals(employee.getUserId(), userId))
            throw new BizException("员工档案关联已变化，请重新同步");
        HrmEmployeeWecomSyncDO state = states.selectById(employeeId);
        boolean newState = state == null;
        if (newState) { state = new HrmEmployeeWecomSyncDO(); state.setEmployeeId(employeeId); }
        var change = new LambdaUpdateWrapper<HrmEmployeeDO>().eq(HrmEmployeeDO::getId, employeeId);
        List<Conflict> conflicts = new ArrayList<>();
        int updated = 0;
        List<String> changedFields = new ArrayList<>();
        for (var field : values.entrySet()) {
            String key = field.getKey();
            validate(key, field.getValue());
            if (selectedExpectedValues != null && !selectedExpectedValues.containsKey(key)) continue;
            Object current = current(employee, key);
            Object upstream = field.getValue();
            if (selectedExpectedValues != null && !Objects.equals(current == null ? null : current.toString(), selectedExpectedValues.get(key)))
                throw new BizException("本地档案在确认期间已变化，请重新同步");
            // 只接收成功可读的补丁；缺失和无权限字段不会进入 values。
            if (!Objects.equals(current, upstream)) {
                set(change, key, upstream);
                changedFields.add(label(key));
                updated++;
            }
            if (key.equals("birthday")) state.setBirthday(time(upstream));
            else if (key.equals("entryTime")) state.setEntryTime(time(upstream));
        }
        if (selectedExpectedValues != null && !values.keySet().containsAll(selectedExpectedValues.keySet()))
            throw new BizException("企微字段当前为空或不可读，未覆盖本地日期");
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        if (updated > 0) {
            if (employees.update(null, change) != 1) throw new BizException("员工档案保存失败，请重新同步");
            changes.insert(new HrmEmployeeChangeRecordDO().setEmployeeId(employeeId).setType(11)
                    .setOldDeptId(employee.getDeptId()).setNewDeptId(employee.getDeptId())
                    .setOldPostName(employee.getPostName()).setNewPostName(employee.getPostName())
                    .setOldPostLevel(employee.getPostLevel()).setNewPostLevel(employee.getPostLevel())
                    .setOldWorkAddress(employee.getWorkAddress()).setNewWorkAddress(employee.getWorkAddress())
                    .setOldLeaderEmployeeId(employee.getLeaderEmployeeId()).setNewLeaderEmployeeId(employee.getLeaderEmployeeId())
                    .setEffectTime(now).setAppliedTime(now)
                    .setRemark("企微人事同步更新字段：" + String.join("、", changedFields)));
        }
        state.setSyncedAt(now);
        if (newState) states.insert(state);
        else states.update(null, new LambdaUpdateWrapper<HrmEmployeeWecomSyncDO>()
                .eq(HrmEmployeeWecomSyncDO::getEmployeeId, employeeId)
                .set(HrmEmployeeWecomSyncDO::getBirthday, state.getBirthday())
                .set(HrmEmployeeWecomSyncDO::getEntryTime, state.getEntryTime())
                .set(HrmEmployeeWecomSyncDO::getSyncedAt, state.getSyncedAt()));
        return new Written(updated, conflicts);
    }

    private static Object current(HrmEmployeeDO employee, String key) {
        return switch (key) {
            case "birthday" -> date(employee.getBirthday()); case "entryTime" -> date(employee.getEntryTime());
            case "regularTime" -> date(employee.getRegularTime()); case "leaveTime" -> date(employee.getLeaveTime());
            case "entryStatus" -> employee.getEntryStatus(); case "status" -> employee.getStatus();
            case "type" -> employee.getType();
            case "probation" -> employee.getProbation();
            default -> throw new BizException("不支持的花名册字段");
        };
    }
    private static void validate(String key, Object value) {
        if (Set.of("birthday", "entryTime", "regularTime", "leaveTime").contains(key)) {
            if (value != null && !(value instanceof LocalDate)) throw new BizException("花名册日期字段类型异常");
        } else {
            if (!(value instanceof Integer number)) throw new BizException("花名册状态字段不能清空或使用非整数");
            boolean valid = switch (key) {
                case "entryStatus" -> number >= 0 && number <= 5;
                case "status" -> number >= 1 && number <= 8;
                case "type" -> number >= 1 && number <= 2;
                case "probation" -> number >= 0 && number <= 6;
                default -> false;
            };
            if (!valid) throw new BizException("不支持的花名册状态或试用期");
        }
    }
    private static void set(LambdaUpdateWrapper<HrmEmployeeDO> change, String key, Object value) {
        switch (key) {
            case "birthday" -> change.set(HrmEmployeeDO::getBirthday, time(value));
            case "entryTime" -> change.set(HrmEmployeeDO::getEntryTime, time(value));
            case "regularTime" -> change.set(HrmEmployeeDO::getRegularTime, time(value));
            case "leaveTime" -> change.set(HrmEmployeeDO::getLeaveTime, time(value));
            case "entryStatus" -> change.set(HrmEmployeeDO::getEntryStatus, value);
            case "status" -> change.set(HrmEmployeeDO::getStatus, value);
            case "type" -> change.set(HrmEmployeeDO::getType, value);
            case "probation" -> change.set(HrmEmployeeDO::getProbation, value);
            default -> throw new BizException("不支持的花名册字段");
        }
    }
    private static String label(String key) {
        return switch (key) {
            case "birthday" -> "生日"; case "entryTime" -> "入职日期"; case "regularTime" -> "转正日期";
            case "leaveTime" -> "离职日期"; case "entryStatus" -> "入职状态"; case "status" -> "员工状态";
            case "type" -> "员工类型";
            case "probation" -> "试用期"; default -> key;
        };
    }
    private static LocalDate date(LocalDateTime value) { return value == null ? null : value.toLocalDate(); }
    private static LocalDateTime time(Object value) { return value == null ? null : ((LocalDate) value).atStartOfDay(); }
}
