package com.cyt.center.module.hrm.controller.admin.performance;

import cn.dev33.satoken.annotation.SaCheckPermission;

import cn.hutool.core.collection.CollUtil;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.collection.MapUtils;
import com.cyt.center.framework.common.util.number.NumberUtils;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.performance.vo.resulttemplate.HrmPerformanceResultTemplatePageReqVO;
import com.cyt.center.module.hrm.controller.admin.performance.vo.resulttemplate.HrmPerformanceResultTemplateRespVO;
import com.cyt.center.module.hrm.controller.admin.performance.vo.resulttemplate.HrmPerformanceResultTemplateSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.performance.config.HrmPerformanceResultTemplateDO;
import com.cyt.center.module.hrm.service.performance.config.HrmPerformanceResultTemplateService;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertSet;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Tag(name = "管理后台 - HRM 绩效结果模板")
@RestController
@RequestMapping("/hrm/performance/result-template")
@Validated
public class HrmPerformanceResultTemplateController {

    @Resource
    private HrmPerformanceResultTemplateService resultTemplateService;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建绩效结果模板")
    @SaCheckPermission("hrm:performance:result-template:create")
    public Result<Long> createPerformanceResultTemplate(
            @Valid @RequestBody HrmPerformanceResultTemplateSaveReqVO createReqVO) {
        return success(resultTemplateService.createPerformanceResultTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新绩效结果模板")
    @SaCheckPermission("hrm:performance:result-template:update")
    public Result<Boolean> updatePerformanceResultTemplate(
            @Valid @RequestBody HrmPerformanceResultTemplateSaveReqVO updateReqVO) {
        resultTemplateService.updatePerformanceResultTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除绩效结果模板")
    @Parameter(name = "id", description = "模板编号", required = true, example = "1024")
    @SaCheckPermission("hrm:performance:result-template:delete")
    public Result<Boolean> deletePerformanceResultTemplate(@RequestParam("id") Long id) {
        resultTemplateService.deletePerformanceResultTemplate(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除绩效结果模板")
    @Parameter(name = "ids", description = "模板编号列表", required = true, example = "1024,1025")
    @SaCheckPermission("hrm:performance:result-template:delete")
    public Result<Boolean> deletePerformanceResultTemplateList(@RequestParam("ids") List<Long> ids) {
        resultTemplateService.deletePerformanceResultTemplateList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得绩效结果模板")
    @Parameter(name = "id", description = "模板编号", required = true, example = "1024")
    @SaCheckPermission("hrm:performance:result-template:query")
    public Result<HrmPerformanceResultTemplateRespVO> getPerformanceResultTemplate(
            @RequestParam("id") Long id) {
        // 1. 获得结果模板
        HrmPerformanceResultTemplateDO template = resultTemplateService.getPerformanceResultTemplate(id);
        // 2. 拼接数据
        return success(buildResultTemplateRespVO(template));
    }

    @GetMapping("/page")
    @Operation(summary = "获得绩效结果模板分页")
    @SaCheckPermission("hrm:performance:result-template:query")
    public Result<PageResult<HrmPerformanceResultTemplateRespVO>> getPerformanceResultTemplatePage(
            @Validated HrmPerformanceResultTemplatePageReqVO pageReqVO) {
        PageResult<HrmPerformanceResultTemplateDO> pageResult =
                resultTemplateService.getPerformanceResultTemplatePage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }
        // 2. 拼接数据
        return success(new PageResult<>(buildResultTemplateRespVOList(pageResult.getList()),
                pageResult.getTotal()));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得绩效结果模板精简列表")
    @Parameter(name = "status", description = "状态", example = "0")
    @SaCheckPermission("hrm:performance:result-template:query")
    public Result<List<HrmPerformanceResultTemplateRespVO>> getPerformanceResultTemplateSimpleList(
            @RequestParam(value = "status", required = false) Integer status) {
        List<HrmPerformanceResultTemplateDO> templates =
                resultTemplateService.getPerformanceResultTemplateList(status);
        return success(BeanUtils.toBean(templates, HrmPerformanceResultTemplateRespVO.class));
    }

    // ==================== 拼接 VO ====================

    private HrmPerformanceResultTemplateRespVO buildResultTemplateRespVO(
            HrmPerformanceResultTemplateDO template) {
        if (template == null) {
            return null;
        }
        return CollUtil.getFirst(buildResultTemplateRespVOList(singletonList(template)));
    }

    private List<HrmPerformanceResultTemplateRespVO> buildResultTemplateRespVOList(
            List<HrmPerformanceResultTemplateDO> templates) {
        if (CollUtil.isEmpty(templates)) {
            return emptyList();
        }
        // 1. 获得创建人信息
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(templates, template -> NumberUtils.parseLong(template.getCreator())));
        // 2. 拼接响应
        return BeanUtils.toBean(templates, HrmPerformanceResultTemplateRespVO.class, template ->
                MapUtils.findAndThen(userMap, NumberUtils.parseLong(template.getCreator()),
                        user -> template.setCreatorName(user.getNickname())));
    }

}
