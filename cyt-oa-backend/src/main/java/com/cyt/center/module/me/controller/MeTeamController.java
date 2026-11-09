package com.cyt.center.module.me.controller;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.me.model.cmd.MeCmds.RecognizeCmd;
import com.cyt.center.module.me.model.vo.MeVos.ColleagueMilestoneView;
import com.cyt.center.module.me.model.vo.MeVos.ReportBrief;
import com.cyt.center.module.me.service.MeProfileService;
import com.cyt.center.module.me.service.MeTeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "个人中心 - 我的团队")
@RestController
@RequestMapping("/api/me/team")
public class MeTeamController {

    @Resource
    private MeTeamService meTeamService;
    @Resource
    private MeProfileService meProfileService;

    @GetMapping("/board")
    @Operation(summary = "成员 × 最近 8 周的周报看板，附等我的点评")
    public Result<MeTeamService.Board> board() {
        Long userId = getLoginUserId();
        meTeamService.requireManager(userId);
        return success(meTeamService.board(userId));
    }

    @GetMapping("/pending-reviews")
    @Operation(summary = "下属已提交、我还没点评的汇报")
    public Result<List<ReportBrief>> pendingReviews() {
        Long userId = getLoginUserId();
        return success(meTeamService.pendingReviews(userId, meTeamService.myTeam(userId)));
    }

    @GetMapping("/milestones")
    @Operation(summary = "团队里程碑雷达：下属的生日 / 周年 / 待评估的转正")
    public Result<List<ColleagueMilestoneView>> milestones() {
        return success(meProfileService.teamMilestoneViews(getLoginUserId()));
    }

    @PostMapping("/recognition")
    @Operation(summary = "给下属点赞或提名本月之星（点赞按周幂等，之星按月只留一个）")
    public Result<Boolean> recognize(@Valid @RequestBody RecognizeCmd cmd) {
        meTeamService.recognize(getLoginUserId(), cmd);
        return success(true);
    }
}
