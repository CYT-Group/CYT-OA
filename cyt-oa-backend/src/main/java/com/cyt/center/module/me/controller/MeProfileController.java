package com.cyt.center.module.me.controller;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.me.model.cmd.MeCmds;
import com.cyt.center.module.me.model.cmd.MeCmds.VoiceCmd;
import com.cyt.center.module.me.model.vo.MeVos.ColleagueMilestoneView;
import com.cyt.center.module.me.model.vo.MeVos.Footprint;
import com.cyt.center.module.me.model.vo.MeVos.MilestoneView;
import com.cyt.center.module.me.model.vo.MeVos.Profile;
import com.cyt.center.module.me.model.vo.MeVos.RecognitionView;
import com.cyt.center.module.me.model.vo.MeVos.VoiceView;
import com.cyt.center.module.me.service.MeProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "个人中心 - 我的资料与足迹")
@RestController
@RequestMapping("/api/me")
public class MeProfileController {

    @Resource
    private MeProfileService meProfileService;

    @GetMapping("/profile")
    @Operation(summary = "人事档案摘要 + 足迹 + 提醒偏好")
    public Result<Profile> profile() {
        return success(meProfileService.profile(getLoginUserId()));
    }

    @GetMapping("/footprint")
    @Operation(summary = "足迹计数")
    public Result<Footprint> footprint() {
        return success(meProfileService.footprint(getLoginUserId()));
    }

    @GetMapping("/milestones")
    @Operation(summary = "生日、入职周年、转正、晋级等里程碑")
    public Result<List<MilestoneView>> milestones() {
        return success(meProfileService.milestoneViews(getLoginUserId()));
    }

    @GetMapping("/milestones/colleagues")
    @Operation(summary = "临近的同事生日与周年，可送祝福")
    public Result<List<ColleagueMilestoneView>> colleagueMilestones() {
        return success(meProfileService.colleagueMilestoneViews(getLoginUserId()));
    }

    @PostMapping("/milestones/blessing")
    @Operation(summary = "给同事的生日 / 周年送祝福")
    public Result<Boolean> bless(@Valid @RequestBody MeCmds.BlessCmd cmd) {
        meProfileService.sendBlessing(getLoginUserId(), cmd);
        return success(true);
    }

    @GetMapping("/recognitions")
    @Operation(summary = "收到的认可，period 形如 2026-11，为空取全部")
    public Result<List<RecognitionView>> recognitions(@RequestParam(value = "period", required = false) String period) {
        return success(meProfileService.recognitionViews(getLoginUserId(), period));
    }

    @GetMapping("/voices")
    @Operation(summary = "我发过的心声")
    public Result<List<VoiceView>> voices() {
        return success(meProfileService.voiceViews(getLoginUserId()));
    }

    @PostMapping("/voices")
    @Operation(summary = "提建议 / 写信 / 填调研，可匿名")
    public Result<Long> submitVoice(@Valid @RequestBody VoiceCmd cmd) {
        return success(meProfileService.submitVoice(getLoginUserId(), cmd));
    }
}
