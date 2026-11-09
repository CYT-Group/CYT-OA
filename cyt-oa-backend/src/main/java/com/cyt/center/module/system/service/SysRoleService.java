package com.cyt.center.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.system.mapper.SysRoleMapper;
import com.cyt.center.module.system.model.entity.SysRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roles;

    public List<SysRole> list() {
        return roles.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    public SysRole requireByCode(String code) {
        SysRole role = roles.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, code));
        if (role == null) {
            throw new BizException("角色不存在：" + code);
        }
        return role;
    }

}
