package com.cyt.center.module.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.security.PasswordHasher;
import com.cyt.center.module.system.mapper.SysUserMapper;
import com.cyt.center.module.system.mapper.SysUserRoleMapper;
import com.cyt.center.module.system.model.entity.SysRole;
import com.cyt.center.module.system.model.entity.SysUser;
import com.cyt.center.module.system.model.entity.SysUserRole;
import com.cyt.center.module.system.model.query.SysUserQuery;
import com.cyt.center.module.system.model.vo.OptionVO;
import com.cyt.center.module.system.model.vo.SysUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 账号读写。账号本体由通讯录同步产生，这里只负责登录校验与角色绑定，
 * 任何「按用户名自动认领企微身份」的逻辑都不允许出现。
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper users;
    private final SysUserRoleMapper userRoles;
    private final DeptNamePathResolver deptPaths;
    private final SysRoleService roleService;

    /** 口令登录只服务本机联调；线上该入口由配置关闭，用户只走企微。 */
    public SysUser authenticate(String username, String rawPassword) {
        List<SysUser> matched = users.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        SysUser user = matched.stream().filter(item -> username.equals(item.getUsername())).findFirst().orElse(null);
        if (user == null || user.getPasswordHash() == null
                || !PasswordHasher.matches(rawPassword, user.getPasswordHash())) {
            // 不区分「用户不存在」和「口令错误」，避免暴露账号是否存在。
            throw AuthException.unauthorized("账号或密码不正确");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw AuthException.forbidden("账号已停用，请联系管理员");
        }
        return user;
    }

    /** MySQL 默认排序规则忽略大小写，取回后再按原始 userid 精确核对，避免绑定到另一个人的账号。 */
    public SysUser byWecomUserid(String wecomUserid) {
        return users.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getWecomUserid, wecomUserid))
                .stream()
                .filter(item -> wecomUserid.equals(item.getWecomUserid()))
                .findFirst()
                .orElse(null);
    }

    public SysUser requireById(Long userId) {
        SysUser user = users.selectById(userId);
        if (user == null) {
            throw new BizException("账号不存在");
        }
        return user;
    }

    /** 企微登录与口令登录共用：校验启用状态并记录本次登录时间。 */
    public SysUser requireLoginable(Long userId) {
        SysUser user = requireById(userId);
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw AuthException.forbidden("账号已停用，请联系管理员");
        }
        markLogin(user.getId());
        return user;
    }

    public void markLogin(Long userId) {
        SysUser change = new SysUser();
        change.setId(userId);
        change.setLastLoginAt(LocalDateTime.now());
        users.updateById(change);
    }

    public PageResult<SysUserVO> page(SysUserQuery query) {
        LambdaQueryWrapper<SysUser> condition = new LambdaQueryWrapper<>();
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String keyword = query.getKeyword().trim();
            condition.and(wrapper -> wrapper.like(SysUser::getName, keyword)
                    .or().like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getWecomUserid, keyword));
        }
        if (query.getDeptId() != null) {
            condition.eq(SysUser::getMainDeptId, query.getDeptId());
        }
        if (query.getEnabled() != null) {
            condition.eq(SysUser::getEnabled, query.getEnabled());
        }
        condition.orderByAsc(SysUser::getId);
        Page<SysUser> page = users.selectPage(new Page<>(query.getPage(), query.getSize()), condition);
        List<SysUser> records = page.getRecords();
        Map<Long, String> deptNames = deptNames(records);
        List<SysUserVO> list = records.stream()
                .map(user -> toVO(user, deptNames.get(user.getMainDeptId()), userRoles.selectRoleCodes(user.getId())))
                .toList();
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), list);
    }

    /** 用可变 Map 返回：不可变 Map.get(null) 会直接抛 NPE，而本地账号的主部门本来就是空的。 */
    private Map<Long, String> deptNames(List<SysUser> records) {
        return deptPaths.resolve(records.stream().map(SysUser::getMainDeptId).toList());
    }

    /**
     * 能担任部门负责人的账号：负责人按企微 userid 反查下属，本地账号没有 userid 就无从反查，
     * 停用的账号也不该出现在下拉里。label 带上部门路径，同名同事才分得开。
     */
    public List<OptionVO> leaderCandidates() {
        List<SysUser> records = users.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEnabled, true)
                .isNotNull(SysUser::getWecomUserid)
                .orderByAsc(SysUser::getMainDeptId)
                .orderByAsc(SysUser::getId));
        Map<Long, String> deptNames = deptNames(records);
        return records.stream()
                .map(user -> new OptionVO(user.getWecomUserid(), labelWithDept(user, deptNames.get(user.getMainDeptId()))))
                .toList();
    }

    /** 负责人 userid → 姓名；MySQL 排序规则不区分大小写，取回后按原值精确核对。 */
    public Map<String, String> namesByWecomUserids(Collection<String> userids) {
        List<String> wanted = userids.stream().filter(Objects::nonNull).filter(id -> !id.isBlank()).distinct().toList();
        if (wanted.isEmpty()) {
            return Map.of();
        }
        Map<String, String> names = new HashMap<>();
        for (SysUser user : users.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getWecomUserid, wanted))) {
            if (wanted.contains(user.getWecomUserid())) {
                names.put(user.getWecomUserid(), user.getName());
            }
        }
        return names;
    }

    private String labelWithDept(SysUser user, String deptName) {
        return deptName == null || deptName.isBlank() ? user.getName() : user.getName() + "（" + deptName + "）";
    }

    public SysUserVO toVO(SysUser user, String mainDeptName, List<String> roles) {
        return new SysUserVO(user.getId(), user.getWecomUserid(), user.getUsername(), user.getName(),
                user.getPosition(), user.getMainDeptId(), mainDeptName, user.getMobile(), user.getEmail(),
                user.getAvatar(), user.getWecomStatus(), Boolean.TRUE.equals(user.getEnabled()),
                user.getWecomUserid() == null || user.getWecomUserid().isBlank(), user.getLastLoginAt(),
                user.getDirectorySyncedAt(), roles);
    }

    /** 角色是唯一可在本地维护的权限入口；改完立刻踢掉该账号的会话，避免旧令牌继续持有高权限。 */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<String> roleCodes) {
        requireById(userId);
        if (roleCodes == null || roleCodes.isEmpty()) {
            userRoles.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
            StpUtil.logout(userId);
            return;
        }
        List<Long> roleIds = roleCodes.stream().distinct().map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(code -> roleService.requireByCode(code).getId())
                .sorted()
                .toList();
        userRoles.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            SysUserRole binding = new SysUserRole();
            binding.setUserId(userId);
            binding.setRoleId(roleId);
            userRoles.insert(binding);
        }
        StpUtil.logout(userId);
    }

    public List<String> roleCodesOf(Long userId) {
        return userRoles.selectRoleCodes(userId);
    }

    /** 仅由完整通讯录同步调用；组织关系、HR 档案和会话由同步事务统一清理。 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDirectoryUsers(List<Long> userIds) {
        if (userIds.isEmpty()) return;
        userRoles.delete(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));
        users.delete(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds));
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long userId, boolean enabled, Long actorId) {
        SysUser user = requireById(userId);
        if (!enabled && (userId.equals(actorId) || roleCodesOf(userId).contains(PrincipalService.ROLE_ADMIN))) {
            throw new BizException("不能停用本人或管理员账号");
        }
        if (enabled && user.getWecomUserid() != null && !Integer.valueOf(1).equals(user.getWecomStatus())) {
            throw new BizException("企微账号尚未激活或已停用，请先在企微处理并同步");
        }
        SysUser change = new SysUser();
        change.setId(userId);
        change.setEnabled(enabled);
        users.updateById(change);
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() { StpUtil.logout(userId); }
                });
    }

    public void grantRole(SysUser user, String roleCode) {
        if (userRoles.selectRoleCodes(user.getId()).contains(roleCode)) {
            return;
        }
        SysRole role = roleService.requireByCode(roleCode);
        SysUserRole binding = new SysUserRole();
        binding.setUserId(user.getId());
        binding.setRoleId(role.getId());
        userRoles.insert(binding);
    }

}
