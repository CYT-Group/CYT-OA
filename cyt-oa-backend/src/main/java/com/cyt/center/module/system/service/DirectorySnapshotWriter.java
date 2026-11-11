package com.cyt.center.module.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.system.mapper.SysDeptMapper;
import com.cyt.center.module.system.mapper.SysUserDeptMapper;
import com.cyt.center.module.system.mapper.SysUserMapper;
import com.cyt.center.module.system.model.entity.SysDept;
import com.cyt.center.module.system.model.entity.SysUser;
import com.cyt.center.module.system.model.entity.SysUserDept;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 通讯录快照落库：任何一条数据不合格就整体回滚，绝不留下半套组织。
 * 账号档案只由这里写入，本地能改的只有角色绑定。
 */
@Service
@RequiredArgsConstructor
public class DirectorySnapshotWriter {

    private static final int WECOM_ACTIVE = 1;

    private final SysUserMapper users;
    private final SysDeptMapper departments;
    private final SysUserDeptMapper userDepartments;
    private final SysUserService accountService;
    private final CenterProperties properties;
    private final EmployeeDirectoryArchiveService employeeArchives;

    @Transactional(rollbackFor = Exception.class)
    public EnterpriseDirectorySyncService.SyncResult apply(JsonNode snapshot) {
        JsonNode employeeRows = snapshot.path("users");
        JsonNode departmentRows = snapshot.path("departments");
        if (!employeeRows.isArray() || !departmentRows.isArray() || departmentRows.isEmpty()) {
            throw new BizException("企微通讯录结构不完整，未执行同步");
        }
        JsonNode counts = snapshot.path("counts");
        if (counts.path("users").asInt(-1) != employeeRows.size()
                || counts.path("departments").asInt(-1) != departmentRows.size()) {
            throw new BizException("企微通讯录数量校验失败，未执行同步");
        }

        Map<Long, SysDept> departmentMap = readDepartments(departmentRows);
        List<SysUser> existing = users.selectList(null);
        Map<String, SysUser> byExternalId = new HashMap<>();
        Set<String> usernames = new HashSet<>();
        for (SysUser user : existing) {
            if (user.getWecomUserid() != null) {
                byExternalId.put(normalizeUserid(user.getWecomUserid()), user);
            }
            if (user.getUsername() != null) {
                usernames.add(normalizeUserid(user.getUsername()));
            }
        }

        Set<String> seen = new HashSet<>();
        List<String> conflicts = new ArrayList<>();
        List<Long> blocked = new ArrayList<>();
        List<SysUser> identities = new ArrayList<>();
        Map<Long, Set<Long>> memberships = new HashMap<>();
        for (SysUserDept binding : userDepartments.selectList(null)) {
            memberships.computeIfAbsent(binding.getUserId(), key -> new HashSet<>()).add(binding.getDeptId());
        }
        List<Long> replaceIds = new ArrayList<>();
        List<SysUserDept> newBindings = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int created = 0;
        int updated = 0;

        for (JsonNode row : employeeRows) {
            String externalId = text(row, "userid");
            String name = text(row, "name");
            Integer status = row.path("status").isNumber() ? row.path("status").asInt() : null;
            JsonNode departmentIds = row.path("department_ids");
            if (externalId == null || externalId.isBlank() || externalId.length() > 128
                    || !externalId.equals(externalId.trim()) || !seen.add(normalizeUserid(externalId))
                    || name == null || name.isBlank() || status == null
                    || !Set.of(1, 2, 4, 5).contains(status) || !departmentIds.isArray()) {
                throw new BizException("企微成员数据无效或重复，整个同步已取消");
            }
            Set<Long> deptIds = new LinkedHashSet<>();
            for (JsonNode id : departmentIds) {
                Long deptId = id.isNumber() ? id.asLong() : null;
                if (deptId == null || !departmentMap.containsKey(deptId)) {
                    throw new BizException("成员引用了未知部门，整个同步已取消");
                }
                deptIds.add(deptId);
            }
            Long mainDeptId = row.path("main_department").isNumber() ? row.path("main_department").asLong() : null;
            if (mainDeptId == null && !deptIds.isEmpty()) {
                mainDeptId = deptIds.iterator().next();
            }
            if (mainDeptId != null && !deptIds.contains(mainDeptId)) {
                throw new BizException("成员主部门不在归属部门中，整个同步已取消");
            }

            SysUser current = byExternalId.get(normalizeUserid(externalId));
            if (current == null && usernames.contains(normalizeUserid(externalId))) {
                // 没有企微身份的本地账号（尤其是管理员）绝不能靠用户名自动认领企微身份。
                conflicts.add(externalId);
                continue;
            }

            SysUser change = new SysUser();
            change.setWecomUserid(externalId);
            change.setName(name);
            change.setPosition(nullToEmpty(text(row, "position")));
            change.setMobile(nullToEmpty(text(row, "mobile")));
            change.setEmail(nullToEmpty(text(row, "email")));
            change.setAvatar(nullToEmpty(text(row, "avatar")));
            change.setMainDeptId(mainDeptId);
            change.setWecomStatus(status);
            // 与 Portal 一致：同步只能收紧访问，不能恢复管理员已经停用的账号。
            change.setEnabled(status == WECOM_ACTIVE && (current == null || Boolean.TRUE.equals(current.getEnabled())));
            change.setDirectorySyncedAt(now);
            if (current == null) {
                change.setUsername(externalId);
                users.insert(change);
                usernames.add(normalizeUserid(externalId));
                created++;
            } else {
                change.setId(current.getId());
                // 显式写 null，企微取消主部门时不能被 MyBatis 的非空策略保留旧值。
                users.update(null, new LambdaUpdateWrapper<SysUser>().eq(SysUser::getId, change.getId())
                        .set(SysUser::getWecomUserid, externalId).set(SysUser::getName, name)
                        .set(SysUser::getPosition, change.getPosition()).set(SysUser::getMobile, change.getMobile())
                        .set(SysUser::getEmail, change.getEmail()).set(SysUser::getAvatar, change.getAvatar())
                        .set(SysUser::getMainDeptId, mainDeptId).set(SysUser::getWecomStatus, status)
                        .set(SysUser::getEnabled, change.getEnabled()).set(SysUser::getDirectorySyncedAt, now));
                updated++;
            }
            if (!deptIds.equals(memberships.getOrDefault(change.getId(), Set.of()))) {
                replaceIds.add(change.getId());
                for (Long deptId : deptIds) {
                    SysUserDept binding = new SysUserDept();
                    binding.setUserId(change.getId());
                    binding.setDeptId(deptId);
                    newBindings.add(binding);
                }
            }
            identities.add(change);
            if (!Boolean.TRUE.equals(change.getEnabled())) {
                blocked.add(change.getId());
            }
            if (properties.getAuth().adminUseridList().contains(externalId)) {
                accountService.grantRole(change, PrincipalService.ROLE_ADMIN);
            }
        }

        // 企微完整快照为唯一成员来源；未关联企微的本地账号不在删除范围。
        List<SysUser> missingUsers = existing.stream().filter(user -> !blank(user.getWecomUserid())
                && !seen.contains(normalizeUserid(user.getWecomUserid()))).toList();
        missingUsers.stream()
                .map(SysUser::getId).filter(memberships::containsKey).forEach(replaceIds::add);
        for (int offset = 0; offset < replaceIds.size(); offset += 500) {
            userDepartments.delete(new LambdaQueryWrapper<SysUserDept>().in(SysUserDept::getUserId,
                    replaceIds.subList(offset, Math.min(offset + 500, replaceIds.size()))));
        }
        for (int offset = 0; offset < newBindings.size(); offset += 500) {
            userDepartments.insertBindings(newBindings.subList(offset, Math.min(offset + 500, newBindings.size())));
        }
        employeeArchives.syncIdentities(identities);

        // 全表部门在这里已经读了一次，顺便记住已存在的 ID，下面不必再为每个部门单点查一次。
        Set<Long> existingDeptIds = new HashSet<>();
        for (SysDept old : departments.selectList(null)) {
            existingDeptIds.add(old.getId());
            if (!departmentMap.containsKey(old.getId())) {
                SysDept deactivate = new SysDept();
                deactivate.setId(old.getId());
                deactivate.setActive(false);
                departments.updateById(deactivate);
            }
        }
        for (SysDept department : departmentMap.values()) {
            if (!existingDeptIds.contains(department.getId())) {
                departments.insert(department);
            } else {
                // updateById 跳过 null 字段，手工维护的部门负责人不会被同步覆盖。
                departments.updateById(department);
            }
        }

        for (int offset = 0; offset < missingUsers.size(); offset += 500) {
            List<SysUser> batch = missingUsers.subList(offset, Math.min(offset + 500, missingUsers.size()));
            List<Long> ids = batch.stream().map(SysUser::getId).toList();
            employeeArchives.deleteByUserIds(ids);
            accountService.deleteDirectoryUsers(ids);
            departments.update(null, new LambdaUpdateWrapper<SysDept>()
                    .in(SysDept::getLeaderUserid, batch.stream().map(SysUser::getWecomUserid).toList())
                    .set(SysDept::getLeaderUserid, null));
            blocked.addAll(ids);
        }
        List<String> missing = missingUsers.stream().map(SysUser::getWecomUserid).toList();
        afterCommit(blocked);
        return new EnterpriseDirectorySyncService.SyncResult(created, updated, departmentMap.size(), conflicts, missing);
    }

