package com.cyt.center.module.portal.controller.admin.events;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.portal.controller.admin.events.vo.PortalEventPageReqVO;
import com.cyt.center.module.portal.controller.admin.events.vo.PortalEventRespVO;
import com.cyt.center.module.portal.controller.admin.events.vo.PortalEventSaveReqVO;
import com.cyt.center.module.portal.domain.PortalCapability;
import com.cyt.center.module.portal.service.events.PortalEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 展会/活动日历管理接口层（迁自 Portal 的 routers/events.py 管理端一段）。
 *
 * 写端点统一 @SaCheckPermission(can_manage_events)：Portal 侧 create/update 用 can_manage_events、
 * delete 用 can_delete，迁移契约把这组无审批流的写操作收口到管理能力一把闸。
 */
@Tag(name = "管理后台 - 展会日历")
@RestController
@RequestMapping("/portal/events")
@Validated
@RequiredArgsConstructor
public class PortalEventController {

    private final PortalEventService eventService;

    @GetMapping("/page")
    @Operation(summary = "分页列活动（含全部状态）")
    public Result<PageResult<PortalEventRespVO>> page(@Valid PortalEventPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(eventService.page(pageReqVO), PortalEventRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "单场活动详情（编辑表单用，不看状态）")
    @Parameter(name = "id", description = "活动编号", required = true)
    public Result<PortalEventRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(eventService.get(id), PortalEventRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "新建活动")
    @SaCheckPermission(PortalCapability.MANAGE_EVENTS)
    public Result<PortalEventRespVO> create(@Valid @RequestBody PortalEventSaveReqVO createReqVO) {
        return success(BeanUtils.toBean(eventService.create(createReqVO), PortalEventRespVO.class));
    }

    @PutMapping("/update")
    @Operation(summary = "改活动（全量覆盖）")
    @SaCheckPermission(PortalCapability.MANAGE_EVENTS)
    public Result<PortalEventRespVO> update(@Valid @RequestBody PortalEventSaveReqVO updateReqVO) {
        return success(BeanUtils.toBean(eventService.update(updateReqVO.getId(), updateReqVO),
                PortalEventRespVO.class));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删活动（硬删，删除动作进操作日志）")
    @Parameter(name = "id", description = "活动编号", required = true)
    @SaCheckPermission(PortalCapability.MANAGE_EVENTS)
    public Result<Boolean> delete(@RequestParam("id") Long id) {
        eventService.delete(getLoginUserId(), id);
        return success(true);
    }

}
