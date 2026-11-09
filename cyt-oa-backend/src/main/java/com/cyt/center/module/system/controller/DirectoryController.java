package com.cyt.center.module.system.controller;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.module.system.model.cmd.DeptLeaderCmd;
import com.cyt.center.module.system.model.vo.EmployeeDetailVO;
import com.cyt.center.module.system.model.vo.OrgDeptNodeVO;
import com.cyt.center.module.system.model.vo.OrgMemberVO;
import com.cyt.center.module.system.service.EnterpriseDirectorySyncService;
import com.cyt.center.module.system.service.EnterpriseDirectorySyncService.SyncRun;
import com.cyt.center.module.system.service.OrgStructureService;
import com.cyt.center.module.system.service.SysDeptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通讯录同步入口。同步是低频高危操作，只开放给后台且逐次留痕。
 */
@Slf4j
@RestController
@RequestMapping("/admin-api/system/directory")
@RequiredArgsConstructor
public class DirectoryController {

    private static final String PERMISSION_SYNC = "system:directory:sync";
    private static final String PERMISSION_DEPT_QUERY = "system:dept:query";
    private static final String PERMISSION_DEPT_UPDATE_LEADER = "system:dept:update-leader";
    private static final String PERMISSION_USER_QUERY = "system:user:query";

    private final EnterpriseDirectorySyncService directorySync;
    private final SysDeptService deptService;
    private final OrgStructureService orgStructure;
    private final CurrentUser currentUser;

    /** 点一下就走：同步在后台线程跑，页面轮询 /sync-status，不占着请求线程两分钟。 */
    @PostMapping("/sync")
    public Result<SyncRun> sync() {
        currentUser.requirePermission(PERMISSION_SYNC);
        var actor = currentUser.require();
        return Result.ok(directorySync.start("manual", actor.name() + "#" + actor.userId()));
    }

    @GetMapping("/sync-status")
    public Result<SyncRun> syncStatus() {
        currentUser.requirePermission(PERMISSION_SYNC);
        return Result.ok(directorySync.latest());
    }

    @GetMapping("/runs")
    public Result<List<EnterpriseDirectorySyncService.SyncRun>> runs() {
        currentUser.requirePermission(PERMISSION_SYNC);
        return Result.ok(directorySync.runs());
    }

    /** 网关还没下发部门负责人，先由后台手工指定；下发之后由同步覆盖这一列。 */
    @PutMapping("/departments/leader")
    public Result<Void> updateDeptLeader(@Valid @RequestBody DeptLeaderCmd cmd) {
        currentUser.requirePermission(PERMISSION_DEPT_UPDATE_LEADER);
        deptService.updateLeader(cmd.getId(), cmd.getLeaderUserid());
        var actor = currentUser.require();
        log.info("部门负责人调整 actor={}, deptId={}, leaderUserid={}",
                actor.name() + "#" + actor.userId(), cmd.getId(), cmd.getLeaderUserid());
        return Result.ok();
    }

    /** 组织架构树：全部启用部门一次给全，前端按 parentId 组装。 */
    @GetMapping("/org-tree")
    public Result<List<OrgDeptNodeVO>> orgTree() {
        currentUser.requirePermission(PERMISSION_DEPT_QUERY);
        return Result.ok(orgStructure.tree());
    }

    /** 选中部门的成员，可选把子部门的人一起带出。 */
    @GetMapping("/org-tree/members")
    public Result<PageResult<OrgMemberVO>> members(@RequestParam("deptId") Long deptId,
                                                   @RequestParam(value = "includeChildren", defaultValue = "false") boolean includeChildren,
                                                   @RequestParam(value = "pageNo", defaultValue = "1") long pageNo,
                                                   @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        currentUser.requirePermission(PERMISSION_DEPT_QUERY);
        return Result.ok(orgStructure.members(deptId, includeChildren, pageNo, pageSize));
    }

    /** 员工详情：企微字段为主，本地档案只补企微没有的项。 */
    @GetMapping("/employees/{userId}")
    public Result<EmployeeDetailVO> employeeDetail(@PathVariable("userId") Long userId) {
        currentUser.requirePermission(PERMISSION_USER_QUERY);
        return Result.ok(orgStructure.detail(userId));
    }

}
