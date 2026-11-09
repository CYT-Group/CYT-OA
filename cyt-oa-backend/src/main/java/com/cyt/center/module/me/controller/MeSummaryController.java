package com.cyt.center.module.me.controller;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.me.model.vo.MeVos.Overview;
import com.cyt.center.module.me.model.vo.MeVos.Summary;
import com.cyt.center.module.me.service.MeSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "个人中心 - 概览与徽标")
@RestController
@RequestMapping("/api/me")
public class MeSummaryController {

    @Resource
    private MeSummaryService meSummaryService;

    @GetMapping("/summary")
    @Operation(summary = "侧边栏身份摘要与四类徽标")
    public Result<Summary> summary() {
        return success(meSummaryService.summary(getLoginUserId()));
    }

    @GetMapping("/overview")
    @Operation(summary = "概览页：问候 + 当前节奏 + 统一待办 + 最近报告 + 最新提醒")
    public Result<Overview> overview() {
        return success(meSummaryService.overview(getLoginUserId()));
    }
}
