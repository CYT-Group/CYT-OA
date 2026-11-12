package com.cyt.center.module.portal.controller.admin.hrdoc;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.portal.controller.admin.hrdoc.vo.PortalHrDocumentDraftVersionRespVO;
import com.cyt.center.module.portal.controller.admin.hrdoc.vo.PortalHrDocumentListItemVO;
import com.cyt.center.module.portal.controller.admin.hrdoc.vo.PortalHrDocumentOfflineReqVO;
import com.cyt.center.module.portal.controller.admin.hrdoc.vo.PortalHrDocumentPageReqVO;
import com.cyt.center.module.portal.controller.admin.hrdoc.vo.PortalHrDocumentPublishReqVO;
import com.cyt.center.module.portal.controller.admin.hrdoc.vo.PortalHrDocumentRespVO;
import com.cyt.center.module.portal.controller.admin.hrdoc.vo.PortalHrDocumentSaveReqVO;
import com.cyt.center.module.portal.controller.admin.hrdoc.vo.PortalHrDocumentSlugCheckRespVO;
import com.cyt.center.module.portal.controller.admin.hrdoc.vo.PortalHrDocumentUpdateReqVO;
import com.cyt.center.module.portal.controller.admin.hrdoc.vo.PortalHrDocumentVersionRespVO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDraftVersionDO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentVersionDO;
import com.cyt.center.module.portal.domain.PortalCapability;
import com.cyt.center.module.portal.service.hrdoc.PortalHrDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * HR 文档 CMS 管理接口层（迁自 Portal 的 routers/hr_documents.py 的 /articles/admin/* 一段）。
 *
 * 全部管理端点 require(can_edit_hr_documents)，与旧 Portal 相同；员工公开阅读走 app 接口。
 */
@Tag(name = "管理后台 - HR 文档 CMS")
@RestController
@RequestMapping("/portal/hrdoc")
@Validated
@RequiredArgsConstructor
@SaCheckPermission(PortalCapability.EDIT_HR_DOCUMENTS)
public class PortalHrDocumentController {
    private final com.cyt.center.module.portal.dal.postgresql.hrdoc.PortalHrEditorRepository editorRepository;
    private final com.cyt.center.module.portal.service.push.PortalContentPushService contentPush;

    public record PushRequest(String audience, java.util.List<String> notifyDeptScope,
            java.util.List<String> userIds, boolean force,
            @jakarta.validation.constraints.Size(max=300) String customMessage) {}

    @PostMapping("/notify")
    @SaCheckPermission(PortalCapability.EDIT_HR_DOCUMENTS)
    public Result<java.util.Map<String,Object>> notify(@RequestParam("id") Long id,
            @jakarta.validation.Valid @RequestBody PushRequest request) {
        return success(contentPush.document(getLoginUserId(), id, request.audience(),
                request.notifyDeptScope(), request.userIds(), request.force(), request.customMessage(), false));
    }

    @PostMapping("/test-push-to-me")
    @SaCheckPermission(PortalCapability.EDIT_HR_DOCUMENTS)
    public Result<java.util.Map<String,Object>> testPush(@RequestParam("id") Long id) {
        return success(contentPush.document(getLoginUserId(), id, "users", null, null, true, null, true));
    }

    private final PortalHrDocumentService hrdocService;

    @GetMapping("/resolve")
    @Operation(summary = "按历史别名或当前 slug 解析稳定文档 ID（管理权限）")
    public Result<java.util.Map<String,Long>> resolve(@RequestParam String slug) {
        return success(java.util.Map.of("id",hrdocService.resolveAdminSlug(slug)));
    }

    @GetMapping("/page")
    @Operation(summary = "分页列文档（含草稿）；deleted=true 切回收站；status 支持伪状态 offline")
    public Result<PageResult<PortalHrDocumentListItemVO>> page(@Valid PortalHrDocumentPageReqVO pageReqVO) {
        PageResult<PortalHrDocumentDO> rows = hrdocService.page(pageReqVO, pageReqVO.getCategory(),
                pageReqVO.getStatus(), Boolean.TRUE.equals(pageReqVO.getDeleted()), pageReqVO.getQ());
        return success(new PageResult<>(rows.getList().stream().map(this::toListItem).toList(), rows.getTotal()));
    }

    @GetMapping("/get")
    @Operation(summary = "单篇详情（含草稿与发布确认凭据 revision）")
    @Parameter(name = "id", description = "文档编号", required = true)
    public Result<PortalHrDocumentRespVO> get(@RequestParam("id") Long id) {
        PortalHrDocumentService.Detail detail = hrdocService.getDetail(id);
        return success(toDetailVO(detail.doc()));
    }

    @PostMapping("/create")
    @Operation(summary = "新建文档（进草稿，ID-first 不接收 slug）")
    @SaCheckPermission(PortalCapability.EDIT_HR_DOCUMENTS)
    public Result<PortalHrDocumentRespVO> create(@Valid @RequestBody PortalHrDocumentSaveReqVO reqVO) {
        PortalHrDocumentDO doc = hrdocService.create(getLoginUserId(), reqVO);
        return success(toDetailVO(doc));
    }

