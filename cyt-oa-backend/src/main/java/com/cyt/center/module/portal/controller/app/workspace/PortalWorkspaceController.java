package com.cyt.center.module.portal.controller.app.workspace;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.service.workspace.*;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/** 登录态门户业务入口。每项服务独立检查操作权限、所有权和数据范围。 */
@RestController
@RequestMapping("/api/workspace")
@RequiredArgsConstructor
@Validated
public class PortalWorkspaceController {
    private final WorkspaceIdentity identity;
    private final WorkspaceObjectiveService objectives;
    private final WorkspacePolicyService policies;
    private final WorkspaceVoiceService voices;
    private final WorkspaceSurveyService surveys;
    private final WorkspaceRequestService requests;
    private final WorkspacePersonalService personal;
    private final WorkspaceReviewReportService reviewReports;
    private final WorkspaceTeamAttendanceService teamAttendance;

    @GetMapping("/context") public Result<Map<String,Object>> context(){var out=identity.context();out.put("canApproveMaterials",personal.canApproveMaterials());out.put("canApproveRequests",personal.canApproveRequests());return Result.success(out);}
    @GetMapping("/users") public Result<List<Map<String,Object>>> users(){identity.principal();return Result.success(identity.options());}
    @GetMapping("/celebrations") public Result<List<Map<String,Object>>> celebrations(){return Result.success(personal.celebrations());}
    @GetMapping("/material-records") public Result<List<Map<String,Object>>> materialRecords(@RequestParam(defaultValue="office") String kind){return Result.success(personal.materialRecords(kind,false));}
    @GetMapping("/materials-approval-records") public Result<List<Map<String,Object>>> materialApprovals(@RequestParam(defaultValue="all") String kind){return Result.success(personal.materialRecords(kind,true));}
    @GetMapping("/team-attendance") public Result<Map<String,Object>> teamAttendance(@RequestParam String month,@RequestParam(defaultValue="team") String scope,@RequestParam(defaultValue="1") int pageNo,@RequestParam(defaultValue="20") int pageSize){return Result.success(teamAttendance.page(month,scope,pageNo,pageSize));}
    @GetMapping("/objectives") public Result<List<Map<String,Object>>> objectives(@RequestParam(required=false) String period,@RequestParam(defaultValue="false") boolean alignment){return Result.success(objectives.list(period,alignment));}
    @PostMapping("/objectives") public Result<Map<String,Object>> objective(@RequestBody Map<String,Object> body){return Result.success(objectives.save(null,body));}
    @PutMapping("/objectives/{id}") public Result<Map<String,Object>> objective(@PathVariable @Positive long id,@RequestBody Map<String,Object> body){return Result.success(objectives.save(id,body));}
    @DeleteMapping("/objectives/{id}") public Result<Boolean> deleteObjective(@PathVariable @Positive long id){objectives.delete(id);return Result.success(true);}

    @GetMapping("/policies/{key}") public Result<Map<String,Object>> policy(@PathVariable String key){return Result.success(policies.read(key));}
    @PutMapping("/policies/{key}") public Result<Map<String,Object>> policy(@PathVariable String key,@RequestBody Map<String,Object> body){return Result.success(policies.save(key,body));}

    @GetMapping("/voices") public Result<List<Map<String,Object>>> voices(@RequestParam(defaultValue="all") String channel,@RequestParam(defaultValue="false") boolean manage){return Result.success(voices.list(channel,manage));}
    @PostMapping("/voices") public Result<Long> voice(@RequestBody Map<String,Object> body){return Result.success(voices.submit(body));}
    @PutMapping("/voices/{id}/reply") public Result<Boolean> reply(@PathVariable @Positive long id,@RequestBody Map<String,Object> body){voices.reply(id,body);return Result.success(true);}

