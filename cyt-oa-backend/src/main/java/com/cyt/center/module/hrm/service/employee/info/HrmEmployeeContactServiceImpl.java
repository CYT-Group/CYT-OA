package com.cyt.center.module.hrm.service.employee.info;

import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.contact.HrmEmployeeContactSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeContactDO;
import com.cyt.center.module.hrm.dal.mysql.employee.info.HrmEmployeeContactMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.hutool.core.util.ObjectUtil.notEqual;
import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.EMPLOYEE_CONTACT_NOT_EXISTS;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.EMPLOYEE_RESOURCE_BELONG_INVALID;
import static com.cyt.center.module.hrm.enums.LogRecordConstants.*;

/**
 * HRM 员工联系人 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class HrmEmployeeContactServiceImpl implements HrmEmployeeContactService {

    @Resource
    private HrmEmployeeContactMapper contactMapper;
    @Resource
    private HrmEmployeeService employeeService;

    @Override    public Long createContact(HrmEmployeeContactSaveReqVO reqVO) {
        // 1. 校验员工
        employeeService.validateEmployeeExists(reqVO.getEmployeeId());

        // 2. 创建联系人
        HrmEmployeeContactDO contact = BeanUtils.toBean(reqVO, HrmEmployeeContactDO.class);
        contactMapper.insert(contact);;
        return contact.getId();
    }

    @Override    public void updateContact(HrmEmployeeContactSaveReqVO reqVO) {
        // 1. 校验员工和联系人
        employeeService.validateEmployeeExists(reqVO.getEmployeeId());
        HrmEmployeeContactDO contact = validateContactExists(reqVO.getId());
        if (notEqual(contact.getEmployeeId(), reqVO.getEmployeeId())) {
            throw exception(EMPLOYEE_RESOURCE_BELONG_INVALID, "联系人");
        }

        // 2. 更新联系人
        contactMapper.updateById(BeanUtils.toBean(reqVO, HrmEmployeeContactDO.class)
                .setEmployeeId(null));;
    }

    @Override    public void deleteContact(Long id) {
        // 1. 校验员工联系人存在
        HrmEmployeeContactDO contact = validateContactExists(id);

        // 2. 删除员工联系人
        contactMapper.deleteById(id);;
    }

    @Override
    public HrmEmployeeContactDO validateContactExists(Long id) {
        HrmEmployeeContactDO contact = contactMapper.selectById(id);
        if (contact == null) {
            throw exception(EMPLOYEE_CONTACT_NOT_EXISTS);
        }
        return contact;
    }

    @Override
    public List<HrmEmployeeContactDO> getContactListByEmployeeId(Long employeeId) {
        return contactMapper.selectListByEmployeeId(employeeId);
    }

}
