package com.cyt.center.module.bpm.service;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.framework.security.core.util.SecurityFrameworkUtils;
import com.cyt.center.module.bpm.framework.flow.core.BpmPermissionHandler;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowCombine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.HisTask;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.entity.User;
import org.dromara.warm.flow.core.enums.FlowStatus;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.enums.UserType;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 审批任务的查询与办理。权限校验由引擎 checkAuth 完成（当前用户须是任务审批人），
 * 这里只把请求转成引擎调用，并把引擎异常转成可展示的业务异常。
 */
@Service
public class BpmTaskService {

    public List<Task> todoList() {
        String uid = requireLoginUserId();
        List<User> users = FlowEngine.userService()
                .listByProcessedBys(null, uid, UserType.APPROVAL.getKey());
        List<Task> tasks = new ArrayList<>();
        for (User user : users) {
            Task task = FlowEngine.taskService().getById(user.getAssociated());
            if (task != null) {
                enrich(task);
                tasks.add(task);
            }
        }
        tasks.sort(Comparator.comparing(Task::getId, Comparator.reverseOrder()));
        return tasks;
    }

    /** 任务表不落 business_id 与流程名，从实例与定义补齐展示字段。 */
    private void enrich(Task task) {
        Instance instance = FlowEngine.insService().getById(task.getInstanceId());
        if (instance != null) {
            task.setBusinessId(instance.getBusinessId());
        }
        Definition definition = FlowEngine.defService().getById(task.getDefinitionId());
        if (definition != null) {
            task.setFlowName(definition.getFlowName());
        }
    }

    public List<HisTask> doneList() {
        String uid = requireLoginUserId();
        List<HisTask> hisTasks = FlowEngine.hisTaskService()
                .list(FlowEngine.newHisTask().setApprover(uid));
        for (HisTask hisTask : hisTasks) {
            Definition definition = FlowEngine.defService().getById(hisTask.getDefinitionId());
            if (definition != null) {
                hisTask.setFlowName(definition.getFlowName());
            }
            Instance instance = FlowEngine.insService().getById(hisTask.getInstanceId());
            if (instance != null) {
                hisTask.setBusinessId(instance.getBusinessId());
            }
        }
        hisTasks.sort(Comparator.comparing(HisTask::getId, Comparator.reverseOrder()));
        return hisTasks;
    }

    /** 已办条数：不补齐流程名与业务编号，只给徽标和足迹计数用。 */
    public int doneCount() {
        String uid = requireLoginUserId();
        return FlowEngine.hisTaskService().list(FlowEngine.newHisTask().setApprover(uid)).size();
    }

    public List<Instance> myInstanceList() {
        String uid = requireLoginUserId();
        List<Instance> instances = FlowEngine.insService()
                .list(FlowEngine.newIns().setCreateBy(uid));
        for (Instance instance : instances) {
            Definition definition = FlowEngine.defService().getById(instance.getDefinitionId());
            if (definition != null) {
                instance.setFlowName(definition.getFlowName());
            }
        }
        instances.sort(Comparator.comparing(Instance::getId, Comparator.reverseOrder()));
        return instances;
    }

    public String flowCodeOf(Long definitionId) {
        Definition definition = FlowEngine.defService().getById(definitionId);
        return definition == null ? null : definition.getFlowCode();
    }

    public void approve(Long taskId, String message, Map<String, Object> variables) {
        FlowParams params = FlowParams.build()
                .skipType(SkipType.PASS.getKey())
                .handler(requireLoginUserId())
                .permissionFlag(currentPermissions())
                .message(message)
                .variable(variables == null ? Map.of() : variables);
        doSkip(taskId, params);
    }

    public void reject(Long taskId, String reason) {
        Task task = requireTask(taskId);
        Node endNode = endNodeOf(task.getDefinitionId());
        FlowParams params = FlowParams.build()
                .skipType(SkipType.REJECT.getKey())
                .nodeCode(endNode.getNodeCode())
                .flowStatus(FlowStatus.REJECT.getKey())
                .handler(requireLoginUserId())
                .permissionFlag(currentPermissions())
                .message(reason);
        doSkip(taskId, params);
    }

    public void transfer(Long taskId, Long targetUserId) {
        requireTask(taskId);
        try {
            FlowEngine.taskService().transfer(taskId, FlowParams.build()
                    .handler(requireLoginUserId())
                    .permissionFlag(currentPermissions())
                    .addHandlers(List.of(String.valueOf(targetUserId))));
        } catch (FlowException ex) {
            throw new BizException(ex.getMessage());
        }
    }

    private void doSkip(Long taskId, FlowParams params) {
        requireTask(taskId);
        try {
            FlowEngine.taskService().skip(taskId, params);
        } catch (FlowException ex) {
            throw new BizException(ex.getMessage());
        }
    }

    private Task requireTask(Long taskId) {
        Task task = FlowEngine.taskService().getById(taskId);
        if (task == null) {
            throw new BizException("待办任务不存在或已办理");
        }
        return task;
    }

    private Node endNodeOf(Long definitionId) {
        FlowCombine flowCombine = FlowEngine.defService().getFlowCombineNoDef(definitionId);
        return flowCombine.getAllNodes().stream()
                .filter(node -> NodeType.isEnd(node.getNodeType()))
                .findFirst()
                .orElseThrow(() -> new BizException("流程缺少结束节点"));
    }

    private String requireLoginUserId() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw new BizException("请先登录");
        }
        return String.valueOf(userId);
    }

    private List<String> currentPermissions() {
        PermissionHandler handler = FlowEngine.permissionHandler();
        if (handler instanceof BpmPermissionHandler bpmPermissionHandler) {
            return bpmPermissionHandler.permissions();
        }
        return List.of(requireLoginUserId());
    }

}
