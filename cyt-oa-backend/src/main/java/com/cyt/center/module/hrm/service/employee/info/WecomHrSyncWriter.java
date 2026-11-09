package com.cyt.center.module.hrm.service.employee.info;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.*;
import com.cyt.center.module.hrm.dal.mysql.employee.info.*;
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

    public record Conflict(Long employeeId, String name, String field, String currentValue, String upstreamValue) { }
    public record Written(int updatedFields, List<Conflict> conflicts) { }

    /** 只读预检：升级遗漏时在读取上游和写员工档案之前明确失败。 */
    public void requireSchema() {
        if (states.countSyncTable() != 1)
            throw new BizException("企微花名册同步尚未完成数据库升级：请管理员执行 db/wecom-hr-sync-schema.sql，本次未写入");
    }

    @Transactional(rollbackFor = Exception.class)
    public Written apply(Long employeeId, Long userId, Map<String, LocalDate> dates,
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
        for (var field : dates.entrySet()) {
            String key = field.getKey();
            if (!Set.of("birthday", "entryTime").contains(key)) throw new BizException("不支持的花名册日期字段");
            if (selectedExpectedValues != null && !selectedExpectedValues.containsKey(key)) continue;
            LocalDateTime current = key.equals("birthday") ? employee.getBirthday() : employee.getEntryTime();
            LocalDateTime previous = key.equals("birthday") ? state.getBirthday() : state.getEntryTime();
            LocalDate upstream = field.getValue();
            if (selectedExpectedValues != null && !Objects.equals(date(current), selectedExpectedValues.get(key)))
                throw new BizException("本地档案在确认期间已变化，请重新同步");
            boolean writable = selectedExpectedValues != null || current == null
                    || current.toLocalDate().equals(upstream)
                    || previous != null && current.toLocalDate().equals(previous.toLocalDate());
            if (!writable) {
                conflicts.add(new Conflict(employeeId, employee.getName(), key, date(current), upstream.toString()));
                continue;
            }
            if (current == null || !current.toLocalDate().equals(upstream)) {
                if (key.equals("birthday")) change.set(HrmEmployeeDO::getBirthday, upstream.atStartOfDay());
                else change.set(HrmEmployeeDO::getEntryTime, upstream.atStartOfDay());
                updated++;
            }
            if (key.equals("birthday")) state.setBirthday(upstream.atStartOfDay());
            else state.setEntryTime(upstream.atStartOfDay());
        }
        if (selectedExpectedValues != null && !dates.keySet().containsAll(selectedExpectedValues.keySet()))
            throw new BizException("企微字段当前为空或不可读，未覆盖本地日期");
        if (updated > 0) employees.update(null, change);
        state.setSyncedAt(LocalDateTime.now());
        if (newState) states.insert(state); else states.updateById(state);
        return new Written(updated, conflicts);
    }
    private static String date(LocalDateTime value) { return value == null ? null : value.toLocalDate().toString(); }
}
