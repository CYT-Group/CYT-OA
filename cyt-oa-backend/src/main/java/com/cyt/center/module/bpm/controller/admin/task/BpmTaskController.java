package com.cyt.center.module.bpm.controller.admin.task;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.module.bpm.controller.admin.task.vo.BpmTaskApproveReqVO;
import com.cyt.center.module.bpm.controller.admin.task.vo.BpmTaskDoneRespVO;
import com.cyt.center.module.bpm.controller.admin.task.vo.BpmTaskRejectReqVO;
import com.cyt.center.module.bpm.controller.admin.task.vo.BpmTaskTodoRespVO;
import com.cyt.center.module.bpm.controller.admin.task.vo.BpmTaskTransferReqVO;
import com.cyt.center.module.bpm.service.BpmTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.dromara.warm.flow.core.entity.HisTask;
import org.dromara.warm.flow.core.entity.Task;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.cyt.center.core.result.Result.success;

@Tag(name = "管理后台 - 审批任务")
@RestController
@RequestMapping("/bpm/task")
@Validated
public class BpmTaskController {

    @Resource
    private BpmTaskService bpmTaskService;

    @GetMapping("/todo-page")
    @Operation(summary = "待办任务分页")
    @SaCheckLogin
    public Result<PageResult<BpmTaskTodoRespVO>> getTodoPage(@Valid PageParam pageParam) {
        List<Task> tasks = bpmTaskService.todoList();
        List<Task> pageList = subList(tasks, pageParam.getPageNo(), pageParam.getPageSize());
        List<BpmTaskTodoRespVO> voList = new ArrayList<>(pageList.size());
        for (Task task : pageList) {
            BpmTaskTodoRespVO vo = new BpmTaskTodoRespVO();
            vo.setId(task.getId());
            vo.setInstanceId(task.getInstanceId());
            vo.setFlowName(task.getFlowName());
            vo.setFlowCode(bpmTaskService.flowCodeOf(task.getDefinitionId()));
            vo.setBusinessId(task.getBusinessId());
            vo.setNodeCode(task.getNodeCode());
            vo.setNodeName(task.getNodeName());
            vo.setCreateTime(toLocalDateTime(task.getCreateTime()));
            voList.add(vo);
        }
        return success(new PageResult<>(voList, (long) tasks.size()));
    }

    @GetMapping("/done-page")
    @Operation(summary = "已办任务分页")
    @SaCheckLogin
    public Result<PageResult<BpmTaskDoneRespVO>> getDonePage(@Valid PageParam pageParam) {
        List<HisTask> hisTasks = bpmTaskService.doneList();
        List<HisTask> pageList = subList(hisTasks, pageParam.getPageNo(), pageParam.getPageSize());
        List<BpmTaskDoneRespVO> voList = new ArrayList<>(pageList.size());
        for (HisTask hisTask : pageList) {
            BpmTaskDoneRespVO vo = new BpmTaskDoneRespVO();
            vo.setId(hisTask.getId());
            vo.setInstanceId(hisTask.getInstanceId());
            vo.setFlowName(hisTask.getFlowName());
            vo.setFlowCode(bpmTaskService.flowCodeOf(hisTask.getDefinitionId()));
            vo.setBusinessId(hisTask.getBusinessId());
            vo.setNodeCode(hisTask.getNodeCode());
            vo.setNodeName(hisTask.getNodeName());
            vo.setTargetNodeCode(hisTask.getTargetNodeCode());
            vo.setSkipType(hisTask.getSkipType());
            vo.setFlowStatus(hisTask.getFlowStatus());
            vo.setMessage(hisTask.getMessage());
            vo.setCreateTime(toLocalDateTime(hisTask.getCreateTime()));
            voList.add(vo);
        }
        return success(new PageResult<>(voList, (long) hisTasks.size()));
    }

    @PutMapping("/approve")
    @Operation(summary = "审批通过")
    @SaCheckLogin
    public Result<Boolean> approve(@Valid @RequestBody BpmTaskApproveReqVO reqVO) {
        bpmTaskService.approve(reqVO.getId(), reqVO.getMessage(), reqVO.getVariables());
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "审批退回（流程结束，单据状态为审批不通过）")
    @SaCheckLogin
    public Result<Boolean> reject(@Valid @RequestBody BpmTaskRejectReqVO reqVO) {
        bpmTaskService.reject(reqVO.getId(), reqVO.getMessage());
        return success(true);
    }

    @PutMapping("/transfer")
    @Operation(summary = "任务转办")
    @SaCheckLogin
    public Result<Boolean> transfer(@Valid @RequestBody BpmTaskTransferReqVO reqVO) {
        bpmTaskService.transfer(reqVO.getId(), reqVO.getTargetUserId());
        return success(true);
    }

    private <T> List<T> subList(List<T> source, Integer pageNo, Integer pageSize) {
        int from = (pageNo - 1) * pageSize;
        if (from >= source.size()) {
            return List.of();
        }
        return source.subList(from, Math.min(from + pageSize, source.size()));
    }

    private static java.time.LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

}
