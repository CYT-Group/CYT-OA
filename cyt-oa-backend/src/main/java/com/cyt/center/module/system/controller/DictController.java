package com.cyt.center.module.system.controller;

import com.cyt.center.core.result.Result;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.module.system.model.entity.SysDictData;
import com.cyt.center.module.system.model.entity.SysDictType;
import com.cyt.center.module.system.service.SysDictService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据字典读取。后台维护页面在移植业务模块时一并补，这里先把唯一数据源定下来。
 */
@RestController
@RequestMapping("/admin-api/system/dict")
@RequiredArgsConstructor
public class DictController {

    private final SysDictService dictService;
    private final CurrentUser currentUser;

    @GetMapping("/types")
    public Result<List<SysDictType>> types() {
        currentUser.requirePermission("system:dict:query");
        return Result.ok(dictService.types());
    }

    @GetMapping("/data")
    public Result<List<SysDictData>> data(
            @RequestParam("type") @NotBlank(message = "不能为空") @Size(max = 64, message = "长度不能超过 64") String type) {
        currentUser.requirePermission("system:dict:query");
        return Result.ok(dictService.data(type));
    }

}
