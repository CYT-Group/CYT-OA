package com.cyt.center.module.portal.controller.app.hrdoc;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.portal.controller.app.hrdoc.vo.PortalHrDocumentEntryRespVO;
import com.cyt.center.module.portal.controller.app.hrdoc.vo.PortalHrDocumentPublishedListItemVO;
import com.cyt.center.module.portal.controller.app.hrdoc.vo.PortalHrDocumentPublishedRespVO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDO;
import com.cyt.center.module.portal.service.hrdoc.PortalHrDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * HR 文档读侧接口（登录即可，无能力位要求；SOP / 部门可见性由服务层逐篇过滤）。
 *
 * 为什么单开一套而不是让门户直接调 /admin-api：管理令牌要求 device=admin，门户的登录人拿的是普通令牌，
 * 读侧必须走 /api/**（口径 docs/Portal管理面迁移契约.md §二）。可见性规则跟管理面同一条用例。
 *
 * 门户切流后的路径对应：
 *   /api/portal/hrdoc/published      ← Portal 的 /articles/published
 *   /api/portal/hrdoc/{id}           ← Portal 的 /articles/{id}
 *   /api/portal/hrdoc/entries        ← Portal 的 /article-entries
 *   /api/portal/hrdoc/entries/{key}  ← Portal 的 /article-entries/{entry_key}
 *   /api/portal/hrdoc/by-slug/{slug} ← Portal 的 /hr-documents/{slug}（旧链接兼容）
 */
@Tag(name = "门户 - HR 文档（读）")
@RestController
@RequestMapping("/api/portal/hrdoc")
@Validated
@RequiredArgsConstructor
public class PortalHrDocumentOpenController {

    private final PortalHrDocumentService hrdocService;

    @GetMapping("/published")
    @Operation(summary = "员工可见的已发布文档摘要（逐篇按部门可见范围过滤）")
    public Result<List<PortalHrDocumentPublishedListItemVO>> published(
            @RequestParam(value = "category", required = false) String category) {
        List<PortalHrDocumentPublishedListItemVO> list = hrdocService.listPublished(getLoginUserId(), category)
                .stream().map(this::toListItem).toList();
        return success(list);
    }

    @GetMapping("/entries")
    @Operation(summary = "一次返回当前员工可见的固定入口映射（entry_key → 文档摘要，无权项静默剔除）")
    public Result<java.util.Map<String, PortalHrDocumentEntryRespVO>> entries() {
        java.util.Map<String, PortalHrDocumentEntryRespVO> map = new java.util.LinkedHashMap<>();
        for (PortalHrDocumentService.EntryItem e : hrdocService.listEntries(getLoginUserId())) {
            map.put(e.entryKey(), toEntryVO(e.entryKey(), e.doc()));
        }
        return success(map);
    }

    @GetMapping("/entries/{entryKey}")
    @Operation(summary = "解析固定入口到已发布文档；无权 403，未绑定/未发布 404")
    public Result<PortalHrDocumentPublishedRespVO> entry(@PathVariable("entryKey") String entryKey) {
        return success(toDetailVO(hrdocService.getEntry(getLoginUserId(), entryKey)));
    }

    @GetMapping("/by-slug/{slug}")
    @Operation(summary = "按旧 slug 解析已发布文档（旧链接兼容）")
    public Result<PortalHrDocumentPublishedRespVO> bySlug(@PathVariable("slug") String slug) {
        return success(toDetailVO(hrdocService.getPublishedBySlug(getLoginUserId(), slug)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "员工按稳定 ID 读取已发布文档（只给已发布快照，不含草稿）")
    @Parameter(name = "id", description = "文档编号", required = true)
    public Result<PortalHrDocumentPublishedRespVO> get(@PathVariable("id") Long id) {
        return success(toDetailVO(hrdocService.getPublished(getLoginUserId(), id)));
    }

    // ══════════════ 序列化 ══════════════

    private PortalHrDocumentPublishedRespVO toDetailVO(PortalHrDocumentDO doc) {
        PortalHrDocumentPublishedRespVO vo = BeanUtils.toBean(doc, PortalHrDocumentPublishedRespVO.class);
        vo.setCanonicalUrl("/article/" + doc.getId());
        vo.setLegacySlug(doc.getSlug());
        vo.setTopic(doc.getTopic() != null ? doc.getTopic() : "admin");
        return vo;
    }

    private PortalHrDocumentPublishedListItemVO toListItem(PortalHrDocumentDO doc) {
        PortalHrDocumentPublishedListItemVO vo = new PortalHrDocumentPublishedListItemVO();
        vo.setId(doc.getId());
        vo.setCanonicalUrl("/article/" + doc.getId());
        vo.setLegacySlug(doc.getSlug());
        vo.setTitle(doc.getTitle());
        vo.setCategory(doc.getCategory());
        vo.setTopic(doc.getTopic() != null ? doc.getTopic() : "admin");
        vo.setIntro(doc.getIntro());
        vo.setRequiresSignoff(doc.getRequiresSignoff());
        vo.setUpdatedAt(doc.getUpdatedAt());
        vo.setPublishedAt(doc.getPublishedAt());
        vo.setTheme(doc.getTheme());
        vo.setCover(doc.getCover());
        return vo;
    }

    private PortalHrDocumentEntryRespVO toEntryVO(String entryKey, PortalHrDocumentDO doc) {
        PortalHrDocumentEntryRespVO vo = new PortalHrDocumentEntryRespVO();
        vo.setEntryKey(entryKey);
        vo.setId(doc.getId());
        vo.setCanonicalUrl("/article/" + doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setCategory(doc.getCategory());
        vo.setIntro(doc.getIntro());
        vo.setRequiresSignoff(doc.getRequiresSignoff());
        vo.setLegacySlug(doc.getSlug());
        return vo;
    }

}
