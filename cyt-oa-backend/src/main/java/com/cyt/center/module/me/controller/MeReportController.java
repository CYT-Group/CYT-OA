package com.cyt.center.module.me.controller;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.module.me.model.cmd.MeCmds.CommentCmd;
import com.cyt.center.module.me.model.cmd.MeCmds.CommentReplyCmd;
import com.cyt.center.module.me.model.cmd.MeCmds.ReportCmd;
import com.cyt.center.module.me.model.vo.MeVos.Report;
import com.cyt.center.module.me.model.vo.MeVos.ReportComment;
import com.cyt.center.module.me.model.vo.MeVos.TrackCell;
import com.cyt.center.module.me.service.MeReportService;
import com.cyt.center.module.oa.controller.admin.workreport.vo.OaWorkReportRespVO;
import com.cyt.center.module.oa.service.workreport.OaWorkReportPdfExporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "个人中心 - 我的报告")
@RestController
@RequestMapping("/api/me/reports")
public class MeReportController {

    @Resource
    private MeReportService meReportService;
    @Resource
    private OaWorkReportPdfExporter pdfExporter;

    @GetMapping
    @Operation(summary = "汇报分页，只含本人的")
    public Result<PageResult<Report>> page(@RequestParam(value = "type", required = false) Integer type,
                                           @RequestParam(value = "status", required = false) Integer status,
                                           @Valid PageParam pageParam) {
        return success(meReportService.page(getLoginUserId(), type, status, pageParam));
    }

    @GetMapping("/drafts")
    @Operation(summary = "未提交的草稿汇报")
    public Result<List<Report>> drafts() {
        return success(meReportService.drafts(getLoginUserId()));
    }

    @GetMapping("/track")
    @Operation(summary = "按周提交轨迹，state 为 submitted / draft / missing")
    public Result<List<TrackCell>> track(@RequestParam(value = "type", required = false) Integer type,
                                         @RequestParam(value = "weeks", defaultValue = "12") int weeks) {
        return success(meReportService.track(getLoginUserId(), type, weeks));
    }

    @GetMapping("/{id}")
    @Operation(summary = "汇报详情，含点评与我的回复")
    public Result<Report> detail(@PathVariable("id") Long id) {
        return success(meReportService.detail(getLoginUserId(), id));
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "某份汇报下的点评")
    public Result<List<ReportComment>> comments(@PathVariable("id") Long id) {
        return success(meReportService.commentsOf(id));
    }

    @PostMapping("/draft")
    @Operation(summary = "保存草稿，同周期已有草稿则覆盖；周期由汇报类型推导")
    public Result<Long> saveDraft(@Valid @RequestBody ReportCmd cmd) {
        return success(meReportService.saveDraft(getLoginUserId(), cmd));
    }

    @PutMapping("/{id}/submit")
    @Operation(summary = "提交汇报")
    public Result<Boolean> submit(@PathVariable("id") Long id) {
        meReportService.submit(getLoginUserId(), id);
        return success(true);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "撤回已提交的汇报")
    public Result<Boolean> cancel(@PathVariable("id") Long id) {
        meReportService.cancel(getLoginUserId(), id);
        return success(true);
    }

    @PostMapping("/{id}/comment")
    @Operation(summary = "上级点评团队成员的汇报")
    public Result<Long> comment(@PathVariable("id") Long id, @Valid @RequestBody CommentCmd cmd) {
        cmd.setReportId(id);
        return success(meReportService.comment(getLoginUserId(), cmd));
    }

    @PutMapping("/comments/{commentId}/reply")
    @Operation(summary = "回复上级点评，仅汇报作者可操作")
    public Result<Boolean> reply(@PathVariable("commentId") Long commentId,
                                 @Valid @RequestBody CommentReplyCmd cmd) {
        cmd.setCommentId(commentId);
        meReportService.reply(getLoginUserId(), cmd);
        return success(true);
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "导出汇报 PDF")
    public ResponseEntity<byte[]> export(@PathVariable("id") Long id) {
        OaWorkReportRespVO respVO = meReportService.exportView(getLoginUserId(), id);
        byte[] pdf = pdfExporter.export(respVO);
        String filename = "工作汇报-" + respVO.getNo() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
