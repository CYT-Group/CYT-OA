package com.cyt.center.module.hrm.service.employee.info;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.integration.WecomGatewayClient;
import com.cyt.center.core.integration.WecomGatewayException;
import com.cyt.center.core.integration.WecomHrDates;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class WecomHrSyncService {
    private final WecomGatewayClient gateway;
    private final HrmEmployeeMapper employees;
    private final SysUserMapper users;
    private final WecomHrSyncWriter writer;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicLong nextStaffRequestNanos = new AtomicLong(Long.MIN_VALUE);

    public record Failure(Long employeeId, String name, String reason, String code, String traceId) {
        public Failure(Long employeeId, String name, String reason) { this(employeeId, name, reason, "", ""); }
    }
    public record Skipped(Long employeeId, String name, String reason, String code, String traceId) {
        public Skipped(Long employeeId, String name, String reason) { this(employeeId, name, reason, "", ""); }
    }
    /** scanned 是检查档案数，requested 是实际调用员工花名册读取的次数。 */
    public record Result(int scanned, int requested, int succeeded, int failed, int skipped, int unchanged,
                         int updatedEmployees, int updatedFields, List<Failure> failures,
                         List<Skipped> skippedEmployees, List<WecomHrSyncWriter.Conflict> conflicts,
                         LocalDateTime checkedAt) {
        public Result {
            failures = List.copyOf(failures); skippedEmployees = List.copyOf(skippedEmployees);
            conflicts = List.copyOf(conflicts);
        }
    }
    private record Target(HrmEmployeeDO employee, SysUser user, boolean present) { }
    private record Profile(Target target, WecomHrDates.Profile profile, String error, String code, String traceId) { }

    /** 保留旧入口；联合同步应传入本轮成功获取的通讯录成员集合，以区别历史档案和可见性异常。 */
    public Result sync() { return sync(null); }

    public Result sync(Set<String> presentUserids) {
        if (!running.compareAndSet(false, true)) throw new BizException("已有一次企微花名册同步正在执行");
        try {
            writer.requireSchema();
            List<HrmEmployeeDO> archives = employees.selectList(new LambdaQueryWrapper<>());
            List<Long> linkedIds = archives.stream().map(HrmEmployeeDO::getUserId).filter(Objects::nonNull).distinct().toList();
            Map<Long, SysUser> userMap = new HashMap<>();
            if (!linkedIds.isEmpty()) users.selectBatchIds(linkedIds).forEach(user -> userMap.put(user.getId(), user));
            Set<String> present = new HashSet<>();
            if (presentUserids != null) presentUserids.stream().filter(Objects::nonNull).map(WecomHrSyncService::userid).forEach(present::add);
            List<Target> targets = new ArrayList<>();
            List<Skipped> skipped = new ArrayList<>();
            for (HrmEmployeeDO employee : archives) {
                SysUser user = userMap.get(employee.getUserId());
                if (user == null || user.getWecomUserid() == null || user.getWecomUserid().isBlank()) {
                    skipped.add(new Skipped(employee.getId(), employee.getName(), "档案未关联可用的企微账号，保留本地资料"));
                    continue;
                }
                boolean currentlyPresent = presentUserids != null && present.contains(userid(user.getWecomUserid()));
                // 未经真实通讯录确认的演示/手工 userid 不能凭恰巧可读的同名账号建立身份关联。
                if (!currentlyPresent && user.getDirectorySyncedAt() == null) {
                    skipped.add(new Skipped(employee.getId(), employee.getName(), "企微身份尚未经通讯录确认，保留本地档案待核实"));
                    continue;
                }
                // 已确认的历史人员可能仍可从人事助手读取，保留其真实离职信息读取能力。
                targets.add(new Target(employee, user, currentlyPresent));
            }
            if (targets.isEmpty()) return result(archives.size(), 0, 0, 0, 0, List.of(), skipped, List.of());
            if (targets.size() > 500) throw new BizException("单次花名册同步最多读取500个关联账号，请分批处理；本轮未写入");
            WecomHrDates.Configuration configuration = WecomHrDates.discover(gateway.hrFields());
            if (configuration.fields().isEmpty()) throw new BizException("企微花名册未返回可同步的人事字段，请检查人事助手配置和应用权限");
            AtomicInteger requested = new AtomicInteger();
            List<Callable<Profile>> requests = targets.stream().<Callable<Profile>>map(target -> () -> {
                try { awaitStaffQuota(); }
                catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    return failure(target, "花名册读取已中断，保留本地资料", "INTERRUPTED", "");
                }
                requested.incrementAndGet();
                return fetch(target, configuration.fields());
            }).toList();
            List<Profile> profiles = new ArrayList<>();
            ExecutorService pool = Executors.newFixedThreadPool(5);
            try {
                List<Future<Profile>> fetched = pool.invokeAll(requests, 150, TimeUnit.SECONDS);
                for (int i = 0; i < fetched.size(); i++) {
                    Future<Profile> future = fetched.get(i);
                    if (future.isCancelled()) profiles.add(failure(targets.get(i), "花名册读取超时，保留本地资料", "TIMEOUT", ""));
                    else try { profiles.add(future.get()); }
                    catch (ExecutionException error) { profiles.add(failure(targets.get(i), "花名册读取未完成，保留本地资料", "", "")); }
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new BizException("花名册同步中断，本轮未写入");
            } finally { pool.shutdownNow(); }

            int succeeded = 0, unchanged = 0, updatedEmployees = 0, updatedFields = 0;
            List<Failure> failures = new ArrayList<>();
            List<WecomHrSyncWriter.Conflict> conflicts = new ArrayList<>();
            for (Profile profile : profiles) {
                HrmEmployeeDO employee = profile.target().employee();
                if (profile.error() != null) {
                    if (!profile.target().present() && profile.code().equals("WECOM_60111"))
                        skipped.add(new Skipped(employee.getId(), employee.getName(), "不在本轮通讯录且企微账号不存在或不可见，保留历史档案待核实", profile.code(), profile.traceId()));
                    else failures.add(new Failure(employee.getId(), employee.getName(), profile.error(), profile.code(), profile.traceId()));
                    continue;
                }
                List<WecomHrDates.Issue> issues = new ArrayList<>(configuration.issues());
                issues.addAll(profile.profile().issues());
                issues.forEach(issue -> conflicts.add(new WecomHrSyncWriter.Conflict(employee.getId(), employee.getName(), issue.field(), null, null, issue.reason())));
                if (profile.profile().readableFields() == 0) {
                    failures.add(new Failure(employee.getId(), employee.getName(), "未成功读取任何可同步的人事字段，保留本地资料"));
                    continue;
                }
                try {
                    WecomHrSyncWriter.Written written = writer.applyProfile(employee.getId(), profile.target().user().getId(), profile.profile());
                    succeeded++; updatedFields += written.updatedFields(); conflicts.addAll(written.conflicts());
                    if (written.updatedFields() > 0) updatedEmployees++; else unchanged++;
                } catch (RuntimeException error) {
                    failures.add(new Failure(employee.getId(), employee.getName(), "档案保存失败或关联已变化，保留本地资料"));
                }
            }
            log.info("企微花名册同步结束 scanned={}, requested={}, succeeded={}, failed={}, skipped={}, updatedFields={}, conflicts={}",
                    archives.size(), requested.get(), succeeded, failures.size(), skipped.size(), updatedFields, conflicts.size());
            return result(archives.size(), requested.get(), succeeded, unchanged, updatedEmployees, failures, skipped, conflicts, updatedFields);
        } finally { running.set(false); }
    }

    public int confirm(Long employeeId, String field, String expectedCurrentValue, String expectedUpstreamValue) {
        if (!Set.of("birthday", "entryTime").contains(field)) throw new BizException("不支持的花名册字段");
        writer.requireSchema();
        HrmEmployeeDO employee = employees.selectById(employeeId);
        SysUser user = employee == null || employee.getUserId() == null ? null : users.selectById(employee.getUserId());
        if (user == null || user.getWecomUserid() == null || user.getWecomUserid().isBlank()) throw new BizException("员工未关联企微账号");
        if (user.getDirectorySyncedAt() == null) throw new BizException("企微身份尚未确认，请先同步通讯录");
        WecomHrDates.Field selected = WecomHrDates.fields(gateway.hrFields()).get(field);
        if (selected == null) throw new BizException("企微未提供所选日期字段，保留本地资料");
        var parsed = WecomHrDates.parseProfile(gateway.hrStaff(user.getWecomUserid(), List.of(selected)), Map.of(field, selected), today());
        if (!parsed.values().containsKey(field)) throw new BizException("企微日期字段不可读，保留本地资料");
        Object upstream = parsed.values().get(field);
        if (!Objects.equals(expectedUpstreamValue, upstream == null ? null : upstream.toString()))
            throw new BizException("企微日期在确认期间已变化，请重新同步");
        Map<String, LocalDate> dates = new LinkedHashMap<>(); dates.put(field, (LocalDate) upstream);
        Map<String, String> expected = new HashMap<>(); expected.put(field, expectedCurrentValue);
        return writer.apply(employeeId, user.getId(), dates, expected).updatedFields();
    }

    private Profile fetch(Target target, Map<String, WecomHrDates.Field> fields) {
        try {
            return new Profile(target, WecomHrDates.parseProfile(gateway.hrStaff(target.user().getWecomUserid().trim(), fields.values()), fields, today()), null, "", "");
        } catch (WecomGatewayException error) {
            String reason = error.code().equals("WECOM_60111")
                    ? "企微UserID不存在或不在花名册应用可见范围，保留本地资料"
                    : "企微花名册读取失败，请检查应用授权、可见范围与上游服务；本地资料已保留";
            return failure(target, reason, error.code(), error.traceId());
        } catch (RuntimeException error) {
            return failure(target, "企微花名册资料未成功读取或格式异常，保留本地资料", "", "");
        }
    }
    private static Profile failure(Target target, String reason, String code, String traceId) { return new Profile(target, null, reason, code, traceId); }
    /** 300次/分钟配额下保留余量；配额窗口跨本Service的连续同步共享。 */
    private void awaitStaffQuota() throws InterruptedException {
        long reserved;
        while (true) {
            long previous = nextStaffRequestNanos.get();
            reserved = Math.max(System.nanoTime(), previous);
            if (nextStaffRequestNanos.compareAndSet(previous, reserved + TimeUnit.MILLISECONDS.toNanos(250))) break;
        }
        long remaining = reserved - System.nanoTime();
        if (remaining > 0) TimeUnit.NANOSECONDS.sleep(remaining);
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
    }
    private static Result result(int scanned, int requested, int succeeded, int unchanged, int updatedEmployees,
                                 List<Failure> failures, List<Skipped> skipped, List<WecomHrSyncWriter.Conflict> conflicts) {
        return result(scanned, requested, succeeded, unchanged, updatedEmployees, failures, skipped, conflicts, 0);
    }
    private static Result result(int scanned, int requested, int succeeded, int unchanged, int updatedEmployees,
                                 List<Failure> failures, List<Skipped> skipped, List<WecomHrSyncWriter.Conflict> conflicts, int updatedFields) {
        return new Result(scanned, requested, succeeded, failures.size(), skipped.size(), unchanged, updatedEmployees, updatedFields,
                failures, skipped, conflicts, LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
    }
    private static String userid(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    private static LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }
}
