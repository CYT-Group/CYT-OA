package com.cyt.center.module.hrm.controller.admin.employee;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.contact.HrmEmployeeContactRespVO;
import com.cyt.center.module.hrm.controller.admin.employee.vo.contact.HrmEmployeeContactSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeContactDO;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeContactService;
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

import java.util.List;

import static com.cyt.center.core.result.Result.success;

@Tag(name = "管理后台 - HRM 员工联系人")
@RestController
@RequestMapping("/hrm/employee/contact")
@Validated
public class HrmEmployeeContactController {

    @Resource
    private HrmEmployeeContactService contactService;

    @GetMapping("/list")
    @Operation(summary = "获得员工联系人列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:query")
    public Result<List<HrmEmployeeContactRespVO>> getContactList(
            @RequestParam("employeeId") Long employeeId) {
        List<HrmEmployeeContactDO> contacts = contactService.getContactListByEmployeeId(employeeId);
        return success(BeanUtils.toBean(contacts, HrmEmployeeContactRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工联系人")
    @SaCheckPermission("hrm:employee:update")
    public Result<Long> createContact(@Valid @RequestBody HrmEmployeeContactSaveReqVO reqVO) {
        return success(contactService.createContact(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工联系人")
    @SaCheckPermission("hrm:employee:update")
    public Result<Boolean> updateContact(@Valid @RequestBody HrmEmployeeContactSaveReqVO reqVO) {
        contactService.updateContact(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工联系人")
    @Parameter(name = "id", description = "联系人编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:delete")
    public Result<Boolean> deleteContact(@RequestParam("id") Long id) {
        contactService.deleteContact(id);
        return success(true);
    }

}
