package com.cyt.center.module.me.model.cmd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 个人中心写操作的入参。字段一律白名单声明，不接受前端传入的身份、部门或审批人。
 */
public final class MeCmds {

    private MeCmds() {
    }

    @Data
    public static class PreferenceCmd {
        private Boolean reportDue;
        private Boolean overdue;
        private Boolean milestone;
        private Boolean corpNews;
    }

    @Data
    public static class VoiceCmd {
        @NotBlank(message = "请选择发声渠道")
        @Size(max = 30, message = "渠道长度不能超过 30")
        private String channel;

        @NotBlank(message = "内容不能为空")
        @Size(max = 2000, message = "内容长度不能超过 2000")
        private String content;

        /** true 时对读者隐去姓名，作者本人仍可找回 */
        private Boolean anonymous;
    }

    @Data
    public static class FollowCmd {
        @NotNull(message = "节点编号不能为空")
        private Long nodeId;

        /** 开始前几天提醒，缺省 3 天 */
        private Integer remindBefore;
    }

    @Data
    public static class UnfollowCmd {
        @NotNull(message = "节点编号不能为空")
        private Long nodeId;
    }

    @Data
    public static class BlessCmd {
        @NotNull(message = "缺少祝福对象")
        private Long toUserId;

        /** 祝福针对的里程碑类型：birthday、anniversary */
        @NotBlank(message = "缺少里程碑类型")
        @Size(max = 30, message = "类型长度不能超过 30")
        private String type;

        /** 针对的那一天，同一天不重复记账 */
        @NotNull(message = "缺少里程碑日期")
        private LocalDate date;
    }

    @Data
    public static class RecognizeCmd {
        @NotNull(message = "缺少认可对象")
        private Long toUserId;

        /** like 本周点赞、praise 本月公开表扬、star_nomination 本月之星；其余类型服务端拒绝 */
        @NotBlank(message = "缺少认可类型")
        @Size(max = 30, message = "类型长度不能超过 30")
        private String type;

        @Size(max = 200, message = "评语长度不能超过 200")
        private String note;
    }

    @Data
    public static class CommentCmd {
        /** 由路径 /{id}/comment 覆盖，请求体里不用带。 */
        private Long reportId;

        @Size(max = 20, message = "评级长度不能超过 20")
        private String grade;

        @NotBlank(message = "点评内容不能为空")
        @Size(max = 1000, message = "点评内容不能超过 1000 字")
        private String reviewText;
    }

    @Data
    public static class CommentReplyCmd {
        /** 由路径 /comments/{commentId}/reply 覆盖，请求体里不用带。 */
        private Long commentId;

        @NotBlank(message = "回复内容不能为空")
        @Size(max = 1000, message = "回复内容不能超过 1000 字")
        private String reply;
    }

    /** 周报正文：与 demo 的四段式一致 —— 完成 / 计划 / 风险 / 关键数据 */
    @Data
    public static class ReportCmd {
        private Long id;

        @NotNull(message = "汇报类型不能为空")
        private Integer type;

        private String title;

        @Size(max = 4000, message = "工作总结过长")
        private String summary;

        @Size(max = 4000, message = "工作计划过长")
        private String plan;

        @Size(max = 2000, message = "风险说明过长")
        private String problem;

        /** 关键数据：demo 四段式的最后一栏，落 oa_work_report.remark */
        @Size(max = 2000, message = "关键数据过长")
        private String remark;

        private List<WorkItemCmd> workItems;

        private List<String> planItems;

        @Data
        public static class WorkItemCmd {
            @NotBlank(message = "工作项内容不能为空")
            @Size(max = 500, message = "工作项内容不能超过 500 字")
            private String content;

            private Integer progress;
        }
    }

    @Data
    public static class HandleCmd {
        @NotNull(message = "任务编号不能为空")
        private Long taskId;

        @Size(max = 500, message = "审批意见不能超过 500 字")
        private String message;
    }
}
