package com.cyt.center.module.portal.controller.admin.contracts;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.service.contracts.PortalContractService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/** 合同个人资料只进入本次渲染内存，不持久化、不写日志。 */
@RestController
@RequestMapping("/portal/contracts")
@RequiredArgsConstructor
@SaCheckPermission("can_generate_contracts")
public class PortalContractController {
    private final PortalContractService service;
    public record Entity(@NotBlank @Size(max=100) String name,@NotBlank @Size(max=200) String address,@NotBlank @Size(max=50) String legalRep) {}
    public record Employee(@NotBlank @Size(max=50) String name,@NotBlank @Pattern(regexp="[0-9]{17}[0-9Xx]") String idNumber,
                           @NotBlank @Size(max=50) String phone,@NotBlank @Size(max=200) String homeAddress,
                           @NotBlank @Size(max=200) String emergencyContact) {}
    public record Dates(@NotNull LocalDate signDate,@NotNull LocalDate startDate,@NotNull LocalDate endDate,@NotNull LocalDate probationEnd) {}
    public record Position(@NotBlank @Size(max=100) String title,@NotBlank @Size(max=50) String reportTo,@NotBlank @Size(max=50) String workCity,
                           @NotBlank @Size(max=2000) String workSchedule,@NotEmpty @Size(max=20) List<@NotBlank @Size(max=500) String> duties) {}
    public record Salary(@NotNull @Min(0) Integer probationBase,@NotNull @Min(0) Integer probationAllowance,@NotNull @Min(1) Integer regularBase,
                         @NotNull @Min(0) Integer regularAllowance,@NotNull @Min(0) Integer performance) {}
    public record Generate(@NotNull @Positive Long companyId,@NotNull @Valid Employee employee,@NotNull @Valid Dates dates,
                           @NotNull @Valid Position position,@NotNull @Valid Salary salary) {}
    @GetMapping("/config") public Result<Map<String,Object>> config() { return Result.success(service.config()); }
    @PostMapping("/entities") public Result<Map<String,Object>> create(@Valid @RequestBody Entity r) { return Result.success(service.entity(getLoginUserId(),null,r)); }
    @PutMapping("/entities/{id}") public Result<Map<String,Object>> update(@PathVariable long id,@Valid @RequestBody Entity r) { return Result.success(service.entity(getLoginUserId(),id,r)); }
    @PostMapping("/generate") public ResponseEntity<byte[]> generate(@Valid @RequestBody Generate r) {
        byte[] bytes=service.generate(getLoginUserId(),r);
        String name="劳动合同-"+r.employee.name.replaceAll("[\\p{Cntrl}\\\"';/\\\\]", "")+"-"+r.dates.signDate+".docx";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(name,StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL,"no-store").body(bytes);
    }
}
