package com.cyt.center.module.oa.controller.admin.leave;

import cn.dev33.satoken.annotation.SaCheckPermission;

import cn.hutool.core.collection.CollUtil;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.collection.MapUtils;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplySaveReqVO;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplySubmitReqVO;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplyPageReqVO;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplyRespVO;
import com.cyt.center.module.oa.dal.dataobject.leave.OaLeaveApplyDO;
import com.cyt.center.module.oa.service.leave.OaLeaveApplyService;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertList;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertSet;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 请假申请")
@RestController
@RequestMapping("/oa/leave-apply")
@Validated
public class OaLeaveApplyController {

    @Resource
    private OaLeaveApplyService leaveApplyService;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建请假申请草稿")
    @SaCheckPermission("oa:leave-apply:create")
    public Result<Long> createLeaveApply(@Valid @RequestBody OaLeaveApplySaveReqVO createReqVO) {
        return success(leaveApplyService.createLeaveApply(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新请假申请草稿")
    @SaCheckPermission("oa:leave-apply:create")
    public Result<Boolean> updateLeaveApply(@Valid @RequestBody OaLeaveApplySaveReqVO saveReqVO) {
        leaveApplyService.updateLeaveApply(saveReqVO, getLoginUserId());
        return success(true);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交请假申请")
    @SaCheckPermission("oa:leave-apply:create")
    public Result<Boolean> submitLeaveApply(@Valid @RequestBody OaLeaveApplySubmitReqVO submitReqVO) {
        leaveApplyService.submitLeaveApply(submitReqVO, getLoginUserId());
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得本人请假申请分页")
    @SaCheckPermission("oa:leave-apply:query")
    public Result<PageResult<OaLeaveApplyRespVO>> getLeaveApplyPage(@Valid OaLeaveApplyPageReqVO pageReqVO) {
        PageResult<OaLeaveApplyDO> page = leaveApplyService.getLeaveApplyPage(getLoginUserId(), pageReqVO);
        return success(new PageResult<>(buildLeaveApplyRespVOList(page.getList()), page.getTotal()));
    }

    @GetMapping("/get")
    @Operation(summary = "获得请假申请详情")
    @Parameter(name = "id", description = "申请编号", required = true, example = "1024")
    @SaCheckPermission("oa:leave-apply:query")
    public Result<OaLeaveApplyRespVO> getLeaveApply(@RequestParam("id") Long id) {
        OaLeaveApplyDO application = leaveApplyService.getLeaveApply(id);
        return success(buildLeaveApplyRespVO(application));
    }

    // ==================== 拼接 VO ====================

    /**
     * 拼接请假申请详情
     *
     * @param application 申请
     * @return 请假申请响应
     */
    private OaLeaveApplyRespVO buildLeaveApplyRespVO(OaLeaveApplyDO application) {
        if (application == null) {
            return null;
        }
        return CollUtil.getFirst(buildLeaveApplyRespVOList(Collections.singletonList(application)));
    }

    /**
     * 批量拼接申请人展示信息
     *
     * @param applications 申请列表
     * @return 响应列表
     */
    private List<OaLeaveApplyRespVO> buildLeaveApplyRespVOList(List<OaLeaveApplyDO> applications) {
        if (CollUtil.isEmpty(applications)) {
            return Collections.emptyList();
        }
        // 1. 批量查询申请人
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(
                convertSet(applications, application -> Long.valueOf(application.getCreator())));
        // 2. 转换并拼接 VO
        return convertList(applications, application -> {
            OaLeaveApplyRespVO vo = BeanUtils.toBean(application, OaLeaveApplyRespVO.class);
            MapUtils.findAndThen(users, Long.valueOf(application.getCreator()), user -> vo.setCreatorName(user.getNickname()));
            return vo;
        });
    }

}
