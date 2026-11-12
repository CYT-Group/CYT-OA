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
        if (!counts.path("users").isIntegralNumber() || !counts.path("users").canConvertToLong()
                || !counts.path("departments").isIntegralNumber() || !counts.path("departments").canConvertToLong()
                || counts.path("users").asLong(-1) != employeeRows.size()
                || counts.path("departments").asLong(-1) != departmentRows.size()) {
            throw new BizException("企微通讯录数量校验失败，未执行同步");
        }

        Map<Long, SysDept> departmentMap = readDepartments(departmentRows);
        // 在第一笔写入前检查全部成员，尾部坏行也不能产生部分账号变更。
        List<Member> members = readMembers(employeeRows, departmentMap);
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

        for (Member member : members) {
            JsonNode row = member.source();
            String externalId = member.userid();
            String name = member.name();
            int status = member.status();
            Set<Long> deptIds = member.departmentIds();
            Long mainDeptId = member.mainDepartment();
            seen.add(normalizeUserid(externalId));

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

        // 一次未返回可能只是应用可见范围变化：仅报告，不删除或改变历史账号与人事状态。
        List<SysUser> missingUsers = existing.stream().filter(user -> !blank(user.getWecomUserid())
                && !seen.contains(normalizeUserid(user.getWecomUserid()))).toList();
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
        }
        for (SysDept department : departmentMap.values()) {
            if (!existingDeptIds.contains(department.getId())) {
                departments.insert(department);
            } else {
                // updateById 跳过 null 字段，手工维护的部门负责人不会被同步覆盖。
                departments.updateById(department);
            }
        }

        List<String> missing = missingUsers.stream().map(SysUser::getWecomUserid).toList();
        afterCommit(blocked);
        return new EnterpriseDirectorySyncService.SyncResult(created, updated, departmentMap.size(), conflicts, missing);
    }

    private record Member(JsonNode source, String userid, String name, int status,
                          Set<Long> departmentIds, Long mainDepartment) { }

    private List<Member> readMembers(JsonNode rows, Map<Long, SysDept> departmentMap) {
        var members = new ArrayList<Member>();
        var seen = new HashSet<String>();
        for (JsonNode row : rows) {
            String userid = text(row, "userid"), name = text(row, "name");
            JsonNode status = row.path("status"), ids = row.path("department_ids");
            if (!row.path("userid").isTextual() || userid == null || userid.isBlank()
                    || userid.length() > 128 || !userid.equals(userid.trim())
                    || !seen.add(normalizeUserid(userid)) || !row.path("name").isTextual()
                    || name == null || name.isBlank() || !status.isIntegralNumber() || !status.canConvertToInt()
                    || !Set.of(1L, 2L, 4L, 5L).contains(status.asLong(-1)) || !ids.isArray()) {
                throw new BizException("企微成员数据无效或重复，整个同步已取消");
            }
            var memberships = new LinkedHashSet<Long>();
            for (JsonNode id : ids) {
                if (!id.isIntegralNumber() || !id.canConvertToLong() || !departmentMap.containsKey(id.asLong()))
                    throw new BizException("成员引用了未知部门，整个同步已取消");
                memberships.add(id.asLong());
            }
            JsonNode main = row.path("main_department");
            if (!main.isMissingNode() && !main.isNull() && (!main.isIntegralNumber() || !main.canConvertToLong()))
                throw new BizException("成员主部门格式无效，整个同步已取消");
            Long mainId = main.isIntegralNumber() ? Long.valueOf(main.asLong()) : memberships.stream().findFirst().orElse(null);
            if (mainId != null && !memberships.contains(mainId))
                throw new BizException("成员主部门不在归属部门中，整个同步已取消");
            members.add(new Member(row, userid, name, status.asInt(), memberships, mainId));
        }
        return members;
    }

    private Map<Long, SysDept> readDepartments(JsonNode departmentRows) {
        Map<Long, SysDept> departmentMap = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (JsonNode row : departmentRows) {
            SysDept department = new SysDept();
            department.setId(row.path("id").isIntegralNumber() && row.path("id").canConvertToLong() ? row.path("id").asLong() : null);
            department.setParentId(row.path("parent_id").isIntegralNumber() && row.path("parent_id").canConvertToLong() ? row.path("parent_id").asLong() : null);
            department.setName(text(row, "name"));
            department.setPath(text(row, "path"));
            department.setSortOrder(row.path("order").isNumber() ? row.path("order").asLong() : 0L);
            department.setActive(true);
            department.setSyncedAt(now);
            // path 只是网关原样带过来的数据，展示名另有来源，不该因为缺它就整趟同步作废
            if (department.getId() == null || department.getId() <= 0 || department.getParentId() == null
                    || department.getParentId() < 0 || !row.path("name").isTextual() || blank(department.getName())
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
     * 与 sys_user 上 LOWER(username) / LOWER(wecom_userid) 唯一索引保持一致。
     * 同一人的 userid 仅改变大小写时应更新现有账号，避免创建重复身份或触发唯一键冲突。
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
