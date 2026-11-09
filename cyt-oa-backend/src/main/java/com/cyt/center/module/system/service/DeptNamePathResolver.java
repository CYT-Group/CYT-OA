package com.cyt.center.module.system.service;

import com.cyt.center.module.system.mapper.SysDeptMapper;
import com.cyt.center.module.system.model.entity.SysDept;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 部门展示名：按 parent_id 现拼「畅韵科技/市场部/耳机市场部」。
 * 不读 sys_dept.path —— 那一列直接来自企微网关，口径跟着上游变过（历史数据是 deptId 串，
 * 现在的网关给的是名称串），把它端给用户迟早会看到 /1/6/19/63/。
 */
@Service
@RequiredArgsConstructor
public class DeptNamePathResolver {

    private static final int MAX_DEPTH = 32;

    private final SysDeptMapper departments;

    /**
     * 停用部门照样参与解析：人员历史归属会引用它们，解析不到就整列变空。
     * 返回值固定用可变 Map：本地账号的主部门是 NULL，而不可变 Map 连 get(null) 都会抛 NPE。
     */
    public Map<Long, String> resolve(Collection<Long> deptIds) {
        Set<Long> wanted = deptIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> paths = new HashMap<>();
        if (wanted.isEmpty()) {
            return paths;
        }
        Map<Long, String> all = resolveAll(departments.selectList(null));
        for (Long deptId : wanted) {
            String path = all.get(deptId);
            if (path != null) {
                paths.put(deptId, path);
            }
        }
        return paths;
    }

    public String resolve(Long deptId) {
        return deptId == null ? null : resolve(List.of(deptId)).get(deptId);
    }

    /** 调用方已经握着整份部门列表时走这个重载，省一次全表查询。 */
    public Map<Long, String> resolveAll(Collection<SysDept> universe) {
        Map<Long, SysDept> byId = new HashMap<>();
        for (SysDept dept : universe) {
            byId.put(dept.getId(), dept);
        }
        Map<Long, String> paths = new HashMap<>();
        for (Long deptId : byId.keySet()) {
            String path = join(deptId, byId);
            if (path != null) {
                paths.put(deptId, path);
            }
        }
        return paths;
    }

    private String join(Long deptId, Map<Long, SysDept> byId) {
        List<String> parts = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        SysDept cursor = byId.get(deptId);
        while (cursor != null && parts.size() < MAX_DEPTH && visited.add(cursor.getId())) {
            parts.add(cursor.getName());
            Long parentId = cursor.getParentId();
            cursor = parentId == null || parentId == 0 ? null : byId.get(parentId);
        }
        if (parts.isEmpty()) {
            return null;
        }
        Collections.reverse(parts);
        return String.join("/", parts);
    }
}
