package com.cyt.center.module.me.controller;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.module.me.model.cmd.MeCmds.PreferenceCmd;
import com.cyt.center.module.me.model.vo.MeVos.Reminder;
import com.cyt.center.module.me.model.vo.MeVos.ReminderPreference;
import com.cyt.center.module.me.service.MeReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "个人中心 - 我的提醒")
@RestController
@RequestMapping("/api/me/reminders")
public class MeReminderController {

    @Resource
    private MeReminderService meReminderService;

    @GetMapping
    @Operation(summary = "提醒分页，取列表前先按真实业务状态同步一次")
    public Result<PageResult<Reminder>> page(@RequestParam(value = "cat", required = false) String category,
                                             @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly,
                                             @Valid PageParam pageParam) {
        Long userId = getLoginUserId();
        meReminderService.sync(userId);
        return success(meReminderService.page(userId, category, unreadOnly, pageParam));
    }

    @GetMapping("/recent")
    @Operation(summary = "最新几条提醒，概览页用")
    public Result<List<Reminder>> recent(@RequestParam(value = "limit", defaultValue = "5") int limit) {
        Long userId = getLoginUserId();
        meReminderService.sync(userId);
        return success(meReminderService.recent(userId, limit));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读条数")
    public Result<Map<String, Long>> unreadCount() {
        Long userId = getLoginUserId();
        meReminderService.sync(userId);
        return success(Map.of("count", meReminderService.unreadCount(userId)));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条已读")
    public Result<Boolean> markRead(@PathVariable("id") Long id) {
        meReminderService.markRead(getLoginUserId(), id);
        return success(true);
    }

    @PutMapping("/read-all")
    @Operation(summary = "按分类全部已读，cat 为空则全部")
    public Result<Map<String, Integer>> markAllRead(@RequestParam(value = "cat", required = false) String category) {
        return success(Map.of("count", meReminderService.markAllRead(getLoginUserId(), category)));
    }

    @GetMapping("/preferences")
    @Operation(summary = "提醒偏好")
    public Result<ReminderPreference> preferences() {
        return success(meReminderService.preferences(getLoginUserId()));
    }

    @PutMapping("/preferences")
    @Operation(summary = "保存提醒偏好")
    public Result<Boolean> savePreferences(@Valid @RequestBody PreferenceCmd cmd) {
        meReminderService.savePreferences(getLoginUserId(), cmd);
        return success(true);
    }
}
