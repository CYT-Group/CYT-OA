package com.cyt.center.module.portal.controller.admin.feedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工反馈响应 VO。
 */
@Schema(description = "管理后台 - 员工反馈 Response VO")
@Data
public class PortalFeedbackRespVO {

    private Long id;

    private String reporterName;

    private String department;

    private String category;

    private String subCategory;

    private String brand;

    private String productName;

    private String rawMessage;

    private List<String> imageUrls;

    private String status;

    private LocalDateTime createdAt;

}
