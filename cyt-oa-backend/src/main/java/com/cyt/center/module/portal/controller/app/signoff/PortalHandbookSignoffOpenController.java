package com.cyt.center.module.portal.controller.app.signoff;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.servlet.ServletUtils;
import com.cyt.center.module.portal.controller.admin.signoff.vo.PortalSignoffRespVO;
import com.cyt.center.module.portal.controller.admin.signoff.vo.PortalSignoffSignReqVO;
import com.cyt.center.module.portal.dal.dataobject.signoff.PortalHandbookSignoffDO;
import com.cyt.center.module.portal.service.signoff.PortalHandbookSignoffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 制度签收读侧/写入口（员工登录即可签收，无能力位要求）。
 *
 * 签收（sign）+ 我的签收（my-signoffs）走 Open API；签收统计在管理面（can_view_signoff_stats）。
 */
@Tag(name = "门户 - 制度签收")
@RestController
@RequestMapping("/api/portal/handbook")
@Validated
@RequiredArgsConstructor
public class PortalHandbookSignoffOpenController {

    private final PortalHandbookSignoffService signoffService;

    @PostMapping("/sign")
    @Operation(summary = "员工线上签收指定文档版本（幂等，重复签不报错）")
    public Result<PortalSignoffRespVO> sign(@Valid @RequestBody PortalSignoffSignReqVO reqVO,
                                           HttpServletRequest request) {
        PortalHandbookSignoffDO record = signoffService.sign(getLoginUserId(), reqVO,
                ServletUtils.getClientIP(), ServletUtils.getUserAgent());
        return success(toVO(record));
    }

    @GetMapping("/my-signoffs")
    @Operation(summary = "我的签收记录（附各文档当前版本，前端对比判断是否需要重新签收）")
    public Result<List<PortalSignoffRespVO>> mySignoffs() {
        PortalHandbookSignoffService.MySignoffs r = signoffService.mySignoffs(getLoginUserId());
        return success(r.items().stream().map(this::toVO).toList());
    }

    private PortalSignoffRespVO toVO(PortalHandbookSignoffDO record) {
        PortalSignoffRespVO vo = new PortalSignoffRespVO();
        vo.setId(record.getId());
        vo.setDocument(record.getDocument());
        vo.setDocumentId(record.getDocumentId());
        vo.setVersion(record.getVersion());
        vo.setSignedAt(record.getSignedAt());
        return vo;
    }

}
