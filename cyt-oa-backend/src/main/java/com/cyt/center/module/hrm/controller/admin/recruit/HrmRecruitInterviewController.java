package com.cyt.center.module.hrm.controller.admin.recruit;

import cn.dev33.satoken.annotation.SaCheckPermission;

import cn.hutool.core.collection.CollUtil;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.collection.MapUtils;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.interview.HrmRecruitInterviewRespVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.interview.HrmRecruitInterviewResultReqVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.interview.HrmRecruitInterviewSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.dal.dataobject.recruit.candidate.HrmRecruitInterviewDO;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.hrm.service.recruit.candidate.HrmRecruitInterviewService;
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

@Tag(name = "管理后台 - HRM 招聘面试")
@RestController
@RequestMapping("/hrm/recruit/interview")
@Validated
public class HrmRecruitInterviewController {

    @Resource
    private HrmRecruitInterviewService recruitInterviewService;
    @Resource
    private HrmEmployeeService employeeService;

    @PostMapping("/create")
    @Operation(summary = "创建招聘面试")
    @SaCheckPermission("hrm:recruit:interview:create")
    public Result<Long> createRecruitInterview(@Valid @RequestBody HrmRecruitInterviewSaveReqVO createReqVO) {
        return success(recruitInterviewService.createRecruitInterview(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新招聘面试")
    @SaCheckPermission("hrm:recruit:interview:update")
    public Result<Boolean> updateRecruitInterview(@Valid @RequestBody HrmRecruitInterviewSaveReqVO updateReqVO) {
        recruitInterviewService.updateRecruitInterview(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-result")
    @Operation(summary = "更新招聘面试结果")
    @SaCheckPermission("hrm:recruit:interview:update")
    public Result<Boolean> updateRecruitInterviewResult(@Valid @RequestBody HrmRecruitInterviewResultReqVO reqVO) {
        recruitInterviewService.updateRecruitInterviewResult(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除招聘面试")
    @Parameter(name = "id", description = "面试编号", required = true, example = "1024")
    @SaCheckPermission("hrm:recruit:interview:delete")
    public Result<Boolean> deleteRecruitInterview(@RequestParam("id") Long id) {
        recruitInterviewService.deleteRecruitInterview(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得招聘面试")
    @Parameter(name = "id", description = "面试编号", required = true, example = "1024")
    @SaCheckPermission("hrm:recruit:interview:query")
    public Result<HrmRecruitInterviewRespVO> getRecruitInterview(@RequestParam("id") Long id) {
        return success(buildRecruitInterviewRespVO(recruitInterviewService.getRecruitInterview(id)));
    }

    @GetMapping("/list-by-candidate")
    @Operation(summary = "获得候选人的招聘面试列表")
    @Parameter(name = "candidateId", description = "候选人编号", required = true, example = "1024")
    @SaCheckPermission("hrm:recruit:interview:query")
    public Result<List<HrmRecruitInterviewRespVO>> getRecruitInterviewListByCandidate(
            @RequestParam("candidateId") Long candidateId) {
        return success(buildRecruitInterviewRespVOList(
                recruitInterviewService.getRecruitInterviewListByCandidateId(candidateId)));
    }

    // ==================== 拼接 VO ====================

    private HrmRecruitInterviewRespVO buildRecruitInterviewRespVO(HrmRecruitInterviewDO recruitInterview) {
        if (recruitInterview == null) {
            return null;
        }
        return getFirst(buildRecruitInterviewRespVOList(Collections.singletonList(recruitInterview)));
    }

    private List<HrmRecruitInterviewRespVO> buildRecruitInterviewRespVOList(List<HrmRecruitInterviewDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 1. 获得面试官信息
        Set<Long> employeeIds = convertSet(list, HrmRecruitInterviewDO::getInterviewEmployeeId);
        employeeIds.addAll(convertSetByFlatMap(list, HrmRecruitInterviewDO::getOtherInterviewEmployeeIds,
                Collection::stream));
        Map<Long, HrmEmployeeDO> employeeMap = employeeService.getEmployeeMap(employeeIds);

        // 2. 拼接响应
        return BeanUtils.toBean(list, HrmRecruitInterviewRespVO.class, vo -> {
            MapUtils.findAndThen(employeeMap, vo.getInterviewEmployeeId(),
                    employee -> vo.setInterviewEmployeeName(employee.getName()));
            vo.setOtherInterviewEmployeeNames(convertList(
                    convertList(vo.getOtherInterviewEmployeeIds(), employeeMap::get),
                    HrmEmployeeDO::getName));
        });
    }

}