    @PutMapping("/update")
    @Operation(summary = "PATCH 语义保存草稿（只动非 null 字段；写 draft_* 不影响已发布快照）")
    @SaCheckPermission(PortalCapability.EDIT_HR_DOCUMENTS)
    public Result<PortalHrDocumentRespVO> update(@Valid @RequestBody PortalHrDocumentUpdateReqVO reqVO) {
        PortalHrDocumentDO doc = hrdocService.update(getLoginUserId(), reqVO.getId(), reqVO);
        return success(toDetailVO(doc));
    }

    @PostMapping("/publish")
    @Operation(summary = "发布（草稿晋升为正式列并写版本快照；仅显式勾选才推送企微）")
    @SaCheckPermission(PortalCapability.EDIT_HR_DOCUMENTS)
    public Result<PortalHrDocumentRespVO> publish(@RequestParam("id") Long id,
                                                  @Valid @RequestBody PortalHrDocumentPublishReqVO reqVO) {
        PortalHrDocumentDO doc = hrdocService.publish(getLoginUserId(), id, reqVO.getExpectedRevision(),
                reqVO.getVersion());
        if (Boolean.TRUE.equals(reqVO.getNotifyWecom())) contentPush.document(getLoginUserId(), id, "scope",
                reqVO.getNotifyDeptScope(), null, false, null, false);
        return success(toDetailVO(doc));
    }

    @PostMapping("/offline")
    @Operation(summary = "下架（员工端立即不可见；内容/版本/签收记录保留）")
    @SaCheckPermission(PortalCapability.EDIT_HR_DOCUMENTS)
    public Result<PortalHrDocumentRespVO> offline(@RequestParam("id") Long id,
                                                  @Valid @RequestBody(required = false) PortalHrDocumentOfflineReqVO reqVO) {
        PortalHrDocumentDO doc = hrdocService.offline(getLoginUserId(), id, reqVO == null ? null : reqVO.getReason());
        return success(toDetailVO(doc));
    }

    @PostMapping("/restore")
    @Operation(summary = "重新上架（只清下架标记，不升版本不写快照）")
    @SaCheckPermission(PortalCapability.EDIT_HR_DOCUMENTS)
    public Result<PortalHrDocumentRespVO> restore(@RequestParam("id") Long id) {
        PortalHrDocumentDO doc = hrdocService.restore(getLoginUserId(), id);
        return success(toDetailVO(doc));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "逻辑删除进回收站（已发布未下架的须先下架）")
    @SaCheckPermission(PortalCapability.EDIT_HR_DOCUMENTS)
    public Result<PortalHrDocumentRespVO> delete(@RequestParam("id") Long id) {
        PortalHrDocumentDO doc = hrdocService.delete(getLoginUserId(), id);
        return success(toDetailVO(doc));
    }

    @PostMapping("/undelete")
    @Operation(summary = "从回收站恢复（只清删除标记）")
    @SaCheckPermission(PortalCapability.EDIT_HR_DOCUMENTS)
    public Result<PortalHrDocumentRespVO> undelete(@RequestParam("id") Long id) {
        PortalHrDocumentDO doc = hrdocService.undelete(getLoginUserId(), id);
        return success(toDetailVO(doc));
    }

    @GetMapping("/versions")
    @Operation(summary = "发布版本快照列表（新的在前，不含正文）")
    public Result<List<PortalHrDocumentVersionRespVO>> versions(@RequestParam("id") Long id) {
        return success(hrdocService.listVersions(id).stream()
                .map(v -> toVersionVO(v, false)).toList());
    }

    @GetMapping("/versions/{version}")
    @Operation(summary = "单个发布版本详情（含正文）")
    public Result<PortalHrDocumentVersionRespVO> version(@RequestParam("id") Long id,
                                                         @PathVariable("version") String version) {
        return success(toVersionVO(hrdocService.getVersion(id, version), true));
    }

    @GetMapping("/draft-versions")
    @Operation(summary = "草稿版本快照列表（新的在前，不含正文）")
    public Result<List<PortalHrDocumentDraftVersionRespVO>> draftVersions(@RequestParam("id") Long id) {
        return success(hrdocService.listDraftVersions(id).stream()
                .map(v -> toDraftVersionVO(v, false)).toList());
    }

    @GetMapping("/draft-versions/{versionId}")
    @Operation(summary = "单个草稿版本详情（含正文）")
    public Result<PortalHrDocumentDraftVersionRespVO> draftVersion(@RequestParam("id") Long id,
                                                                   @PathVariable("versionId") Long versionId) {
        return success(toDraftVersionVO(hrdocService.getDraftVersion(id, versionId), true));
    }

