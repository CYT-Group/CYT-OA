package com.cyt.center.module.bpm.framework.flow.core;

/**
 * 流程引擎与业务代码之间的约定。
 */
public final class BpmFlowConstants {

    private BpmFlowConstants() {
    }

    /**
     * 流程变量里存「发起人自选审批人」的键：nodeCode -> 用户编号列表。
     * 发起时随 variables 存入实例，之后每个节点分派时由全局监听器取出覆盖节点审批人。
     */
    public static final String ASSIGNEE_VAR_KEY = "_assignees";

}
