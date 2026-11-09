package com.cyt.center.module.me.controller;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.me.model.cmd.MeCmds.FollowCmd;
import com.cyt.center.module.me.model.cmd.MeCmds.UnfollowCmd;
import com.cyt.center.module.me.model.vo.MeVos.RhythmNodeView;
import com.cyt.center.module.me.service.MeRhythmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "个人中心 - 大促与新品节奏")
@RestController
@RequestMapping("/api/me/rhythm")
public class MeRhythmController {

    @Resource
    private MeRhythmService meRhythmService;

    @GetMapping("/nodes")
    @Operation(summary = "节奏节点列表，日历视图与列表视图共用")
    public Result<List<RhythmNodeView>> nodes(
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "kind", required = false) String kind) {
        return success(meRhythmService.list(getLoginUserId(), from, to, kind));
    }

    @GetMapping("/nodes/{id}")
    @Operation(summary = "节点详情，含各部门作战安排")
    public Result<RhythmNodeView> node(@PathVariable("id") Long id) {
        return success(meRhythmService.get(getLoginUserId(), id));
    }

    @PostMapping("/follow")
    @Operation(summary = "关注节点并设置提前提醒天数")
    public Result<Boolean> follow(@Valid @RequestBody FollowCmd cmd) {
        meRhythmService.follow(getLoginUserId(), cmd.getNodeId(), cmd.getRemindBefore());
        return success(true);
    }

    @PostMapping("/unfollow")
    @Operation(summary = "取消关注")
    public Result<Boolean> unfollow(@Valid @RequestBody UnfollowCmd cmd) {
        meRhythmService.unfollow(getLoginUserId(), cmd.getNodeId());
        return success(true);
    }
}
