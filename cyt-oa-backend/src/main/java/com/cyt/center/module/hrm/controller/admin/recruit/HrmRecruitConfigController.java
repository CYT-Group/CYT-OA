package com.cyt.center.module.hrm.controller.admin.recruit;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.config.HrmRecruitEliminateReasonSaveReqVO;
import com.cyt.center.module.hrm.service.recruit.config.HrmRecruitConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;

@Tag(name = "管理后台 - HRM 招聘设置")
@RestController
@RequestMapping("/hrm/recruit/config")
@Validated
public class HrmRecruitConfigController {

    @Resource
    private HrmRecruitConfigService recruitConfigService;

    @PostMapping("/eliminate-reason/save")
    @Operation(summary = "保存招聘淘汰原因")
    @SaCheckPermission("hrm:recruit:config:update")
    public Result<Boolean> saveRecruitEliminateReason(
            @Valid @RequestBody HrmRecruitEliminateReasonSaveReqVO saveReqVO) {
        recruitConfigService.saveRecruitEliminateReason(saveReqVO);
        return success(true);
    }

    @GetMapping("/eliminate-reason/list")
    @Operation(summary = "获得招聘淘汰原因列表")
    @SaCheckPermission("hrm:recruit:config:query")
    public Result<List<String>> getRecruitEliminateReasonList() {
        return success(recruitConfigService.getRecruitEliminateReasonList());
    }

}
