package com.cyt.center.module.hrm.controller.admin.portal.employee;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.contact.HrmEmployeeContactRespVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeContactDO;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeContactService;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - HRM 员工端联系人")
@RestController
@RequestMapping("/hrm/portal/employee/contact")
@Validated
public class HrmPortalEmployeeContactController {

    @Resource
    private HrmEmployeeContactService contactService;
    @Resource
    private HrmEmployeeService employeeService;

    @GetMapping("/list")
    @Operation(summary = "获得我的联系人列表")
    @SaCheckPermission("hrm:portal:query")
    public Result<List<HrmEmployeeContactRespVO>> getContactList() {
        Long employeeId = employeeService.validateEmployeeBySelf(getLoginUserId()).getId();
        List<HrmEmployeeContactDO> contacts = contactService.getContactListByEmployeeId(employeeId);
        return success(BeanUtils.toBean(contacts, HrmEmployeeContactRespVO.class));
    }

}
