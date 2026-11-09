package com.cyt.center.module.portal.service.support;

import com.cyt.center.framework.common.util.servlet.ServletUtils;
import com.cyt.center.module.system.api.logger.OperateLogApi;
import com.cyt.center.module.system.api.logger.dto.OperateLogCreateReqDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 后台管理动作审计：Portal 的 portal_admin_audit 口径落到 sys_operate_log。
 *
 * 三条必须保住的语义（都是 Portal 那边踩过之后定下来的）：
 * 1. 审计在业务事务提交之后才写——业务回滚不该留下一条「改成了什么样」的假记录；
 *    这里用 TransactionSynchronization 挂 afterCommit，等价于原先「commit 之后再 audit」。
 * 2. 审计抖动不该把成功的业务打成失败，所以写入失败只记 warn，不向上抛。
 * 3. 大对象（富文本正文、整份 JSON 快照）压成 {@code <sha256:16位摘要 len:原长>}，
 *    小值保持原文可读——阈值见 {@link #VALUE_MAX_LEN}。摘要带长度，是为了让「前后是否相等」
 *    和「改了多少」两个问题都能只靠一条日志回答。
 *
 * 字段映射与 B1 搬历史的 364 行同口径：sub_type 存 Portal 的 action 键，
 * biz_id 取 target_id 的数字前缀（reorder 那种「整层」操作没有单一编号，留 null、
 * 原始串进 extra），target_table / before_value / after_value / comment 一一对应。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortalAudit {

    /** 单条 before/after 的字面量上限，超过即按摘要存 */
    static final int VALUE_MAX_LEN = 500;

    private static final String MODULE_TYPE = "后台管理";

    private final OperateLogApi operateLogApi;

    /**
     * 记一条审计。
     *
     * @param action      Portal 口径的动作键，如 category_create / news_approve
     * @param targetTable 被改动的表名
     * @param targetId    目标编号，允许是非数字串（如 material.sample/root），非数字时只进 extra
     * @param comment     说明，如审批意见
     */
    public void record(Long userId, String action, String targetTable, Object targetId,
                       String beforeValue, String afterValue, String comment) {
        OperateLogCreateReqDTO dto = new OperateLogCreateReqDTO()
                .setUserId(userId)
                .setUserType(2)
                .setType(MODULE_TYPE)
                .setSubType(action)
                .setBizId(numericPrefix(targetId))
                .setAction(action)
                .setExtra(targetId == null ? null : String.valueOf(targetId))
                .setTargetTable(targetTable)
                .setBeforeValue(summarize(beforeValue))
                .setAfterValue(summarize(afterValue))
                .setComment(comment == null ? "" : comment);
        fillRequestContext(dto);
        afterCommit(() -> write(dto, action, targetId));
    }

    private void write(OperateLogCreateReqDTO dto, String action, Object targetId) {
        try {
            operateLogApi.createOperateLog(dto);
        } catch (Exception ex) {
            log.warn("[record][{} 目标({}/{})] 审计写入失败", action, dto.getTargetTable(), targetId, ex);
        }
    }

    /**
     * 有事务就把写入推到 commit 之后，没有事务（只读用例误调、或脚本直调）就立刻写。
     */
    private void afterCommit(Runnable write) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            write.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                write.run();
            }
        });
    }

    /**
     * 请求上下文能拿到就填，拿不到（定时任务、脚本直调）留 null：
     * MyBatis-Plus 的 insert 默认不写 null 字段，库里落的是列默认值空串，不会插失败。
     */
    private void fillRequestContext(OperateLogCreateReqDTO dto) {
        HttpServletRequest request = ServletUtils.getRequest();
        dto.setUserIp(ServletUtils.getClientIP());
        dto.setUserAgent(ServletUtils.getUserAgent());
        if (request != null) {
            dto.setRequestMethod(request.getMethod());
            dto.setRequestUrl(truncate(request.getRequestURI(), 255));
        }
    }

    /** 大对象 → {@code <sha256:16位 len:原长>}；小对象原样返回；null 透传 */
    public static String summarize(String value) {
        if (value == null || value.length() <= VALUE_MAX_LEN) {
            return value;
        }
        return "<sha256:" + digest16(value) + " len:" + value.length() + ">";
    }

    private static String digest16(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception ex) {
            // JVM 必有 SHA-256；真出问题退化成只记长度，别让审计把业务带崩
            return "unknown";
        }
    }

    /** Portal 的 target_id 是文本列，可能是数字也可能是 slug，取数字前缀当业务编号 */
    private static Long numericPrefix(Object targetId) {
        if (targetId == null) {
            return null;
        }
        String text = String.valueOf(targetId);
        int start = -1;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return null;
        }
        int end = start;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
        }
        String digits = text.substring(start, end);
        // 超长数字串（比如把整层 id 拼进 target）当「没有单一编号」，不做有损截断
        return digits.length() > 18 ? null : Long.valueOf(digits);
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

}
