package com.cyt.center.module.hrm.controller.admin.recruit;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.post.type.HrmRecruitPostTypeRespVO;
import com.cyt.center.module.hrm.dal.dataobject.recruit.post.HrmRecruitPostTypeDO;
import com.cyt.center.module.hrm.service.recruit.post.HrmRecruitPostTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;

@Tag(name = "管理后台 - HRM 招聘职位类型")
@RestController
@RequestMapping("/hrm/recruit/post-type")
@Validated
public class HrmRecruitPostTypeController {

    @Resource
    private HrmRecruitPostTypeService recruitPostTypeService;

    @GetMapping("/list")
    @Operation(summary = "获得招聘职位类型列表")
    @Parameter(name = "status", description = "状态", example = "0")
    @SaCheckPermission("hrm:recruit:post:query")
    public Result<List<HrmRecruitPostTypeRespVO>> getRecruitPostTypeList(@RequestParam(value = "status", required = false) Integer status) {
        List<HrmRecruitPostTypeDO> postTypes = recruitPostTypeService.getRecruitPostTypeList(status);
        return success(BeanUtils.toBean(postTypes, HrmRecruitPostTypeRespVO.class));
    }

}
