package com.cyt.center.module.bpm.controller.admin.processinstance;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.module.bpm.api.task.BpmProcessInstanceApi;
import com.cyt.center.module.bpm.controller.admin.processinstance.vo.BpmProcessInstanceCancelReqVO;
import com.cyt.center.module.bpm.controller.admin.processinstance.vo.BpmProcessInstanceRespVO;
import com.cyt.center.module.bpm.service.BpmTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.dromara.warm.flow.core.entity.Instance;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 流程实例")
@RestController
@RequestMapping("/bpm/process-instance")
@Validated
public class BpmProcessInstanceController {

    @Resource
    private BpmTaskService bpmTaskService;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @GetMapping("/my-page")
    @Operation(summary = "我发起的流程实例分页")
    @SaCheckLogin
    public Result<PageResult<BpmProcessInstanceRespVO>> getMyPage(@Valid PageParam pageParam) {
        List<Instance> instances = bpmTaskService.myInstanceList();
        int from = (pageParam.getPageNo() - 1) * pageParam.getPageSize();
        if (from >= instances.size()) {
            return success(new PageResult<>(List.of(), (long) instances.size()));
        }
        List<Instance> pageList = instances.subList(from,
                Math.min(from + pageParam.getPageSize(), instances.size()));
        List<BpmProcessInstanceRespVO> voList = new ArrayList<>(pageList.size());
        for (Instance instance : pageList) {
            BpmProcessInstanceRespVO vo = new BpmProcessInstanceRespVO();
            vo.setId(instance.getId());
            vo.setFlowName(instance.getFlowName());
            vo.setFlowCode(bpmTaskService.flowCodeOf(instance.getDefinitionId()));
            vo.setBusinessId(instance.getBusinessId());
            vo.setFlowStatus(instance.getFlowStatus());
            vo.setNodeName(instance.getNodeName());
            vo.setCreateTime(instance.getCreateTime() == null ? null
                    : instance.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            voList.add(vo);
        }
        return success(new PageResult<>(voList, (long) instances.size()));
    }

    @PutMapping("/cancel")
    @Operation(summary = "发起人撤销流程")
    @SaCheckLogin
    public Result<Boolean> cancel(@Valid @RequestBody BpmProcessInstanceCancelReqVO reqVO) {
        processInstanceApi.cancelProcessInstanceByStartUser(getLoginUserId(), reqVO.getId(), reqVO.getReason());
        return success(true);
    }

}
