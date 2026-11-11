package com.cyt.center.module.portal.controller.admin.resumes;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.service.resumes.PortalResumeInterviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/portal/resumes/{resumeId}/interviews")
@SaCheckPermission("can_manage_resumes")
@RequiredArgsConstructor
public class PortalResumeInterviewController {
    private final PortalResumeInterviewService service;
    public record Create(@NotBlank @Size(max=120) String position,@NotEmpty @Size(max=10) List<Map<String,Object>> rounds) {}
    public record Close(@NotBlank @Size(max=5000) String summary,String result) {}
    @GetMapping public Result<List<Map<String,Object>>> list(@PathVariable long resumeId){return Result.success(service.list(resumeId));}
    @PostMapping public Result<Map<String,Object>> create(@PathVariable long resumeId,@Valid @RequestBody Create r){return Result.success(service.create(getLoginUserId(),resumeId,r.position,r.rounds));}
    @PutMapping("/{processId}/rounds/{roundId}") public Result<Map<String,Object>> update(@PathVariable long resumeId,@PathVariable long processId,@PathVariable long roundId,@RequestBody Map<String,Object> r){return Result.success(service.updateRound(getLoginUserId(),resumeId,processId,roundId,r));}
    @PostMapping("/{processId}/finish") public Result<Map<String,Object>> finish(@PathVariable long resumeId,@PathVariable long processId,@Valid @RequestBody Close r){return Result.success(service.close(getLoginUserId(),resumeId,processId,r.summary,r.result,false));}
    @PostMapping("/{processId}/cancel") public Result<Map<String,Object>> cancel(@PathVariable long resumeId,@PathVariable long processId,@Valid @RequestBody Close r){return Result.success(service.close(getLoginUserId(),resumeId,processId,r.summary,null,true));}
}
