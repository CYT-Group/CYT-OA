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

    // ========== 培训中心 1-060-500-000 ==========
    ErrorCode TRAINING_NOT_EXISTS = new ErrorCode(1_060_500_000, "课程不存在");
    ErrorCode TRAINING_FORBIDDEN = new ErrorCode(1_060_500_001, "权限不足");
    ErrorCode TRAINING_STATUS_INVALID = new ErrorCode(1_060_500_002, "status 只能是 draft 或 published");
    ErrorCode TRAINING_LEVEL_INVALID = new ErrorCode(1_060_500_003, "level 只能是 required 或 elective");
    ErrorCode TRAINING_CATEGORY_REQUIRED = new ErrorCode(1_060_500_004, "分类不能为空");
    ErrorCode TRAINING_PUBLISH_NO_LESSONS = new ErrorCode(1_060_500_005, "课程至少要有一个课时才能发布");
    ErrorCode TRAINING_LESSON_NOT_EXISTS = new ErrorCode(1_060_500_006, "课时不存在");
    ErrorCode TRAINING_LESSON_FIELDS_INVALID = new ErrorCode(1_060_500_007, "{}");
    ErrorCode TRAINING_PROGRESS_PERCENT_INVALID = new ErrorCode(1_060_500_008, "percent 须在 0-100 之间");
    /** NAS/MinIO 未配置或不可达（Portal 的 Unavailable → 503 语义） */
    ErrorCode TRAINING_NAS_UNAVAILABLE = new ErrorCode(1_060_500_009, "{}");
    /** 媒体预签名：课时缺对象键/图片/外链（Portal 的 Invalid → 422 语义） */
    ErrorCode TRAINING_MEDIA_KEY_MISSING = new ErrorCode(1_060_500_010, "{}");
    /** 管理端直传：kind / 扩展名不匹配（Portal 的 Invalid → 422 语义） */
    ErrorCode TRAINING_UPLOAD_INVALID = new ErrorCode(1_060_500_011, "{}");
    /** 导入与另一次扫描撞唯一键（Portal 的 Conflict → 409 语义） */
    ErrorCode TRAINING_IMPORT_CONFLICT = new ErrorCode(1_060_500_012, "导入与另一次扫描撞车，请稍后重试");

    // ========== 展会日历 1-060-600-000 ==========
    ErrorCode EVENT_NOT_EXISTS = new ErrorCode(1_060_600_000, "活动不存在");

    // ========== 新品动态 1-060-700-000 ==========
    ErrorCode PRODUCT_NOT_EXISTS = new ErrorCode(1_060_700_000, "产品不存在");

    // ========== HR 文档 CMS 1-060-800-000 ==========
    ErrorCode HRDOC_NOT_EXISTS = new ErrorCode(1_060_800_000, "文档不存在");
    ErrorCode HRDOC_FORBIDDEN = new ErrorCode(1_060_800_001, "权限不足");
    ErrorCode HRDOC_NOT_PUBLISHED = new ErrorCode(1_060_800_002, "文档未发布");
    ErrorCode HRDOC_OFFLINE = new ErrorCode(1_060_800_003, "文档已下架");
    /** slug 形状/长度不合法（Portal 的 Invalid → 422 语义） */
    ErrorCode HRDOC_SLUG_INVALID = new ErrorCode(1_060_800_004, "{}");
    /** slug 或其旧路径已被其它文档占用（Portal 的 Conflict → 409 语义） */
    ErrorCode HRDOC_SLUG_TAKEN = new ErrorCode(1_060_800_005, "slug 已被其它文档占用");
    ErrorCode HRDOC_CATEGORY_INVALID = new ErrorCode(1_060_800_006, "{}");
    ErrorCode HRDOC_TOPIC_INVALID = new ErrorCode(1_060_800_007, "{}");
    /** 块/展示字段/封面/签收等形状校验不通过（Portal 的 Invalid → 422 语义） */
    ErrorCode HRDOC_FIELDS_INVALID = new ErrorCode(1_060_800_008, "{}");
    /** 无草稿可发布（Portal 的 BadRequest → 400 语义） */
    ErrorCode HRDOC_NO_DRAFT = new ErrorCode(1_060_800_009, "无草稿内容可发布");
    ErrorCode HRDOC_ALREADY_PUBLISHED_NOT_OFFLINE = new ErrorCode(1_060_800_010, "文档已发布且未下架，请先下架再删除");
    ErrorCode HRDOC_NEVER_PUBLISHED = new ErrorCode(1_060_800_011, "文档从未发布");
    /** 目标版本已发布过（Portal 的 Conflict → 409 语义） */
    ErrorCode HRDOC_VERSION_TAKEN = new ErrorCode(1_060_800_012, "版本 {} 已发布过，请使用新版本号");
    ErrorCode HRDOC_VERSION_NOT_EXISTS = new ErrorCode(1_060_800_013, "版本不存在");
    ErrorCode HRDOC_DRAFT_VERSION_NOT_EXISTS = new ErrorCode(1_060_800_014, "草稿版本不存在");
    /** 发布确认凭据失效（草稿在确认期间变化，Portal 的 Conflict → 409 语义） */
    ErrorCode HRDOC_REVISION_CONFLICT = new ErrorCode(1_060_800_015, "草稿在确认期间已变化，请重新检查后发布");
    ErrorCode HRDOC_BODY_CHANGED = new ErrorCode(1_060_800_016, "正文已修改，请确认更高版本号后发布");

    // ========== 制度签收 1-060-910-000 ==========
    ErrorCode SIGNOFF_REF_REQUIRED = new ErrorCode(1_060_910_000, "document_id 与 document 必须且只能提供一个");
    ErrorCode SIGNOFF_NOT_SIGNABLE = new ErrorCode(1_060_910_001, "文档未配置为可签收（需 requires_signoff=true 且已发布）");
    ErrorCode SIGNOFF_VERSION_MISMATCH = new ErrorCode(1_060_910_002, "版本号不匹配：当前最新版本为 {}");
    ErrorCode SIGNOFF_DEPT_FORBIDDEN = new ErrorCode(1_060_910_003, "本文档限定特定部门签收");
    ErrorCode SIGNOFF_UNKNOWN_DOC = new ErrorCode(1_060_910_004, "未知文档或未发布");


}
