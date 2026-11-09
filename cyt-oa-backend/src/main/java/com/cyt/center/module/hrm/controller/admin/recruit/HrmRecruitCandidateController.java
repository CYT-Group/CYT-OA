package com.cyt.center.module.hrm.controller.admin.recruit;

import cn.dev33.satoken.annotation.SaCheckPermission;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.collection.MapUtils;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.HrmRecruitStatusCountRespVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.candidate.HrmRecruitCandidateUpdateChannelReqVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.candidate.HrmRecruitCandidateCleanReqVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.candidate.HrmRecruitCandidateUpdateEliminateReqVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.candidate.HrmRecruitCandidateEntryReqVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.candidate.HrmRecruitCandidatePageReqVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.candidate.HrmRecruitCandidateUpdatePostReqVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.candidate.HrmRecruitCandidateRespVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.candidate.HrmRecruitCandidateSaveReqVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.candidate.HrmRecruitCandidateUpdateStatusReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.dal.dataobject.recruit.candidate.HrmRecruitCandidateDO;
import com.cyt.center.module.hrm.dal.dataobject.recruit.config.HrmRecruitChannelDO;
import com.cyt.center.module.hrm.dal.dataobject.recruit.candidate.HrmRecruitInterviewDO;
import com.cyt.center.module.hrm.dal.dataobject.recruit.post.HrmRecruitPostDO;
import com.cyt.center.module.hrm.enums.recruit.candidate.HrmRecruitCandidateStatusEnum;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.hrm.service.recruit.candidate.HrmRecruitCandidateService;
import com.cyt.center.module.hrm.service.recruit.config.HrmRecruitChannelService;
import com.cyt.center.module.hrm.service.recruit.candidate.HrmRecruitInterviewService;
import com.cyt.center.module.hrm.service.recruit.post.HrmRecruitPostService;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertList;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertSet;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertSetByFlatMap;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.getFirst;

@Tag(name = "管理后台 - HRM 招聘候选人")
@RestController
@RequestMapping("/hrm/recruit/candidate")
@Validated
public class HrmRecruitCandidateController {

