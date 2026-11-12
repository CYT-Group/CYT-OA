package com.cyt.center.module.system.api.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import com.cyt.center.module.system.mapper.SysDeptMapper;
import com.cyt.center.module.system.mapper.SysUserDeptMapper;
import com.cyt.center.module.system.mapper.SysUserMapper;
import com.cyt.center.module.system.model.entity.SysDept;
import com.cyt.center.module.system.model.entity.SysUser;
import com.cyt.center.module.system.model.entity.SysUserDept;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Admin 用户 API 实现类：数据源是本项目的 sys_user（企微通讯录同步而来）。
 *
 * @author 芋道源码
 */
@Service
@RequiredArgsConstructor
public class AdminUserApiImpl implements AdminUserApi {

    /** 芋道 CommonStatusEnum 口径：0 开启、1 停用 */
    private static final Integer STATUS_ENABLE = 0;
    private static final Integer STATUS_DISABLE = 1;

    private final SysUserMapper users;
    private final SysUserDeptMapper userDepartments;
    private final SysDeptMapper departments;
    private final DeptApi deptApi;

    @Override
    public AdminUserRespDTO getUser(Long id) {
        if (id == null) {
            return null;
        }
        SysUser user = users.selectById(id);
        return user == null ? null : convert(user);
    }

    @Override
    public AdminUserRespDTO getUserByMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return null;
        }
        // 取回后按原始手机号精确核对
        return users.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getMobile, mobile)
                        .orderByAsc(SysUser::getId))
                .stream()
                .filter(user -> mobile.equals(user.getMobile()))
                .findFirst()
                .map(this::convert)
                .orElse(null);
    }

    @Override
    public List<AdminUserRespDTO> getUserListBySubordinate(Long id) {
        if (id == null) {
            return new ArrayList<>();
        }
        SysUser leader = users.selectById(id);
        if (leader == null || blank(leader.getWecomUserid())) {
            return new ArrayList<>();
        }
        // 1.1 负责人只记在 sys_dept.leader_userid（企微 userid）上，先取他负责的部门
        Set<Long> deptIds = new LinkedHashSet<>();
        for (SysDept dept : departments.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getLeaderUserid, leader.getWecomUserid())
                .orderByAsc(SysDept::getId))) {
            // 取回后按原始 userid 精确核对
            if (leader.getWecomUserid().equals(dept.getLeaderUserid())) {
                deptIds.add(dept.getId());
            }
        }
        if (deptIds.isEmpty()) {
            return new ArrayList<>();
        }
        // 1.2 连同所有子部门
        Set<Long> scopedDeptIds = new LinkedHashSet<>(deptIds);
        for (DeptRespDTO child : deptApi.getChildDeptList(deptIds)) {
            scopedDeptIds.add(child.getId());
        }
        // 2. 获取部门对应的用户信息，排除自己
        List<AdminUserRespDTO> result = new ArrayList<>();
        for (SysUser user : selectListByDeptIds(scopedDeptIds)) {
            if (!user.getId().equals(id)) {
                result.add(convert(user));
            }
        }
        return result;
    }

    @Override
    public List<AdminUserRespDTO> getUserList(Collection<Long> ids) {
        return convert(selectByIds(ids));
    }

    @Override
    public List<AdminUserRespDTO> getUserListByDeptIds(Collection<Long> deptIds) {
        return convert(selectListByDeptIds(deptIds));
    }

    /** 新库的岗位是企微下发的文本（sys_user.position），没有岗位主数据，按岗位编号查人拿不到结果。 */
    @Override
    public List<AdminUserRespDTO> getUserListByPostIds(Collection<Long> postIds) {
        return new ArrayList<>();
    }

    @Override
    public List<AdminUserRespDTO> getUserListByNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return new ArrayList<>();
        }
        return convert(users.selectList(new LambdaQueryWrapper<SysUser>()
                .like(SysUser::getName, nickname.trim())
                .orderByAsc(SysUser::getId)));
    }

    @Override
    public List<AdminUserRespDTO> getUserListByStatus(Integer status) {
        if (status == null) {
            return new ArrayList<>();
        }
        return convert(users.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEnabled, STATUS_ENABLE.equals(status))
                .orderByAsc(SysUser::getId)));
    }

    @Override
    public AdminUserRespDTO validateUser(Long id) {
        SysUser user = id == null ? null : users.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BizException("名字为【" + user.getName() + "】的用户已被禁用");
        }
        return convert(user);
    }

    @Override
    public void validateUserList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<AdminUserRespDTO> matched = getUserList(ids);
        Set<Long> found = new LinkedHashSet<>();
        for (AdminUserRespDTO user : matched) {
            found.add(user.getId());
            if (!STATUS_ENABLE.equals(user.getStatus())) {
                throw new BizException("名字为【" + user.getNickname() + "】的用户已被禁用");
            }
        }
        for (Long id : ids) {
            if (id == null || !found.contains(id)) {
                throw new BizException("用户不存在");
            }
        }
    }

    private List<SysUser> selectByIds(Collection<Long> ids) {
        Set<Long> distinct = clean(ids);
        if (distinct.isEmpty()) {
            return List.of();
        }
        return users.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, distinct)
                .orderByAsc(SysUser::getId));
    }

    /** 芋道一人在单部门，新库是 main_dept_id + sys_user_dept 多对多：按部门取人时两边并集，才算「所有部门」。 */
    private List<SysUser> selectListByDeptIds(Collection<Long> deptIds) {
        Set<Long> distinct = clean(deptIds);
        if (distinct.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (SysUser user : users.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getMainDeptId, distinct)
                .orderByAsc(SysUser::getId))) {
            userIds.add(user.getId());
        }
        for (SysUserDept binding : userDepartments.selectList(new LambdaQueryWrapper<SysUserDept>()
                .in(SysUserDept::getDeptId, distinct))) {
            userIds.add(binding.getUserId());
        }
        return selectByIds(userIds);
    }

    private static Set<Long> clean(Collection<Long> ids) {
        Set<Long> result = new LinkedHashSet<>();
        if (ids != null) {
            for (Long id : ids) {
                if (id != null) {
                    result.add(id);
                }
            }
        }
        return result;
    }

    private List<AdminUserRespDTO> convert(Collection<SysUser> list) {
        List<AdminUserRespDTO> result = new ArrayList<>(list.size());
        for (SysUser user : list) {
            result.add(convert(user));
        }
        return result;
    }

    private AdminUserRespDTO convert(SysUser user) {
        AdminUserRespDTO dto = new AdminUserRespDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getName());
        dto.setStatus(toCommonStatus(user.getEnabled()));
        dto.setDeptId(user.getMainDeptId());
        dto.setPostIds(Set.of());
        dto.setMobile(user.getMobile());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        return dto;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    /** 新库用 boolean 表达启用状态，这里翻译成芋道 CommonStatusEnum 的 0/1，调用方的状态比较才不用改。 */
    private static Integer toCommonStatus(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? STATUS_ENABLE : STATUS_DISABLE;
    }

}
