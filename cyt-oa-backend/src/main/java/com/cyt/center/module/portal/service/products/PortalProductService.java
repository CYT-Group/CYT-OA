package com.cyt.center.module.portal.service.products;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.portal.controller.admin.products.vo.PortalProductPageReqVO;
import com.cyt.center.module.portal.controller.admin.products.vo.PortalProductSaveReqVO;
import com.cyt.center.module.portal.dal.dataobject.products.PortalProductDO;

import java.util.List;

/**
 * 新品动态用例（逐条对齐 Portal 的 application/products.py）。
 *
 * 无审批流：创建即 published；SQL 只在 dal/mysql/products，本层只串规则与事务。
 */
public interface PortalProductService {

    /** 管理端分页列新品（含全部状态，管理面不分状态） */
    PageResult<PortalProductDO> page(PortalProductPageReqVO pageReqVO);

    /** 管理端取单条新品（不看状态，编辑表单用），不存在 404 */
    PortalProductDO get(Long id);

    /** 新建新品：status 恒 published，is_featured 缺省 false */
    PortalProductDO create(PortalProductSaveReqVO createReqVO);

    /** 全量覆盖改新品：可清空 brand / category / description / image_url / link_url / launch_date */
    PortalProductDO update(Long id, PortalProductSaveReqVO updateReqVO);

    /** 删除新品，随后记审计（Portal 只有删除留审计） */
    void delete(Long userId, Long id);

    /** 读侧：已发布的新品列表（可带品牌筛选），按创建时间倒序（Portal 的 GET /products） */
    List<PortalProductDO> listPublished(String brand, Integer limit);

    /** 读侧：单条新品详情；不存在或未发布统一 404 */
    PortalProductDO getPublished(Long id);

}
