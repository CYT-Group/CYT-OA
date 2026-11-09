package com.cyt.center.module.bpm.enums.task;

import com.cyt.center.framework.common.core.ArrayValuable;
import com.cyt.center.framework.common.util.object.ObjectUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 流程实例的状态。取值与 ruoyi 保持一致，移植过来的业务代码按状态码判断审批结果。
 */
@Getter
@AllArgsConstructor
public enum BpmProcessInstanceStatusEnum implements ArrayValuable<Integer> {

    NOT_START(-1, "未开始"),
    RUNNING(1, "审批中"),
    APPROVE(2, "审批通过"),
    REJECT(3, "审批不通过"),
    CANCEL(4, "已取消");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(BpmProcessInstanceStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String desc;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static boolean isRejectStatus(Integer status) {
        return REJECT.getStatus().equals(status);
    }

    public static boolean isProcessEndStatus(Integer status) {
        return ObjectUtils.equalsAny(status, APPROVE.getStatus(), REJECT.getStatus(), CANCEL.getStatus());
    }

    public static BpmProcessInstanceStatusEnum valueOf(Integer status) {
        return Arrays.stream(values()).filter(item -> item.getStatus().equals(status)).findFirst().orElse(null);
    }

}
