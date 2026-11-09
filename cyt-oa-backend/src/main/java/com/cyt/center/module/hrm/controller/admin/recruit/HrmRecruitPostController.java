package com.cyt.center.module.hrm.controller.admin.recruit;

import cn.dev33.satoken.annotation.SaCheckPermission;

import cn.hutool.core.collection.CollUtil;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.collection.MapUtils;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.framework.ip.core.utils.AreaUtils;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.HrmRecruitStatusCountRespVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.post.HrmRecruitPostPageReqVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.post.HrmRecruitPostRespVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.post.HrmRecruitPostSaveReqVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.post.HrmRecruitPostStatusReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.dal.dataobject.recruit.post.HrmRecruitPostDO;
import com.cyt.center.module.hrm.dal.dataobject.recruit.post.HrmRecruitPostTypeDO;
import com.cyt.center.module.hrm.enums.recruit.post.HrmRecruitPostStatusEnum;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.hrm.service.recruit.candidate.HrmRecruitCandidateService;
import com.cyt.center.module.hrm.service.recruit.post.HrmRecruitPostService;
import com.cyt.center.module.hrm.service.recruit.post.HrmRecruitPostTypeService;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertList;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertSet;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertSetByFlatMap;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.getFirst;
import static com.cyt.center.core.result.Result.success;

@Tag(name = "管理后台 - HRM 招聘职位")
@RestController
@RequestMapping("/hrm/recruit/post")
@Validated
public class HrmRecruitPostController {

    @Resource
    private HrmRecruitPostService recruitPostService;
    @Resource
    private HrmRecruitCandidateService recruitCandidateService;
    @Resource
    private HrmEmployeeService employeeService;
    @Resource
    private HrmRecruitPostTypeService recruitPostTypeService;

    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建招聘职位")
    @SaCheckPermission("hrm:recruit:post:create")
    public Result<Long> createRecruitPost(@Valid @RequestBody HrmRecruitPostSaveReqVO createReqVO) {
        return success(recruitPostService.createRecruitPost(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新招聘职位")
    @SaCheckPermission("hrm:recruit:post:update")
    public Result<Boolean> updateRecruitPost(@Valid @RequestBody HrmRecruitPostSaveReqVO updateReqVO) {
        recruitPostService.updateRecruitPost(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新招聘职位状态")
    @SaCheckPermission("hrm:recruit:post:update")
    public Result<Boolean> updateRecruitPostStatus(@Valid @RequestBody HrmRecruitPostStatusReqVO reqVO) {
        recruitPostService.updateRecruitPostStatus(reqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得招聘职位")
    @Parameter(name = "id", description = "招聘职位编号", required = true, example = "1024")
    @SaCheckPermission("hrm:recruit:post:query")
    public Result<HrmRecruitPostRespVO> getRecruitPost(@RequestParam("id") Long id) {
        return success(buildRecruitPostRespVO(recruitPostService.getRecruitPost(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得招聘职位分页")
    @SaCheckPermission("hrm:recruit:post:query")
    public Result<PageResult<HrmRecruitPostRespVO>> getRecruitPostPage(
            @Validated HrmRecruitPostPageReqVO pageReqVO) {
        PageResult<HrmRecruitPostDO> pageResult = recruitPostService.getRecruitPostPage(pageReqVO);
        return success(new PageResult<>(buildRecruitPostRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得招聘职位精简列表")
    @SaCheckPermission("hrm:recruit:post:query")
    public Result<List<HrmRecruitPostRespVO>> getRecruitPostSimpleList() {
        return success(convertList(recruitPostService.getRecruitPostSimpleList(), post ->
                new HrmRecruitPostRespVO().setId(post.getId()).setPostName(post.getPostName())
                        .setStatus(post.getStatus())));
    }

    @GetMapping("/status-count")
    @Operation(summary = "获得招聘职位状态数量")
    @SaCheckPermission("hrm:recruit:post:query")
    public Result<List<HrmRecruitStatusCountRespVO>> getRecruitPostStatusCount(
            @Validated HrmRecruitPostPageReqVO pageReqVO) {
        Map<Integer, Long> countMap = recruitPostService.getRecruitPostStatusCount(pageReqVO);
        return success(convertList(Arrays.asList(HrmRecruitPostStatusEnum.values()),
                status -> new HrmRecruitStatusCountRespVO(status.getStatus(),
                        countMap.getOrDefault(status.getStatus(), 0L))));
    }

    // ==================== 拼接 VO ====================

    private HrmRecruitPostRespVO buildRecruitPostRespVO(HrmRecruitPostDO recruitPost) {
        if (recruitPost == null) {
            return null;
        }
        return getFirst(buildRecruitPostRespVOList(Collections.singletonList(recruitPost)));
    }

    private List<HrmRecruitPostRespVO> buildRecruitPostRespVOList(List<HrmRecruitPostDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 1.1 员工信息
        Set<Long> employeeIds = convertSet(list, HrmRecruitPostDO::getOwnerEmployeeId);
        employeeIds.addAll(convertSetByFlatMap(list, HrmRecruitPostDO::getInterviewEmployeeIds,
                Collection::stream));
        Map<Long, HrmEmployeeDO> employeeMap = employeeService.getEmployeeMap(employeeIds);
        // 1.2 部门信息
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(list, HrmRecruitPostDO::getDeptId));
        // 1.3 职位类型信息
        Map<Long, HrmRecruitPostTypeDO> postTypeMap = recruitPostTypeService.getRecruitPostTypeMap(
                convertSet(list, HrmRecruitPostDO::getPostTypeId));
        // 1.4 已入职人数
        Map<Long, Long> joinedCountMap = recruitCandidateService.getJoinedCandidateCountMap(
                convertSet(list, HrmRecruitPostDO::getId));
        // 2. 拼接响应
        return BeanUtils.toBean(list, HrmRecruitPostRespVO.class, vo -> {
            vo.setAreaName(AreaUtils.format(vo.getAreaId()));
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            MapUtils.findAndThen(employeeMap, vo.getOwnerEmployeeId(),
                    employee -> vo.setOwnerEmployeeName(employee.getName()));
            vo.setInterviewEmployeeNames(convertList(vo.getInterviewEmployeeIds(),
                    employeeId -> employeeMap.containsKey(employeeId)
                            ? employeeMap.get(employeeId).getName() : null));
            MapUtils.findAndThen(postTypeMap, vo.getPostTypeId(),
                    postType -> vo.setPostTypeName(postType.getName()));
            fillRecruitPostProgress(vo, joinedCountMap.getOrDefault(vo.getId(), 0L));
        });
    }

    private void fillRecruitPostProgress(HrmRecruitPostRespVO recruitPost, Long hasEntryNum) {
        recruitPost.setHasEntryNum(hasEntryNum);
        if (recruitPost.getRecruitNum() != null && recruitPost.getRecruitNum() != 0) {
            recruitPost.setRecruitSchedule(BigDecimal.valueOf(hasEntryNum).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(recruitPost.getRecruitNum()), 2, RoundingMode.HALF_UP));
        }
    }

}
