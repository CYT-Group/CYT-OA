package com.cyt.center.module.portal.service.products;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.json.JsonUtils;
import com.cyt.center.module.portal.controller.admin.products.vo.PortalProductPageReqVO;
import com.cyt.center.module.portal.controller.admin.products.vo.PortalProductSaveReqVO;
import com.cyt.center.module.portal.dal.dataobject.products.PortalProductDO;
import com.cyt.center.module.portal.dal.postgresql.products.PortalProductMapper;
import com.cyt.center.module.portal.service.support.PortalAudit;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.PRODUCT_NOT_EXISTS;

/**
 * 新品动态用例实现（逐条对齐 Portal 的 application/products.py）。
 */
@Slf4j
@Service
public class PortalProductServiceImpl implements PortalProductService {

    private static final String AUDIT_TABLE = "portal_products";
    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_LIMIT = 100;

    @Resource
    private PortalProductMapper productMapper;
    @Resource
    private PortalAudit audit;

    @Override
    public PageResult<PortalProductDO> page(PortalProductPageReqVO pageReqVO) {
        return productMapper.selectPage(pageReqVO, blankToNull(pageReqVO.getBrand()),
                blankToNull(pageReqVO.getQ()));
    }

    @Override
    public PortalProductDO get(Long id) {
        PortalProductDO row = id == null ? null : productMapper.selectById(id);
        if (row == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalProductDO create(PortalProductSaveReqVO createReqVO) {
        PortalProductDO row = new PortalProductDO();
        row.setName(createReqVO.getName().trim());
        row.setBrand(blankToNull(createReqVO.getBrand()));
        row.setCategory(blankToNull(createReqVO.getCategory()));
        row.setDescription(blankToNull(createReqVO.getDescription()));
        row.setImageUrl(blankToNull(createReqVO.getImageUrl()));
        row.setLinkUrl(blankToNull(createReqVO.getLinkUrl()));
        row.setLaunchDate(createReqVO.getLaunchDate());
        row.setStatus(PortalProductDO.STATUS_PUBLISHED);
        row.setIsFeatured(Boolean.TRUE.equals(createReqVO.getIsFeatured()));
        productMapper.insert(row);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalProductDO update(Long id, PortalProductSaveReqVO updateReqVO) {
        PortalProductDO row = id == null ? null : productMapper.selectById(id);
        if (row == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
        row.setName(updateReqVO.getName().trim());
        row.setBrand(blankToNull(updateReqVO.getBrand()));
        row.setCategory(blankToNull(updateReqVO.getCategory()));
        row.setDescription(blankToNull(updateReqVO.getDescription()));
        row.setImageUrl(blankToNull(updateReqVO.getImageUrl()));
        row.setLinkUrl(blankToNull(updateReqVO.getLinkUrl()));
        row.setLaunchDate(updateReqVO.getLaunchDate());
        if (updateReqVO.getIsFeatured() != null) {
            row.setIsFeatured(updateReqVO.getIsFeatured());
        }
        productMapper.updateById(row);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        PortalProductDO row = id == null ? null : productMapper.selectById(id);
        if (row == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
        productMapper.deleteById(id);
        audit.record(userId, "delete", AUDIT_TABLE, id, snapshot(row), null, null);
    }

    @Override
    public List<PortalProductDO> listPublished(String brand, Integer limit) {
        return productMapper.selectPublished(blankToNull(brand), clampLimit(limit, DEFAULT_LIMIT));
    }

    @Override
    public PortalProductDO getPublished(Long id) {
        PortalProductDO row = id == null ? null : productMapper.selectById(id);
        // 不存在 / 未发布统一 404（Portal 的 NotFound 不暴露存在性）
        if (row == null || !PortalProductDO.STATUS_PUBLISHED.equals(row.getStatus())) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
        return row;
    }

    // ══════════════ 内部 ══════════════

    private static int clampLimit(Integer limit, int fallback) {
        if (limit == null || limit <= 0) {
            return fallback;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /** 行的审计快照：键名沿用 Portal 的 snake_case；介绍按摘要存 */
    private String snapshot(PortalProductDO row) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", row.getName());
        snapshot.put("brand", row.getBrand());
        snapshot.put("category", row.getCategory());
        snapshot.put("description", PortalAudit.summarize(row.getDescription()));
        snapshot.put("image_url", row.getImageUrl());
        snapshot.put("link_url", row.getLinkUrl());
        snapshot.put("launch_date", row.getLaunchDate() == null ? null : row.getLaunchDate().toString());
        snapshot.put("status", row.getStatus());
        snapshot.put("is_featured", row.getIsFeatured());
        return JsonUtils.toJsonString(snapshot);
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

}