    @DeleteMapping("/draft-versions/{versionId}")
    @Operation(summary = "删除单个草稿版本快照")
    @SaCheckPermission(PortalCapability.EDIT_HR_DOCUMENTS)
    public Result<Boolean> deleteDraftVersion(@RequestParam("id") Long id,
                                              @PathVariable("versionId") Long versionId) {
        hrdocService.deleteDraftVersion(getLoginUserId(), id, versionId);
        return success(true);
    }

    @GetMapping("/slug-available")
    @Operation(summary = "slug 实时占用检查（编辑器输入框边打边查）")
    public Result<PortalHrDocumentSlugCheckRespVO> slugAvailable(
            @RequestParam("slug") String slug,
            @RequestParam(value = "exclude", required = false) String exclude) {
        PortalHrDocumentService.SlugCheck check = hrdocService.checkSlug(slug, exclude);
        PortalHrDocumentSlugCheckRespVO vo = new PortalHrDocumentSlugCheckRespVO();
        vo.setAvailable(check.available());
        vo.setReason(check.reason());
        vo.setMessage(check.message());
        vo.setOwner(check.owner());
        return success(vo);
    }

    // ══════════════ 序列化 ══════════════

    private PortalHrDocumentRespVO toDetailVO(PortalHrDocumentDO doc) {
        var detail=hrdocService.describe(doc);
        var vo=BeanUtils.toBean(doc,PortalHrDocumentRespVO.class);
        vo.setCanonicalUrl("/article/"+doc.getId());
        vo.setLegacySlug(doc.getSlug());
        vo.setRevision(detail.revision());vo.setPublicationState(detail.publicationState());
        String key=(doc.getSlug()==null?"article:"+doc.getId():doc.getSlug())+":"+doc.getVersion();
        vo.setLastPush(editorRepository.lastPush(key));
        return vo;
    }

    private PortalHrDocumentListItemVO toListItem(PortalHrDocumentDO doc) {
        PortalHrDocumentListItemVO vo = new PortalHrDocumentListItemVO();
        vo.setId(doc.getId());
        vo.setCanonicalUrl("/article/" + doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setRequiresSignoff(doc.getRequiresSignoff());
        vo.setDeptScope(doc.getDeptScope());
        vo.setVersion(doc.getVersion());
        vo.setDraftTitle(doc.getDraftTitle());
        vo.setDraftRequiresSignoff(doc.getDraftRequiresSignoff());
        vo.setDraftDeptScope(doc.getDraftDeptScope());
        vo.setDraftVersion(doc.getDraftVersion());
        vo.setCategory(doc.getCategory());
        vo.setTopic(doc.getTopic());
        vo.setStatus(doc.getStatus());
        vo.setUpdatedBy(doc.getUpdatedBy());
        vo.setUpdatedAt(doc.getUpdatedAt());
        vo.setSubmittedBy(doc.getSubmittedBy());
        vo.setSubmittedAt(doc.getSubmittedAt());
        vo.setPublishedBy(doc.getPublishedBy());
        vo.setPublishedAt(doc.getPublishedAt());
        vo.setOfflineAt(doc.getOfflineAt());
        vo.setOfflineBy(doc.getOfflineBy());
        vo.setOfflineReason(doc.getOfflineReason());
        vo.setDeletedAt(doc.getDeletedAt());
        vo.setDeletedBy(doc.getDeletedBy());
        vo.setLegacySlug(doc.getSlug());
        return vo;
    }

    private PortalHrDocumentVersionRespVO toVersionVO(PortalHrDocumentVersionDO v, boolean includeContent) {
        PortalHrDocumentVersionRespVO vo = new PortalHrDocumentVersionRespVO();
        vo.setId(v.getId());
        vo.setDocumentId(v.getDocumentId());
        vo.setVersion(v.getVersion());
        vo.setTitle(v.getTitle());
        vo.setPublishedAt(v.getPublishedAt());
        vo.setPublishedBy(v.getPublishedBy());
        vo.setApprovedBy(v.getApprovedBy());
        vo.setSubmittedBy(v.getSubmittedBy());
        vo.setLegacySlug(v.getSlug());
        if (includeContent) {
            vo.setContentHtml(v.getContentHtml());
            vo.setContentBlocks(v.getContentBlocks());
        }
        return vo;
    }

    private PortalHrDocumentDraftVersionRespVO toDraftVersionVO(PortalHrDocumentDraftVersionDO v, boolean includeContent) {
        PortalHrDocumentDraftVersionRespVO vo = new PortalHrDocumentDraftVersionRespVO();
        boolean isBlocks = v.getContentBlocks() != null;
        vo.setId(v.getId());
        vo.setTitle(v.getTitle());
        vo.setSavedBy(v.getSavedBy());
        vo.setCreatedAt(v.getCreatedAt());
        vo.setFormat(isBlocks ? "blocks" : "html");
        vo.setSummary(isBlocks ? (v.getContentBlocks().size() + " 块")
                : ((v.getContentHtml() == null ? "" : v.getContentHtml()).length() + " 字"));
        if (includeContent) {
            vo.setContentHtml(v.getContentHtml());
            vo.setContentBlocks(v.getContentBlocks());
        }
        return vo;
    }

}