    private Map<Long, SysDept> readDepartments(JsonNode departmentRows) {
        Map<Long, SysDept> departmentMap = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (JsonNode row : departmentRows) {
            SysDept department = new SysDept();
            department.setId(row.path("id").isNumber() ? row.path("id").asLong() : null);
            department.setParentId(row.path("parent_id").isNumber() ? row.path("parent_id").asLong() : null);
            department.setName(text(row, "name"));
            department.setPath(text(row, "path"));
            department.setSortOrder(row.path("order").isNumber() ? row.path("order").asLong() : 0L);
            department.setActive(true);
            department.setSyncedAt(now);
            // path 只是网关原样带过来的数据，展示名另有来源，不该因为缺它就整趟同步作废
            if (department.getId() == null || department.getId() <= 0 || department.getParentId() == null
                    || blank(department.getName())
                    || departmentMap.put(department.getId(), department) != null) {
                throw new BizException("企微部门数据无效，未执行同步");
            }
        }
        for (SysDept dept : departmentMap.values()) {
            Set<Long> ancestors = new HashSet<>();
            SysDept cursor = dept;
            while (cursor != null && cursor.getParentId() != 0) {
                if (!ancestors.add(cursor.getId()) || !departmentMap.containsKey(cursor.getParentId())) {
                    throw new BizException("企微部门存在循环或缺失父部门，未执行同步");
                }
                cursor = departmentMap.get(cursor.getParentId());
            }
        }
        return departmentMap;
    }

    /** 停用人员要掉线，但必须等数据提交后再踢，否则回滚了却把人赶出去。 */
    private void afterCommit(List<Long> blocked) {
        if (blocked.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                blocked.forEach(StpUtil::logout);
            }
        });
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isValueNode() && !value.isNull() ? value.asText() : null;
    }

    /**
     * uk_sys_user_wecom / uk_sys_user_username 都是 utf8mb4_0900_ai_ci，MySQL 认为 DengXue 就是 dengxue，
     * Java 的 Map 不这么认为。两边口径不统一时，改过大小的同一个人会被判成新人再插一遍，
     * 撞唯一键后整个同步回滚。
     */
    private static String normalizeUserid(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

}
