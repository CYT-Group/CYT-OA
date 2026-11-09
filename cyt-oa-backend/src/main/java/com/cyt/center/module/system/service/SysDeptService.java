package com.cyt.center.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.system.mapper.SysDeptMapper;
import com.cyt.center.module.system.model.entity.SysDept;
import com.cyt.center.module.system.model.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysDeptService {

    private final SysDeptMapper departments;
    private final SysUserService userService;

    public List<SysDept> listActive() {
        return departments.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getActive, true)
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getSortOrder)
                .orderByAsc(SysDept::getId));
    }

    /**
     * 指定部门负责人：只认企微 userid（负责人视角的下属范围靠它反查），账号必须存在且未停用；留空即取消指定。
     */
    public void updateLeader(Long deptId, String leaderUserid) {
        SysDept dept = departments.selectById(deptId);
        if (dept == null || !Boolean.TRUE.equals(dept.getActive())) {
            throw new BizException("部门不存在或已停用");
        }
        String userid = leaderUserid == null || leaderUserid.isBlank() ? "" : leaderUserid.trim();
        if (!userid.isEmpty()) {
            SysUser leader = userService.byWecomUserid(userid);
            if (leader == null) {
                throw new BizException("负责人账号不存在，请先执行通讯录同步");
            }
            if (!Boolean.TRUE.equals(leader.getEnabled())) {
                throw new BizException("该账号已停用，不能担任负责人");
            }
        }
        // updateById 会跳过 null 字段，取消指定要显式把列写回 NULL
        departments.update(null, new LambdaUpdateWrapper<SysDept>()
                .set(SysDept::getLeaderUserid, userid)
                .eq(SysDept::getId, deptId));
    }

}
