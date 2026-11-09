package com.cyt.center.module.oa.controller.admin.seal;

import cn.dev33.satoken.annotation.SaCheckPermission;

import cn.hutool.core.collection.CollUtil;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.collection.MapUtils;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.oa.controller.admin.seal.vo.*;
import com.cyt.center.module.oa.dal.dataobject.seal.OaSealDO;
import com.cyt.center.module.oa.service.seal.OaSealService;
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
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Stream;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.*;

@Tag(name = "管理后台 - 印章")
@RestController
@RequestMapping("/oa/seal")
@Validated
public class OaSealController {

    @Resource
    private OaSealService sealService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建印章")
    @SaCheckPermission("oa:seal:create")
    public Result<Long> createSeal(@Valid @RequestBody OaSealSaveReqVO createReqVO) {
        return success(sealService.createSeal(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新印章")
    @SaCheckPermission("oa:seal:update")
    public Result<Boolean> updateSeal(@Valid @RequestBody OaSealSaveReqVO updateReqVO) {
        sealService.updateSeal(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除印章")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @SaCheckPermission("oa:seal:delete")
    public Result<Boolean> deleteSeal(@RequestParam("id") Long id) {
        sealService.deleteSeal(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得印章")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @SaCheckPermission("oa:seal:query")
    public Result<OaSealRespVO> getSeal(@RequestParam("id") Long id) {
        OaSealDO seal = sealService.validateSealExists(id);
        return success(buildSealRespVO(seal));
    }

    @GetMapping("/page")
    @Operation(summary = "获得印章分页")
    @SaCheckPermission("oa:seal:query")
    public Result<PageResult<OaSealRespVO>> getSealPage(@Valid OaSealPageReqVO pageReqVO) {
        PageResult<OaSealDO> pageResult = sealService.getSealPage(pageReqVO);
        return success(new PageResult<>(buildSealRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    // ==================== 拼接 VO ====================

    /**
     * 拼接印章详情
     *
     * @param seal 印章
     * @return 印章详情
     */
    private OaSealRespVO buildSealRespVO(OaSealDO seal) {
        if (seal == null) {
            return null;
        }
        return CollUtil.getFirst(buildSealRespVOList(Collections.singletonList(seal)));
    }

    /**
     * 拼接印章列表的保管人和部门信息
     *
     * @param seals 印章列表
     * @return 印章响应列表
     */
    private List<OaSealRespVO> buildSealRespVOList(List<OaSealDO> seals) {
        if (CollUtil.isEmpty(seals)) {
            return Collections.emptyList();
        }
        // 1. 批量查询保管人及部门
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertSet(seals, OaSealDO::getKeeperUserId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSetByFlatMap(seals, seal -> Stream.of(seal.getDeptId(), seal.getKeeperDeptId())));
        // 2. 转换台账并拼接展示字段
        return convertList(seals, seal -> {
            OaSealRespVO respVO = BeanUtils.toBean(seal, OaSealRespVO.class);
            MapUtils.findAndThen(deptMap, seal.getDeptId(), dept -> respVO.setDeptName(dept.getName()));
            MapUtils.findAndThen(userMap, seal.getKeeperUserId(), user -> respVO.setKeeperName(user.getNickname()));
            MapUtils.findAndThen(deptMap, seal.getKeeperDeptId(), dept -> respVO.setKeeperDeptName(dept.getName()));
            return respVO;
        });
    }
}
