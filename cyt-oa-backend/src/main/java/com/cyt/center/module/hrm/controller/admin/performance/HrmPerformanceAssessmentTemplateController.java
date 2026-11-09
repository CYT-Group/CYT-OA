package com.cyt.center.module.hrm.controller.admin.performance;

import cn.dev33.satoken.annotation.SaCheckPermission;

import cn.hutool.core.collection.CollUtil;
import com.cyt.center.framework.common.enums.CommonStatusEnum;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.number.NumberUtils;
import com.cyt.center.framework.common.util.collection.MapUtils;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.performance.vo.assessmenttemplate.HrmPerformanceAssessmentTemplatePageReqVO;
import com.cyt.center.module.hrm.controller.admin.performance.vo.assessmenttemplate.HrmPerformanceAssessmentTemplateRespVO;
import com.cyt.center.module.hrm.controller.admin.performance.vo.assessmenttemplate.HrmPerformanceAssessmentTemplateSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.performance.config.HrmPerformanceAssessmentTemplateDO;
import com.cyt.center.module.hrm.service.performance.config.HrmPerformanceAssessmentTemplateService;
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

@Tag(name = "管理后台 - HRM 绩效考核模板")
@RestController
@RequestMapping("/hrm/performance/assessment-template")
@Validated
public class HrmPerformanceAssessmentTemplateController {

    @Resource
    private HrmPerformanceAssessmentTemplateService assessmentTemplateService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建绩效考核模板")
    @SaCheckPermission("hrm:performance:assessment-template:create")
    public Result<Long> createPerformanceAssessmentTemplate(
            @Valid @RequestBody HrmPerformanceAssessmentTemplateSaveReqVO createReqVO) {
        return success(assessmentTemplateService.createPerformanceAssessmentTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新绩效考核模板")
    @SaCheckPermission("hrm:performance:assessment-template:update")
    public Result<Boolean> updatePerformanceAssessmentTemplate(
            @Valid @RequestBody HrmPerformanceAssessmentTemplateSaveReqVO updateReqVO) {
        assessmentTemplateService.updatePerformanceAssessmentTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除绩效考核模板")
    @Parameter(name = "id", description = "模板编号", required = true, example = "1024")
    @SaCheckPermission("hrm:performance:assessment-template:delete")
    public Result<Boolean> deletePerformanceAssessmentTemplate(@RequestParam("id") Long id) {
        assessmentTemplateService.deletePerformanceAssessmentTemplate(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除绩效考核模板")
    @Parameter(name = "ids", description = "模板编号列表", required = true, example = "1024,1025")
    @SaCheckPermission("hrm:performance:assessment-template:delete")
    public Result<Boolean> deletePerformanceAssessmentTemplateList(@RequestParam("ids") List<Long> ids) {
        assessmentTemplateService.deletePerformanceAssessmentTemplateList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得绩效考核模板")
    @Parameter(name = "id", description = "模板编号", required = true, example = "1024")
    @SaCheckPermission("hrm:performance:assessment-template:query")
    public Result<HrmPerformanceAssessmentTemplateRespVO> getPerformanceAssessmentTemplate(
            @RequestParam("id") Long id) {
        // 1. 获得考核模板
        HrmPerformanceAssessmentTemplateDO template =
                assessmentTemplateService.getPerformanceAssessmentTemplate(id);
        // 2. 拼接数据
        return success(buildAssessmentTemplateRespVO(template));
    }

    @GetMapping("/page")
    @Operation(summary = "获得绩效考核模板分页")
    @SaCheckPermission("hrm:performance:assessment-template:query")
    public Result<PageResult<HrmPerformanceAssessmentTemplateRespVO>> getPerformanceAssessmentTemplatePage(
            @Validated HrmPerformanceAssessmentTemplatePageReqVO pageReqVO) {
        PageResult<HrmPerformanceAssessmentTemplateDO> pageResult =
                assessmentTemplateService.getPerformanceAssessmentTemplatePage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }
        // 2. 拼接数据
        return success(new PageResult<>(buildAssessmentTemplateRespVOList(pageResult.getList()),
                pageResult.getTotal()));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得绩效考核模板精简列表")
    @SaCheckPermission("hrm:performance:assessment-template:query")
    public Result<List<HrmPerformanceAssessmentTemplateRespVO>> getPerformanceAssessmentTemplateSimpleList() {
        List<HrmPerformanceAssessmentTemplateDO> templates = assessmentTemplateService
                .getPerformanceAssessmentTemplateListByStatus(CommonStatusEnum.ENABLE.getStatus());
        return success(BeanUtils.toBean(templates, HrmPerformanceAssessmentTemplateRespVO.class));
    }

    // ==================== 拼接 VO ====================

    private HrmPerformanceAssessmentTemplateRespVO buildAssessmentTemplateRespVO(
            HrmPerformanceAssessmentTemplateDO template) {
        if (template == null) {
            return null;
        }
        return CollUtil.getFirst(buildAssessmentTemplateRespVOList(singletonList(template)));
    }

    private List<HrmPerformanceAssessmentTemplateRespVO> buildAssessmentTemplateRespVOList(
            List<HrmPerformanceAssessmentTemplateDO> templates) {
        if (CollUtil.isEmpty(templates)) {
            return emptyList();
        }
        // 1. 获得创建人信息
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(templates, template -> NumberUtils.parseLong(template.getCreator())));
        // 2. 拼接响应
        return BeanUtils.toBean(templates, HrmPerformanceAssessmentTemplateRespVO.class, template ->
                MapUtils.findAndThen(userMap, NumberUtils.parseLong(template.getCreator()),
                        user -> template.setCreatorName(user.getNickname())));
    }

}
