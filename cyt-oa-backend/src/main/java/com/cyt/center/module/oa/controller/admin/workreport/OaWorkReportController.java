package com.cyt.center.module.oa.controller.admin.workreport;

import cn.dev33.satoken.annotation.SaMode;

import cn.dev33.satoken.annotation.SaCheckPermission;

import cn.hutool.core.collection.CollUtil;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.collection.MapUtils;
import com.cyt.center.framework.common.util.number.NumberUtils;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.oa.controller.admin.workreport.vo.OaWorkReportPageReqVO;
import com.cyt.center.module.oa.controller.admin.workreport.vo.OaWorkReportRespVO;
import com.cyt.center.module.oa.controller.admin.workreport.vo.OaWorkReportSaveReqVO;
import com.cyt.center.module.oa.controller.admin.workreport.vo.OaWorkReportStatisticsReqVO;
import com.cyt.center.module.oa.controller.admin.workreport.vo.OaWorkReportStatisticsRespVO;
import com.cyt.center.module.oa.dal.dataobject.workreport.OaWorkReportDO;
import com.cyt.center.module.oa.service.workreport.OaWorkReportService;
import com.cyt.center.module.oa.service.workreport.OaWorkReportPdfExporter;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cyt.center.module.oa.enums.workreport.OaWorkReportTypeEnum;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertList;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertSet;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - OA 工作汇报")
@RestController
@RequestMapping("/oa/work-report")
@Validated
public class OaWorkReportController {

    @Resource
    private OaWorkReportService workReportService;

    @Resource
    private OaWorkReportPdfExporter workReportPdfExporter;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    // ==================== 我的汇报 ====================

    @PostMapping("/create")
    @Operation(summary = "创建工作汇报草稿")
    @SaCheckPermission("oa:work-report:create")
    public Result<Long> createWorkReport(@Valid @RequestBody OaWorkReportSaveReqVO createReqVO) {
        return success(workReportService.createWorkReport(createReqVO, getLoginUserId()));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工作汇报草稿")
    @SaCheckPermission("oa:work-report:update")
    public Result<Boolean> updateWorkReport(@Valid @RequestBody OaWorkReportSaveReqVO updateReqVO) {
        workReportService.updateWorkReport(updateReqVO, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工作汇报草稿")
    @Parameter(name = "id", description = "汇报编号", required = true, example = "1024")
    @SaCheckPermission("oa:work-report:delete")
    public Result<Boolean> deleteWorkReport(@RequestParam("id") Long id) {
        workReportService.deleteWorkReport(id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交工作汇报")
    @Parameter(name = "id", description = "汇报编号", required = true, example = "1024")
    @SaCheckPermission("oa:work-report:update")
    public Result<Boolean> submitWorkReport(@RequestParam("id") Long id) {
        workReportService.submitWorkReport(id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消提交工作汇报")
    @Parameter(name = "id", description = "汇报编号", required = true, example = "1024")
    @SaCheckPermission("oa:work-report:update")
    public Result<Boolean> cancelWorkReport(@RequestParam("id") Long id) {
        workReportService.cancelWorkReport(id, getLoginUserId());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工作汇报详情")
    @Parameter(name = "id", description = "汇报编号", required = true, example = "1024")
    @SaCheckPermission(value = {"oa:work-report:query", "oa:work-report:statistics"}, mode = SaMode.OR)
    public Result<OaWorkReportRespVO> getWorkReport(@RequestParam("id") Long id) {
        OaWorkReportDO workReport = workReportService.getWorkReport(id, getLoginUserId());
        return success(buildWorkReportRespVO(workReport));
    }

    @GetMapping("/page")
    @Operation(summary = "获得我的工作汇报分页")
    @SaCheckPermission("oa:work-report:query")
    public Result<PageResult<OaWorkReportRespVO>> getWorkReportPage(
            @Valid OaWorkReportPageReqVO pageReqVO) {
        PageResult<OaWorkReportDO> pageResult = workReportService.getWorkReportPage(pageReqVO, getLoginUserId());
        return success(new PageResult<>(buildWorkReportRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/export")
    @Operation(summary = "导出工作汇报 PDF")
    @Parameter(name = "id", description = "汇报编号", required = true, example = "1024")
    @SaCheckPermission(value = {"oa:work-report:query", "oa:work-report:statistics"}, mode = SaMode.OR)
    public ResponseEntity<byte[]> exportWorkReport(@RequestParam("id") Long id) {
        OaWorkReportDO workReport = workReportService.getWorkReport(id, getLoginUserId());
        OaWorkReportRespVO respVO = buildWorkReportRespVO(workReport);
        byte[] pdf = workReportPdfExporter.export(respVO);
        String filename = "工作汇报-" + respVO.getNo() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ==================== 汇报统计 ====================

    @GetMapping("/statistics")
    @Operation(summary = "获得工作汇报统计")
    @SaCheckPermission("oa:work-report:statistics")
    public Result<OaWorkReportStatisticsRespVO> getWorkReportStatistics(
            @Valid OaWorkReportStatisticsReqVO reqVO) {
        return success(workReportService.getWorkReportStatistics(reqVO, getLoginUserId()));
    }

    // ==================== 拼接 VO ====================

    /**
     * 拼接工作汇报详情
     *
     * @param workReport 工作汇报
     * @return 工作汇报响应
     */
    private OaWorkReportRespVO buildWorkReportRespVO(OaWorkReportDO workReport) {
        if (workReport == null) {
            return null;
        }
        return CollUtil.getFirst(buildWorkReportRespVOList(Collections.singletonList(workReport)));
    }

    /**
     * 拼接工作汇报响应列表
     *
     * @param workReports 工作汇报列表
     * @return 工作汇报响应列表
     */
    private List<OaWorkReportRespVO> buildWorkReportRespVOList(List<OaWorkReportDO> workReports) {
        if (CollUtil.isEmpty(workReports)) {
            return Collections.emptyList();
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(workReports, report -> NumberUtils.parseLong(report.getCreator())));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSet(workReports, OaWorkReportDO::getDeptId));
        return convertList(workReports, workReport -> {
            Long userId = NumberUtils.parseLong(workReport.getCreator());
            OaWorkReportRespVO respVO = BeanUtils.toBean(workReport, OaWorkReportRespVO.class).setUserId(userId)
                    .setPeriodKey(OaWorkReportTypeEnum.valueOf(workReport.getType()).formatPeriod(workReport.getStartTime()));
            MapUtils.findAndThen(userMap, userId, user -> respVO.setUserName(user.getNickname()));
            MapUtils.findAndThen(deptMap, workReport.getDeptId(), dept -> respVO.setDeptName(dept.getName()));
            return respVO;
        });
    }

}
