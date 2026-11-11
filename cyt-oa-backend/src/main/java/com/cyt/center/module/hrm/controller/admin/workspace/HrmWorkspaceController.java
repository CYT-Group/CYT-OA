package com.cyt.center.module.hrm.controller.admin.workspace;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.cyt.center.core.result.Result;
import com.cyt.center.module.hrm.controller.admin.employee.vo.employee.HrmEmployeeSaveReqVO;
import com.cyt.center.module.hrm.controller.admin.employee.vo.employee.HrmEmployeeQuitReqVO;
import com.cyt.center.module.hrm.service.workspace.HrmWorkspaceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/hrm/workspace")
@RequiredArgsConstructor
public class HrmWorkspaceController {
    private final HrmWorkspaceService service;
    @GetMapping("/departments")
    @SaCheckPermission(value={"hrm:employee:query","hrm:recruit:post:query"},mode=SaMode.OR)
    public Result<?> departments() { return Result.success(service.departments()); }
    @GetMapping("/quit-reasons") @SaCheckPermission("hrm:employee:query")
    public Result<?> quitReasons() {
        return Result.success(java.util.Arrays.stream(com.cyt.center.module.hrm.enums.employee.employment.HrmEmployeeQuitReasonEnum.values())
            .map(v -> Map.of("value",v.getReason().toString(),"label",v.getName())).toList());
    }
    public record Template(@NotBlank @Size(max=100) String name, @NotBlank @Size(max=40) String version,
        @Pattern(regexp="onboard|offboard") @NotNull String kind, @NotNull @Min(1) @Max(9) Integer contractType, @NotBlank @Size(max=255) String scope,
        @NotBlank @Size(max=50000) String content) {}
    public record Review(boolean approved, @Size(max=500) String note) {}
    public record Document(@NotNull @Positive Long templateId, @NotBlank @Size(max=100) String company,
        @NotBlank @Size(max=100) String companyCode, @NotBlank @Size(max=255) String companyAddress,
        @NotNull LocalDate signDate, @NotNull LocalDate endDate,
        @NotNull @DecimalMin("0.01") @Digits(integer=8,fraction=2) BigDecimal salary) {}
    public record Onboard(@NotBlank @Pattern(regexp="[a-zA-Z0-9-]{16,64}") String requestKey,
        @NotNull @Valid HrmEmployeeSaveReqVO employee, @NotNull @Valid Document document) {}
    public record Offboard(@NotBlank @Pattern(regexp="[a-zA-Z0-9-]{16,64}") String requestKey,
        @NotNull @Valid HrmEmployeeQuitReqVO quit, @NotNull @Positive Long templateId,
        @NotBlank @Size(max=100) String company, @NotBlank @Size(max=500) String reason,
        @NotNull LocalDate signDate) {}

    @GetMapping("/templates") @SaCheckPermission("hrm:contract-template:query")
    public Result<?> templates() { return Result.success(service.templates()); }
    @PostMapping("/templates") @SaCheckPermission("hrm:contract-template:create")
    public Result<?> template(@Valid @RequestBody Template r) { return Result.success(service.submitTemplate(r)); }
    @PutMapping("/templates/{id}/review") @SaCheckPermission("hrm:contract-template:review")
    public Result<?> review(@PathVariable long id, @Valid @RequestBody Review r) { service.review(id,r); return Result.success(true); }
    @GetMapping("/lifecycle") @SaCheckPermission("hrm:employee:query")
    public Result<?> lifecycle(@RequestParam long employeeId) { return Result.success(service.lifecycle(employeeId)); }
    @GetMapping("/certificate") @SaCheckPermission("hrm:employee:query")
    public Result<?> certificate(@RequestParam long employeeId) { return Result.success(service.certificate(employeeId)); }
    @PostMapping("/onboard/preview") @SaCheckPermission("hrm:employee:update")
    public Result<?> preview(@Valid @RequestBody Onboard r) { return Result.success(Map.of("content",service.preview(r))); }
    @PostMapping("/onboard") @SaCheckPermission("hrm:employee:update")
    public Result<?> onboard(@Valid @RequestBody Onboard r) { return Result.success(service.onboard(r)); }
    @PostMapping("/offboard/preview") @SaCheckPermission("hrm:employee:update")
    public Result<?> previewOffboard(@Valid @RequestBody Offboard r) { return Result.success(Map.of("content",service.previewOffboard(r))); }
    @PostMapping("/offboard") @SaCheckPermission("hrm:employee:update")
    public Result<?> offboard(@Valid @RequestBody Offboard r) { return Result.success(service.offboard(r)); }
    @GetMapping("/documents/{id}") @SaCheckPermission("hrm:employee:query")
    public Result<?> document(@PathVariable long id) { return Result.success(service.document(id)); }
    @GetMapping("/interviews") @SaCheckPermission("hrm:recruit:interview:query")
    public Result<?> interviews(@RequestParam(defaultValue="1") int pageNo,@RequestParam(defaultValue="20") int pageSize,
        @RequestParam(defaultValue="") String search,@RequestParam(required=false) Integer result) {
        return Result.success(service.interviews(pageNo,pageSize,search,result));
    }
}