    @Resource
    private HrmRecruitCandidateService recruitCandidateService;
    @Resource
    private HrmRecruitPostService recruitPostService;
    @Resource
    private HrmRecruitChannelService recruitChannelService;
    @Resource
    private HrmRecruitInterviewService recruitInterviewService;
    @Resource
    private HrmEmployeeService employeeService;

    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建招聘候选人")
    @SaCheckPermission("hrm:recruit:candidate:create")
    public Result<Long> createRecruitCandidate(@Valid @RequestBody HrmRecruitCandidateSaveReqVO createReqVO) {
        return success(recruitCandidateService.createRecruitCandidate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新招聘候选人")
    @SaCheckPermission("hrm:recruit:candidate:update")
    public Result<Boolean> updateRecruitCandidate(@Valid @RequestBody HrmRecruitCandidateSaveReqVO updateReqVO) {
        recruitCandidateService.updateRecruitCandidate(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新招聘候选人状态")
    @SaCheckPermission("hrm:recruit:candidate:update")
    public Result<Boolean> updateRecruitCandidateStatus(@Valid @RequestBody HrmRecruitCandidateUpdateStatusReqVO reqVO) {
        recruitCandidateService.updateRecruitCandidateStatus(reqVO);
        return success(true);
    }

    @PutMapping("/update-post")
    @Operation(summary = "更新招聘候选人应聘职位")
    @SaCheckPermission("hrm:recruit:candidate:update")
    public Result<Boolean> updateRecruitCandidatePost(@Valid @RequestBody HrmRecruitCandidateUpdatePostReqVO reqVO) {
        recruitCandidateService.updateRecruitCandidatePost(reqVO);
        return success(true);
    }

    @PutMapping("/update-channel")
    @Operation(summary = "更新招聘候选人渠道")
    @SaCheckPermission("hrm:recruit:candidate:update")
    public Result<Boolean> updateRecruitCandidateChannel(
            @Valid @RequestBody HrmRecruitCandidateUpdateChannelReqVO reqVO) {
        recruitCandidateService.updateRecruitCandidateChannel(reqVO);
        return success(true);
    }

    @PutMapping("/eliminate")
    @Operation(summary = "淘汰招聘候选人")
    @SaCheckPermission("hrm:recruit:candidate:update")
    public Result<Boolean> eliminateRecruitCandidate(@Valid @RequestBody HrmRecruitCandidateUpdateEliminateReqVO reqVO) {
        recruitCandidateService.eliminateRecruitCandidate(reqVO);
        return success(true);
    }

    @PostMapping("/convert-employee")
    @Operation(summary = "候选人转员工档案")
    @SaCheckPermission("hrm:recruit:candidate:update")
    public Result<Long> convertRecruitCandidateToEmployee(
            @Valid @RequestBody HrmRecruitCandidateEntryReqVO reqVO) {
        return success(recruitCandidateService.convertRecruitCandidateToEmployee(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除招聘候选人")
    @Parameter(name = "id", description = "候选人编号", required = true, example = "1024")
    @SaCheckPermission("hrm:recruit:candidate:delete")
    public Result<Boolean> deleteRecruitCandidate(@RequestParam("id") Long id) {
        recruitCandidateService.deleteRecruitCandidate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得招聘候选人")
    @Parameter(name = "id", description = "候选人编号", required = true, example = "1024")
    @SaCheckPermission("hrm:recruit:candidate:query")
    public Result<HrmRecruitCandidateRespVO> getRecruitCandidate(@RequestParam("id") Long id) {
        return success(buildRecruitCandidateRespVO(recruitCandidateService.getRecruitCandidate(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得招聘候选人分页")
    @SaCheckPermission("hrm:recruit:candidate:query")
    public Result<PageResult<HrmRecruitCandidateRespVO>> getRecruitCandidatePage(
            @Validated HrmRecruitCandidatePageReqVO pageReqVO) {
        PageResult<HrmRecruitCandidateDO> pageResult = recruitCandidateService.getRecruitCandidatePage(pageReqVO);
        return success(new PageResult<>(buildRecruitCandidateRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/clean-ids")
    @Operation(summary = "获得待清理候选人编号")
    @SaCheckPermission("hrm:recruit:candidate:delete")
    public Result<List<Long>> getCleanRecruitCandidateIdList(@Valid HrmRecruitCandidateCleanReqVO reqVO) {
        List<HrmRecruitCandidateDO> list = recruitCandidateService.getRecruitCandidateList(
                reqVO.getStatuses(), LocalDateTime.now().minusDays(reqVO.getDays()));
        return success(convertList(list, HrmRecruitCandidateDO::getId));
    }

    @GetMapping("/status-count")
    @Operation(summary = "获得候选人状态数量")
    @SaCheckPermission("hrm:recruit:candidate:query")
    public Result<List<HrmRecruitStatusCountRespVO>> getRecruitCandidateStatusCount(
            @Validated HrmRecruitCandidatePageReqVO pageReqVO) {
        Map<Integer, Long> countMap = recruitCandidateService.getRecruitCandidateStatusCount(pageReqVO);
        return success(convertList(Arrays.asList(HrmRecruitCandidateStatusEnum.values()),
                status -> new HrmRecruitStatusCountRespVO(status.getStatus(),
                        countMap.getOrDefault(status.getStatus(), 0L))));
    }

    // ==================== 拼接 VO ====================

    private HrmRecruitCandidateRespVO buildRecruitCandidateRespVO(HrmRecruitCandidateDO recruitCandidate) {
        if (recruitCandidate == null) {
            return null;
        }
        return getFirst(buildRecruitCandidateRespVOList(Collections.singletonList(recruitCandidate)));
    }

    private List<HrmRecruitCandidateRespVO> buildRecruitCandidateRespVOList(List<HrmRecruitCandidateDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 1.1 招聘职位、部门和招聘渠道信息
        Map<Long, HrmRecruitPostDO> recruitPostMap = recruitPostService.getRecruitPostMap(
                convertSet(list, HrmRecruitCandidateDO::getPostId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSet(recruitPostMap.values(), HrmRecruitPostDO::getDeptId));
        Map<Long, HrmRecruitChannelDO> recruitChannelMap = recruitChannelService.getRecruitChannelMap(
                convertSet(list, HrmRecruitCandidateDO::getChannelId));
        // 1.2 当前面试和员工信息
        Set<Long> candidateIds = convertSet(list, HrmRecruitCandidateDO::getId);
        Map<Long, HrmRecruitInterviewDO> recruitInterviewMap = recruitInterviewService
                .getLatestRecruitInterviewMapByCandidateIds(candidateIds);
        Set<Long> employeeIds = convertSet(recruitPostMap.values(), HrmRecruitPostDO::getOwnerEmployeeId);
        employeeIds.addAll(convertSet(recruitInterviewMap.values(), HrmRecruitInterviewDO::getInterviewEmployeeId));
        employeeIds.addAll(convertSetByFlatMap(recruitInterviewMap.values(),
                HrmRecruitInterviewDO::getOtherInterviewEmployeeIds,
                Collection::stream));
        Map<Long, HrmEmployeeDO> employeeMap = employeeService.getEmployeeMap(employeeIds);
        Map<Long, HrmEmployeeDO> candidateEmployeeMap = employeeService.getEmployeeMapByCandidateIds(candidateIds);
        // 1.3 创建人信息
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(list, candidate -> NumberUtil.parseLong(candidate.getCreator())));

        // 2. 拼接响应
        return BeanUtils.toBean(list, HrmRecruitCandidateRespVO.class, vo -> {
            // 2.1 拼接招聘职位、部门和招聘渠道信息
            MapUtils.findAndThen(recruitPostMap, vo.getPostId(), post -> {
                vo.setPostName(post.getPostName()).setPostStatus(post.getStatus())
                        .setDeptId(post.getDeptId()).setOwnerEmployeeId(post.getOwnerEmployeeId());
                MapUtils.findAndThen(deptMap, post.getDeptId(), dept -> vo.setDeptName(dept.getName()));
                MapUtils.findAndThen(employeeMap, post.getOwnerEmployeeId(),
                        employee -> vo.setOwnerEmployeeName(employee.getName()));
            });
            MapUtils.findAndThen(recruitChannelMap, vo.getChannelId(),
                    channel -> vo.setChannelName(channel.getName()));
            // 2.2 拼接面试信息
            if (HrmRecruitCandidateStatusEnum.INTERVIEW_RELATED_STATUSES.contains(vo.getStatus())) {
                MapUtils.findAndThen(recruitInterviewMap, vo.getId(), interview -> {
                    vo.setInterviewId(interview.getId()).setInterviewType(interview.getType())
                            .setInterviewEmployeeId(interview.getInterviewEmployeeId())
                            .setOtherInterviewEmployeeIds(interview.getOtherInterviewEmployeeIds())
                            .setInterviewTime(interview.getInterviewTime()).setInterviewAddress(interview.getAddress())
                            .setInterviewResult(interview.getResult())
                            .setOtherInterviewEmployeeNames(convertList(
                                    convertList(interview.getOtherInterviewEmployeeIds(), employeeMap::get),
                                    HrmEmployeeDO::getName));
                    MapUtils.findAndThen(employeeMap, interview.getInterviewEmployeeId(),
                            employee -> vo.setInterviewEmployeeName(employee.getName()));
                });
            } else {
                vo.setStageNumber(0);
            }
            // 2.3 拼接员工和创建人信息
            MapUtils.findAndThen(candidateEmployeeMap, vo.getId(), employee -> vo.setEmployeeId(employee.getId()));
            MapUtils.findAndThen(userMap, NumberUtil.parseLong(vo.getCreator()),
                    creator -> vo.setCreatorName(creator.getNickname()));
        });
    }

}
