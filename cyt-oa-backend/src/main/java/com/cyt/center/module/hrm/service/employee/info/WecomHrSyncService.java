package com.cyt.center.module.hrm.service.employee.info;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.integration.*;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import com.cyt.center.module.system.mapper.SysUserMapper;
import com.cyt.center.module.system.model.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class WecomHrSyncService {
    private final WecomGatewayClient gateway;
    private final HrmEmployeeMapper employees;
    private final SysUserMapper users;
    private final WecomHrSyncWriter writer;
    private final AtomicBoolean running = new AtomicBoolean();
    public record Failure(Long employeeId, String name, String reason) { }
    public record Result(int scanned, int updatedFields, List<WecomHrSyncWriter.Conflict> conflicts,
                         List<Failure> failures, LocalDateTime checkedAt) { }
    private record Profile(HrmEmployeeDO employee, SysUser user, Map<String, LocalDate> dates, String error) { }

    public Result sync() {
        if (!running.compareAndSet(false, true)) throw new BizException("已有一次企微花名册同步正在执行");
        try {
            writer.requireSchema();
            var fields = WecomHrDates.fields(gateway.hrFields());
            List<HrmEmployeeDO> archives = employees.selectList(new LambdaQueryWrapper<HrmEmployeeDO>()
                    .isNotNull(HrmEmployeeDO::getUserId));
            if (archives.size() > 500) throw new BizException("单次花名册同步最多 500 人，请分批处理");
            Map<Long, SysUser> userMap = new HashMap<>();
            if (!archives.isEmpty()) users.selectBatchIds(archives.stream().map(HrmEmployeeDO::getUserId).toList())
                    .forEach(u -> userMap.put(u.getId(), u));
            List<Callable<Profile>> requests = new ArrayList<>();
            for (HrmEmployeeDO employee : archives) {
                SysUser user = userMap.get(employee.getUserId());
                if (user == null || user.getWecomUserid() == null || user.getWecomUserid().isBlank()) continue;
                requests.add(() -> fetch(employee, user, fields));
            }
            List<Profile> profiles = new ArrayList<>();
            // 有界并发 + 整体截止；在读取全部结束前不持有数据库事务。
            ExecutorService pool = Executors.newFixedThreadPool(5);
            try {
                List<Future<Profile>> fetched = pool.invokeAll(requests, 150, TimeUnit.SECONDS);
                if (fetched.stream().anyMatch(Future::isCancelled)) throw new BizException("花名册读取超时，本轮未写入");
                for (Future<Profile> future : fetched) profiles.add(future.get());
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt(); throw new BizException("花名册同步中断，本轮未写入");
            } catch (ExecutionException error) { throw new BizException("花名册读取异常，本轮未写入"); }
            finally { pool.shutdownNow(); }
            int updated = 0;
            List<Failure> failed = new ArrayList<>();
            List<WecomHrSyncWriter.Conflict> conflicts = new ArrayList<>();
            for (Profile profile : profiles) {
                if (profile.error() != null) {
                    failed.add(new Failure(profile.employee().getId(), profile.employee().getName(), profile.error()));
                    continue;
                }
                try {
                    var result = writer.apply(profile.employee().getId(), profile.user().getId(), profile.dates(), null);
                    updated += result.updatedFields(); conflicts.addAll(result.conflicts());
                } catch (RuntimeException error) {
                    failed.add(new Failure(profile.employee().getId(), profile.employee().getName(), "档案保存失败或关联已变化"));
                }
            }
            log.info("企微花名册同步结束 scanned={}, updatedFields={}, conflicts={}, failures={}", profiles.size(), updated, conflicts.size(), failed.size());
            return new Result(profiles.size(), updated, conflicts, failed, LocalDateTime.now());
        } finally { running.set(false); }
    }

    public int confirm(Long employeeId, String field, String expectedCurrentValue, String expectedUpstreamValue) {
        if (!Set.of("birthday", "entryTime").contains(field)) throw new BizException("不支持的花名册字段");
        writer.requireSchema();
        HrmEmployeeDO employee = employees.selectById(employeeId);
        SysUser user = employee == null || employee.getUserId() == null ? null : users.selectById(employee.getUserId());
        if (user == null || user.getWecomUserid() == null) throw new BizException("员工未关联企微账号");
        var fields = WecomHrDates.fields(gateway.hrFields());
        var dates = WecomHrDates.parse(gateway.hrStaff(user.getWecomUserid(), fields.values()), fields, today());
        if (!Objects.equals(expectedUpstreamValue, dates.get(field) == null ? null : dates.get(field).toString()))
            throw new BizException("企微日期在确认期间已变化，请重新同步");
        Map<String, String> expected = new HashMap<>(); expected.put(field, expectedCurrentValue);
        return writer.apply(employeeId, user.getId(), dates, expected).updatedFields();
    }
    private Profile fetch(HrmEmployeeDO employee, SysUser user, Map<String, WecomHrDates.Field> fields) {
        try {
            return new Profile(employee, user, WecomHrDates.parse(gateway.hrStaff(user.getWecomUserid(), fields.values()), fields, today()), null);
        } catch (RuntimeException error) { return new Profile(employee, user, Map.of(), "企微资料不可读或日期校验失败，请检查应用权限与员工资料"); }
    }
    private static LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }
}
