package com.cyt.center.module.portal.enums;

import com.cyt.center.framework.common.exception.ErrorCode;

/**
 * Portal 内容域错误码
 *
 * 门户管理面（从 CYT-Portal 迁来的内容/分类/埋点），使用 1-060-000-000 段
 *
 * 口径出处：docs/Portal管理面迁移契约.md §一「内容管理」。
 * Portal 侧原本用 HTTP 状态码表达语义（404 防探测、409 撞名、422 入参不合注册表、403 能力不足），
 * 迁到 OA 后统一走 Result.code，Portal 展示端接读接口时按下面的分段还原状态码。
 */
public interface ErrorCodeConstants {

    // ========== 通用分类 1-060-100-000 ==========
    ErrorCode CATEGORY_NOT_EXISTS = new ErrorCode(1_060_100_000, "分类不存在");
    ErrorCode CATEGORY_PARENT_NOT_EXISTS = new ErrorCode(1_060_100_001, "父分类不存在");
    ErrorCode CATEGORY_SCOPE_INVALID = new ErrorCode(1_060_100_002, "未知的分类 scope: {}");
    ErrorCode CATEGORY_NAME_INVALID = new ErrorCode(1_060_100_003, "{}");
    ErrorCode CATEGORY_ATTRS_INVALID = new ErrorCode(1_060_100_004, "{}");
    ErrorCode CATEGORY_CODE_INVALID = new ErrorCode(1_060_100_005, "{}");
    ErrorCode CATEGORY_NAME_DUPLICATE = new ErrorCode(1_060_100_006, "同名分类已存在");
    ErrorCode CATEGORY_CODE_DUPLICATE = new ErrorCode(1_060_100_007, "code 已被占用");
    ErrorCode CATEGORY_ARCHIVED = new ErrorCode(1_060_100_008, "该分类已归档，请先恢复再编辑");
    ErrorCode CATEGORY_ALREADY_ARCHIVED = new ErrorCode(1_060_100_009, "该分类已归档");
    ErrorCode CATEGORY_NOT_ARCHIVED = new ErrorCode(1_060_100_010, "该分类未归档");
    ErrorCode CATEGORY_RESTORE_NAME_DUPLICATE = new ErrorCode(1_060_100_011, "已有同名在役分类，请先改名或归档它");
    ErrorCode CATEGORY_HAS_CHILDREN = new ErrorCode(1_060_100_012, "该分类下还有子分类，不可删除；请先移走或归档它们");
    ErrorCode CATEGORY_HAS_REFS = new ErrorCode(1_060_100_013, "{}");
    ErrorCode CATEGORY_STILL_REFERENCED = new ErrorCode(1_060_100_014, "该分类仍被引用，不可删除");
    ErrorCode CATEGORY_SHAPE_INVALID = new ErrorCode(1_060_100_015, "{}");
    ErrorCode CATEGORY_DEPTH_IMMUTABLE = new ErrorCode(1_060_100_016, "不能改变分类层级（大类与小类不可互转）");
    ErrorCode CATEGORY_SELF_PARENT = new ErrorCode(1_060_100_017, "不能把自己设为父分类");
    ErrorCode CATEGORY_CODE_IMMUTABLE = new ErrorCode(1_060_100_018, "大类 code 建后不可改");
    ErrorCode CATEGORY_CODE_SETTLED = new ErrorCode(1_060_100_019, "code 已设定，不可修改");
    ErrorCode CATEGORY_REORDER_EMPTY = new ErrorCode(1_060_100_020, "ordered_ids 不能为空");
    ErrorCode CATEGORY_REORDER_DUPLICATE = new ErrorCode(1_060_100_021, "ordered_ids 有重复");
    ErrorCode CATEGORY_REORDER_MISSING = new ErrorCode(1_060_100_022, "分类不存在: id={}");
    ErrorCode CATEGORY_REORDER_CROSS_LAYER = new ErrorCode(1_060_100_023, "ordered_ids 必须都在同一层（同 scope、同父分类）");
    ErrorCode CATEGORY_REORDER_CROSS_DEPTH = new ErrorCode(1_060_100_024, "ordered_ids 跨层（大类与小类不能一起排序）");
    ErrorCode CATEGORY_MANAGE_FORBIDDEN = new ErrorCode(1_060_100_025, "权限不足");
    /** 并发建同名 / 同 code：预检漏过去了，唯一索引兜底，文案与预检同款 */
    ErrorCode CATEGORY_DUPLICATE = new ErrorCode(1_060_100_026, "同名分类或 code 已存在");

    // ========== 公司动态 1-060-200-000 ==========
    ErrorCode NEWS_NOT_EXISTS = new ErrorCode(1_060_200_000, "新闻不存在");
    ErrorCode NEWS_FORBIDDEN = new ErrorCode(1_060_200_001, "权限不足");
    ErrorCode NEWS_PUSH_STATUS_INVALID = new ErrorCode(1_060_200_002, "仅 published 状态可推送；当前 {}");
    ErrorCode NEWS_NOT_VISIBLE = new ErrorCode(1_060_200_003, "动态不存在");
    ErrorCode NEWS_COMMENT_NOT_EXISTS = new ErrorCode(1_060_200_004, "评论不存在");
    ErrorCode NEWS_COMMENT_DELETE_FORBIDDEN = new ErrorCode(1_060_200_005, "无权删除该评论");
    ErrorCode NEWS_COMMENT_CONTENT_EMPTY = new ErrorCode(1_060_200_006, "评论内容不能为空");
    ErrorCode NEWS_COMMENT_PARENT_INVALID = new ErrorCode(1_060_200_007, "回复的评论不存在或不属于此动态");

    // ========== 知识库 1-060-300-000 ==========
    ErrorCode WIKI_NOT_EXISTS = new ErrorCode(1_060_300_000, "文档不存在");
    ErrorCode WIKI_FORBIDDEN = new ErrorCode(1_060_300_001, "权限不足");

    // ========== 页面埋点与看板 1-060-400-000 ==========
    /** Portal 的 track 缺 page_url 时回 200 + success=False；迁到 OA 后按入参不合法处理，走这个码 */
    ErrorCode PAGE_VIEW_URL_REQUIRED = new ErrorCode(1_060_400_000, "page_url required");

}