    @GetMapping("/surveys") public Result<List<Map<String,Object>>> surveys(@RequestParam(defaultValue="false") boolean manage){return Result.success(surveys.list(manage));}
    @PostMapping("/surveys") public Result<Map<String,Object>> survey(@RequestBody Map<String,Object> body){return Result.success(surveys.save(null,body));}
    @PutMapping("/surveys/{id}") public Result<Map<String,Object>> survey(@PathVariable @Positive long id,@RequestBody Map<String,Object> body){return Result.success(surveys.save(id,body));}
    @PostMapping("/surveys/{id}/publish") public Result<Map<String,Object>> publish(@PathVariable @Positive long id){return Result.success(surveys.publish(id));}
    @PostMapping("/surveys/{id}/close") public Result<Boolean> close(@PathVariable @Positive long id){surveys.close(id);return Result.success(true);}
    @PostMapping("/surveys/{id}/remind") public Result<Integer> remind(@PathVariable @Positive long id){return Result.success(surveys.remind(id));}
    @PostMapping("/surveys/{id}/answers") public Result<Boolean> answer(@PathVariable @Positive long id,@RequestBody Map<String,Object> body){surveys.answer(id,body);return Result.success(true);}
    @GetMapping("/surveys/{id}/results") public Result<Map<String,Object>> results(@PathVariable @Positive long id){return Result.success(surveys.results(id));}

    @GetMapping("/requests") public Result<List<Map<String,Object>>> requests(@RequestParam(defaultValue="mine") String view,@RequestParam(required=false) String type,@RequestParam(required=false) String status){return Result.success(requests.list(view,type,status));}
    @GetMapping("/requests/{id}") public Result<Map<String,Object>> request(@PathVariable @Positive long id){return Result.success(requests.get(id));}
    @PostMapping("/requests") public Result<Map<String,Object>> request(@RequestBody Map<String,Object> body,@RequestParam(defaultValue="false") boolean submit){return Result.success(requests.save(null,body,submit));}
    @PutMapping("/requests/{id}") public Result<Map<String,Object>> request(@PathVariable @Positive long id,@RequestBody Map<String,Object> body,@RequestParam(defaultValue="false") boolean submit){return Result.success(requests.save(id,body,submit));}
    public record Decision(String comment) {}
    @PostMapping("/requests/{id}/approve") public Result<Map<String,Object>> approve(@PathVariable @Positive long id,@RequestBody Decision body){return Result.success(requests.act(id,true,body.comment()));}
    @PostMapping("/requests/{id}/reject") public Result<Map<String,Object>> reject(@PathVariable @Positive long id,@RequestBody Decision body){return Result.success(requests.act(id,false,body.comment()));}
    @PostMapping("/requests/{id}/cancel") public Result<Boolean> cancel(@PathVariable @Positive long id){requests.cancel(id);return Result.success(true);}

    @GetMapping("/review-reports") public Result<List<Map<String,Object>>> reviewReports(@RequestParam(defaultValue="mine") String view){return Result.success(reviewReports.list(view));}
    @GetMapping("/review-reports/{id}") public Result<Map<String,Object>> reviewReport(@PathVariable @Positive long id){return Result.success(reviewReports.get(id));}
    @PostMapping("/review-reports") public Result<Map<String,Object>> reviewReport(@RequestBody Map<String,Object> body){return Result.success(reviewReports.save(null,body));}
    @PutMapping("/review-reports/{id}") public Result<Map<String,Object>> reviewReport(@PathVariable @Positive long id,@RequestBody Map<String,Object> body){return Result.success(reviewReports.save(id,body));}
    @PostMapping("/review-reports/{id}/submit") public Result<Map<String,Object>> submitReview(@PathVariable @Positive long id){return Result.success(reviewReports.submit(id));}
    @PostMapping("/review-reports/{id}/comment") public Result<Map<String,Object>> commentReview(@PathVariable @Positive long id,@RequestBody Map<String,Object> body){return Result.success(reviewReports.comment(id,body));}
}
