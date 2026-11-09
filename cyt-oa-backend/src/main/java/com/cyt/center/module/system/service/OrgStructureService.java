package com.cyt.center.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import com.cyt.center.module.system.mapper.SysDeptMapper;
import com.cyt.center.module.system.mapper.SysUserDeptMapper;
import com.cyt.center.module.system.mapper.SysUserMapper;
import com.cyt.center.module.system.model.entity.SysDept;
import com.cyt.center.module.system.model.entity.SysUser;
import com.cyt.center.module.system.model.entity.SysUserDept;
import com.cyt.center.module.system.model.vo.EmployeeDetailVO;
import com.cyt.center.module.system.model.vo.OrgDeptNodeVO;
import com.cyt.center.module.system.model.vo.OrgMemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组织架构只读查询：部门树、部门成员、员工详情。人员与部门本体只由通讯录同步写入。
 */
@Service
@RequiredArgsConstructor
public class OrgStructureService {

    private final SysDeptMapper departments;
    private final SysUserMapper users;
    private final SysUserDeptMapper userDepts;
    private final HrmEmployeeMapper employees;
    private final SysUserService userService;
    private final DeptNamePathResolver deptPaths;

    public List<OrgDeptNodeVO> tree() {
        List<SysDept> depts = departments.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getActive, true)
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getSortOrder)
                .orderByAsc(SysDept::getId));
        Map<Long, Long> counts = new HashMap<>();
        for (Map<String, Object> row : userDepts.countByDept()) {
            Object deptId = row.get("dept_id");
            Object memberCount = row.get("member_count");
            if (deptId instanceof Number && memberCount instanceof Number) {
                counts.merge(((Number) deptId).longValue(), ((Number) memberCount).longValue(), Long::sum);
            }
        }
        Map<String, String> leaderNames = userService.namesByWecomUserids(
                depts.stream().map(SysDept::getLeaderUserid).toList());
        Map<Long, String> namePaths = deptPaths.resolveAll(depts);
        return depts.stream()
                .map(dept -> new OrgDeptNodeVO(dept.getId(), dept.getParentId(), dept.getName(),
                        namePaths.get(dept.getId()), dept.getLeaderUserid(), leaderNames.get(dept.getLeaderUserid()),
                        counts.getOrDefault(dept.getId(), 0L)))
                .toList();
    }

    public PageResult<OrgMemberVO> members(Long deptId, boolean includeChildren, long pageNo, long pageSize) {
        SysDept dept = requireActive(deptId);
        List<Long> scope = new ArrayList<>(List.of(deptId));
        if (includeChildren) {
            List<SysDept> all = departments.selectList(new LambdaQueryWrapper<SysDept>().eq(SysDept::getActive, true));
            boolean changed;
            do {
                changed = false;
                for (SysDept other : all) {
                    if (!scope.contains(other.getId()) && scope.contains(other.getParentId())) {
                        scope.add(other.getId());
                        changed = true;
                    }
                }
            } while (changed);
        }
        List<SysUserDept> bindings = userDepts.selectList(
                new LambdaQueryWrapper<SysUserDept>().in(SysUserDept::getDeptId, scope));
        Map<Long, Set<Long>> deptsOfUser = new LinkedHashMap<>();
        for (SysUserDept binding : bindings) {
            deptsOfUser.computeIfAbsent(binding.getUserId(), key -> new LinkedHashSet<>()).add(binding.getDeptId());
        }
        List<Long> userIds = new ArrayList<>(deptsOfUser.keySet());
        if (userIds.isEmpty()) {
            return new PageResult<>(0L);
        }
        Page<SysUser> page = users.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds).orderByAsc(SysUser::getId));
        // 查询范围只决定哪些人入选；每个人仍展示完整兼属部门和主部门。
        List<Long> pageIds = page.getRecords().stream().map(SysUser::getId).toList();
        if (!pageIds.isEmpty()) {
            deptsOfUser.clear();
            for (SysUserDept binding : userDepts.selectList(new LambdaQueryWrapper<SysUserDept>()
                    .in(SysUserDept::getUserId, pageIds))) {
                deptsOfUser.computeIfAbsent(binding.getUserId(), key -> new LinkedHashSet<>()).add(binding.getDeptId());
            }
        }
        Set<Long> allDeptIds = deptsOfUser.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        page.getRecords().stream().map(SysUser::getMainDeptId).filter(Objects::nonNull).forEach(allDeptIds::add);
        Map<Long, String> deptNames = namesOf(allDeptIds);
        Map<Long, String> jobNumbers = jobNumbersOf(page.getRecords().stream().map(SysUser::getId).toList());
        List<OrgMemberVO> list = page.getRecords().stream()
                .map(user -> {
                    List<String> names = deptsOfUser.getOrDefault(user.getId(), Set.of()).stream()
                            .map(deptNames::get)
                            .filter(Objects::nonNull)
                            .toList();
                    return new OrgMemberVO(user.getId(), user.getWecomUserid(), user.getName(), user.getPosition(),
                            user.getMobile(), user.getEmail(), deptNames.get(user.getMainDeptId()), names,
                            Boolean.TRUE.equals(user.getEnabled()), jobNumbers.get(user.getId()));
                })
                .toList();
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), list);
    }

    public EmployeeDetailVO detail(Long userId) {
        SysUser user = users.selectById(userId);
        if (user == null) {
            throw new BizException("账号不存在");
        }
        List<HrmEmployeeDO> archives = employees.selectList(new LambdaQueryWrapper<HrmEmployeeDO>()
                .eq(HrmEmployeeDO::getUserId, userId));
        if (archives.size() > 1) throw new BizException("账号存在多份有效档案，请联系 HR 处理");
        HrmEmployeeDO archive = archives.isEmpty() ? null : archives.getFirst();
        List<SysUserDept> bindings = userDepts.selectList(
                new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, userId));
        Set<Long> deptIds = bindings.stream().map(SysUserDept::getDeptId).collect(Collectors.toSet());
        if (user.getMainDeptId() != null) deptIds.add(user.getMainDeptId());
        Map<Long, SysDept> deptMap = deptIds.isEmpty() ? new HashMap<>() : departments.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, dept -> dept));

        EmployeeDetailVO vo = new EmployeeDetailVO();
        vo.setUserId(user.getId());
        vo.setWecomUserid(user.getWecomUserid());
        vo.setName(user.getName());
        vo.setPosition(user.getPosition());
        vo.setMobile(user.getMobile());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setWecomStatus(user.getWecomStatus());
        vo.setEnabled(Boolean.TRUE.equals(user.getEnabled()));
        vo.setDirectorySyncedAt(user.getDirectorySyncedAt());

        SysDept main = deptMap.get(user.getMainDeptId());
        Map<Long, String> deptNames = deptPaths.resolve(deptIds);
        vo.setMainDeptName(deptNames.get(user.getMainDeptId()));
        vo.setDeptNames(deptIds.stream().map(deptNames::get).filter(Objects::nonNull).toList());
        if (main != null && main.getLeaderUserid() != null && !main.getLeaderUserid().isBlank()) {
            vo.setLeaderName(userService.namesByWecomUserids(List.of(main.getLeaderUserid())).get(main.getLeaderUserid()));
        }

        if (archive != null) {
            vo.setEmployeeId(archive.getId());
            vo.setJobNumber(archive.getJobNumber());
            vo.setEntryStatus(archive.getEntryStatus());
            if (archive.getLeaderEmployeeId() != null) {
                HrmEmployeeDO manager = employees.selectById(archive.getLeaderEmployeeId());
                vo.setManagerName(manager == null ? null : manager.getName());
            }
        }
        return vo;
    }

    private SysDept requireActive(Long deptId) {
        SysDept dept = departments.selectById(deptId);
        if (dept == null || !Boolean.TRUE.equals(dept.getActive())) {
            throw new BizException("部门不存在或已停用");
        }
        return dept;
    }

    private Map<Long, String> namesOf(Set<Long> deptIds) {
        return deptPaths.resolve(deptIds);
    }

    private Map<Long, String> jobNumbersOf(List<Long> userIds) {
        Map<Long, String> jobNumbers = new HashMap<>();
        if (userIds.isEmpty()) {
            return jobNumbers;
        }
        for (HrmEmployeeDO archive : employees.selectList(
                new LambdaQueryWrapper<HrmEmployeeDO>().in(HrmEmployeeDO::getUserId, userIds))) {
            if (archive.getUserId() != null) {
                jobNumbers.put(archive.getUserId(), archive.getJobNumber());
            }
        }
        return jobNumbers;
    }

}
