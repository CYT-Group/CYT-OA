package com.cyt.center.module.system.controller;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.integration.WecomApprovalClient;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.module.system.model.cmd.WecomTemplateBindCmd;
import com.cyt.center.module.system.model.cmd.WecomTemplateCloneCmd;
import com.cyt.center.module.system.model.cmd.WecomTemplateFieldCmd;
import com.cyt.center.module.system.model.cmd.WecomTemplateSaveCmd;
import com.cyt.center.module.system.model.cmd.WecomTemplateUnbindCmd;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 企微审批模板管理。企微没有「列出全部模板」的接口，列表以网关绑定表为口径；
 * 模板详情、新建、更新、克隆都实时打在企微上，Secret 仍留在网关侧。
 */
@Slf4j
@RestController
@RequestMapping("/admin-api/system/wecom-templates")
@RequiredArgsConstructor
public class WecomApprovalTemplateController {

    private static final String PERMISSION = "system:wecom-template:manage";

    private final WecomApprovalClient approvals;
    private final CurrentUser currentUser;

    /** 已绑定模板列表（别名、模板 ID、字段别名、描述、更新时间）。 */
    @GetMapping
    public Result<JsonNode> list() {
        currentUser.requirePermission(PERMISSION);
        return Result.ok(approvals.templates());
    }

    /** 模板详情：绑定别名或裸 template_id 均可，绑定前预览用裸 ID。 */
    @GetMapping("/get")
    public Result<JsonNode> get(@RequestParam("template") String template) {
        currentUser.requirePermission(PERMISSION);
        return Result.ok(approvals.templateDetail(template));
    }

    /**
     * 从历史审批单反查模板 ID。企微不提供「列出全部模板」的接口，管理后台里那些模板 ID 只能这样拿到：
     * 粘一张真实审批单的单号，回它的模板归属。单据内容属于申请人，这里只回定位模板需要的字段。
     */
    @GetMapping("/from-approval")
    public Result<Map<String, Object>> fromApproval(@RequestParam("approvalNo") String approvalNo) {
        currentUser.requirePermission(PERMISSION);
        String approval = approvalNo == null ? "" : approvalNo.trim();
        if (approval.isEmpty()) {
            throw new BizException("请先填写企微审批单号");
        }
        JsonNode data = approvals.detail(approval);
        String templateId = data.path("template_id").asText("");
        if (templateId.isBlank()) {
            throw new BizException("这张审批单里没有 template_id，换一张再试");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("approval_no", data.path("approval_no").asText(approval));
        result.put("template_id", templateId);
        result.put("name", data.path("name").asText(""));
        result.put("applicant", data.path("applicant").asText(""));
        result.put("submitted_at", data.path("submitted_at").asText(""));
        var actor = currentUser.require();
        log.info("按审批单反查模板 actor={}, approvalNo={}, templateId={}",
                actor.name() + "#" + actor.userId(), approval, templateId);
        return Result.ok(result);
    }

    @PostMapping("/bind")
    public Result<JsonNode> bind(@Valid @RequestBody WecomTemplateBindCmd cmd) {
        currentUser.requirePermission(PERMISSION);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("template_id", cmd.getTemplateId());
        params.put("alias", cmd.getAlias());
        params.put("field_aliases", cmd.getFieldAliases() == null ? Map.of() : cmd.getFieldAliases());
        params.put("description", cmd.getDescription() == null ? "" : cmd.getDescription());
        JsonNode result = approvals.templateBind(params);
        var actor = currentUser.require();
        log.info("绑定企微审批模板 actor={}, alias={}, templateId={}",
                actor.name() + "#" + actor.userId(), cmd.getAlias(), cmd.getTemplateId());
        return Result.ok(result);
    }

    @PostMapping("/unbind")
    public Result<JsonNode> unbind(@Valid @RequestBody WecomTemplateUnbindCmd cmd) {
        currentUser.requirePermission(PERMISSION);
        JsonNode result = approvals.templateUnbind(cmd.getAlias());
        var actor = currentUser.require();
        log.info("解绑企微审批模板 actor={}, alias={}", actor.name() + "#" + actor.userId(), cmd.getAlias());
        return Result.ok(result);
    }

    @PostMapping("/create")
    public Result<JsonNode> create(@Valid @RequestBody WecomTemplateSaveCmd cmd) {
        currentUser.requirePermission(PERMISSION);
        JsonNode result = approvals.templateSave("create", saveParams(cmd));
        var actor = currentUser.require();
        log.info("新建企微审批模板 actor={}, alias={}", actor.name() + "#" + actor.userId(), cmd.getAlias());
        return Result.ok(result);
    }

    @PostMapping("/update")
    public Result<JsonNode> update(@Valid @RequestBody WecomTemplateSaveCmd cmd) {
        currentUser.requirePermission(PERMISSION);
        JsonNode result = approvals.templateSave("update", saveParams(cmd));
        var actor = currentUser.require();
        log.info("更新企微审批模板 actor={}, template={}", actor.name() + "#" + actor.userId(), cmd.getTemplate());
        return Result.ok(result);
    }

    @PostMapping("/clone")
    public Result<JsonNode> clone(@Valid @RequestBody WecomTemplateCloneCmd cmd) {
        currentUser.requirePermission(PERMISSION);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("template", cmd.getTemplate());
        params.put("name", cmd.getName());
        if (cmd.getAlias() != null && !cmd.getAlias().isBlank()) {
            params.put("alias", cmd.getAlias());
        }
        if (cmd.getDescription() != null && !cmd.getDescription().isBlank()) {
            params.put("description", cmd.getDescription());
        }
        JsonNode result = approvals.templateSave("clone", params);
        var actor = currentUser.require();
        log.info("克隆企微审批模板 actor={}, source={}, alias={}",
                actor.name() + "#" + actor.userId(), cmd.getTemplate(), cmd.getAlias());
        return Result.ok(result);
    }

    private static Map<String, Object> saveParams(WecomTemplateSaveCmd cmd) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", cmd.getName());
        // create 的契约不接受 template 键，update 靠它定位原模板
        if (cmd.getTemplate() != null && !cmd.getTemplate().isBlank()) {
            params.put("template", cmd.getTemplate());
        }
        if (cmd.getAlias() != null && !cmd.getAlias().isBlank()) {
            params.put("alias", cmd.getAlias());
        }
        params.put("description", cmd.getDescription() == null ? "" : cmd.getDescription());
        List<Map<String, Object>> fields = new ArrayList<>();
        if (cmd.getFields() != null) {
            for (WecomTemplateFieldCmd field : cmd.getFields()) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("key", field.getKey());
                node.put("label", field.getLabel());
                node.put("control", field.getControl());
                node.putIfAbsent("required", Boolean.TRUE.equals(field.getRequired()));
                node.putIfAbsent("multiple", Boolean.TRUE.equals(field.getMultiple()));
                node.putIfAbsent("id", field.getId() == null ? "" : field.getId());
                node.putIfAbsent("options", field.getOptions() == null ? List.of() : field.getOptions());
                node.putIfAbsent("config", field.getConfig() == null ? Map.of() : field.getConfig());
                fields.add(node);
            }
        }
        params.put("fields", fields);
        return params;
    }

}
