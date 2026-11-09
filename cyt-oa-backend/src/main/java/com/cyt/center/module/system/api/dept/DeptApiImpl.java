package com.cyt.center.module.system.api.dept;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import com.cyt.center.module.system.mapper.SysDeptMapper;
import com.cyt.center.module.system.mapper.SysUserMapper;
import com.cyt.center.module.system.model.entity.SysDept;
import com.cyt.center.module.system.model.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin 部门 API 实现类：数据源是本项目的 sys_dept（企微通讯录同步而来）。
 *
 * @author 芋道源码
 */
@Service
@RequiredArgsConstructor
public class DeptApiImpl implements DeptApi {

    /** 芋道 CommonStatusEnum 口径：0 开启、1 停用 */
    private static final Integer STATUS_ENABLE = 0;
    private static final Integer STATUS_DISABLE = 1;
    /** 根部门的 parent_id，与企微网关口径一致 */
    private static final Long ROOT_PARENT_ID = 0L;
    /** 层级遍历上限，脏数据成环时不至于把库拖死 */
    private static final int MAX_DEPTH = Short.MAX_VALUE;

    private final SysDeptMapper departments;
    private final SysUserMapper users;

    @Override
    public DeptRespDTO getDept(Long id) {
        if (id == null) {
            return null;
        }
        SysDept dept = departments.selectById(id);
        return dept == null ? null : convert(List.of(dept)).get(0);
    }

    @Override
    public List<DeptRespDTO> getDeptList(Collection<Long> ids) {
        return convert(selectByIds(ids));
    }

    @Override
    public void validateDeptList(Collection<Long> ids) {
        if (empty(ids)) {
            return;
        }
        Map<Long, DeptRespDTO> deptMap = getDeptMap(ids);
        for (Long id : ids) {
            DeptRespDTO dept = id == null ? null : deptMap.get(id);
            if (dept == null) {
                throw new BizException("当前部门不存在");
            }
            if (!STATUS_ENABLE.equals(dept.getStatus())) {
                throw new BizException("部门(" + dept.getName() + ")不处于开启状态，不允许选择");
            }
        }
    }

    @Override
    public List<DeptRespDTO> getChildDeptList(Collection<Long> ids) {
        if (empty(ids)) {
            return new ArrayList<>();
        }
        List<SysDept> children = new ArrayList<>();
        Set<Long> visited = new LinkedHashSet<>();
        Collection<Long> parentIds = clean(ids);
        for (int depth = 0; depth < MAX_DEPTH && !parentIds.isEmpty(); depth++) {
            List<SysDept> layer = departments.selectList(new LambdaQueryWrapper<SysDept>()
                    .in(SysDept::getParentId, parentIds)
                    .orderByAsc(SysDept::getId));
            if (layer.isEmpty()) {
                break;
            }
            Set<Long> next = new LinkedHashSet<>();
            for (SysDept dept : layer) {
                if (visited.add(dept.getId())) {
                    children.add(dept);
                    next.add(dept.getId());
                }
            }
            parentIds = next;
        }
        return convert(children);
    }

    @Override
    public List<DeptRespDTO> getParentDeptList(Long id) {
        if (id == null) {
            return new ArrayList<>();
        }
        List<SysDept> parents = new ArrayList<>();
        Set<Long> visited = new LinkedHashSet<>();
        visited.add(id);
        SysDept dept = departments.selectById(id);
        for (int depth = 0; depth < MAX_DEPTH && dept != null && dept.getParentId() != null; depth++) {
            Long parentId = dept.getParentId();
            if (ROOT_PARENT_ID.equals(parentId) || !visited.add(parentId)) {
                break;
            }
            SysDept parent = departments.selectById(parentId);
            if (parent == null) {
                break;
            }
            parents.add(parent);
            dept = parent;
        }
        return convert(parents);
    }

    private List<SysDept> selectByIds(Collection<Long> ids) {
        Set<Long> distinct = clean(ids);
        if (distinct.isEmpty()) {
            return List.of();
        }
        return departments.selectList(new LambdaQueryWrapper<SysDept>()
                .in(SysDept::getId, distinct)
                .orderByAsc(SysDept::getId));
    }

    /** 入参可能带 null 与重复值，MyBatis-Plus 的 in() 拼空集合会直接产生非法 SQL。 */
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

    private List<DeptRespDTO> convert(Collection<SysDept> depts) {
        Map<String, Long> leaderUserIds = resolveLeaderUserIds(depts);
        List<DeptRespDTO> list = new ArrayList<>(depts.size());
        for (SysDept dept : depts) {
            DeptRespDTO dto = new DeptRespDTO();
            dto.setId(dept.getId());
            dto.setName(dept.getName());
            dto.setParentId(dept.getParentId());
            dto.setLeaderUserId(dept.getLeaderUserid() == null ? null : leaderUserIds.get(dept.getLeaderUserid()));
            dto.setStatus(Boolean.TRUE.equals(dept.getActive()) ? STATUS_ENABLE : STATUS_DISABLE);
            list.add(dto);
        }
        return list;
    }

    /** sys_dept.leader_userid 存的是企微 userid，须经 sys_user.wecom_userid 换成数值主键；通讯录里查不到人就返回 null，不做猜测。 */
    private Map<String, Long> resolveLeaderUserIds(Collection<SysDept> depts) {
        Set<String> userids = new LinkedHashSet<>();
        for (SysDept dept : depts) {
            String userid = dept.getLeaderUserid();
            if (userid != null && !userid.isBlank()) {
                userids.add(userid);
            }
        }
        Map<String, Long> result = new HashMap<>();
        if (userids.isEmpty()) {
            return result;
        }
        // MySQL 默认排序规则忽略大小写，取回后按原始 userid 精确核对，避免把负责人算到同名的另一个人身上
        for (SysUser user : users.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getWecomUserid, userids)
                .orderByAsc(SysUser::getId))) {
            String userid = user.getWecomUserid();
            if (userid != null && userids.contains(userid)) {
                result.putIfAbsent(userid, user.getId());
            }
        }
        return result;
    }

    private static boolean empty(Collection<?> ids) {
        return ids == null || ids.isEmpty();
    }

}
