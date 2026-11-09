package com.cyt.center.module.hrm.service.employee.experience;

import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.educationexperience.HrmEmployeeEducationExperienceSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.experience.HrmEmployeeEducationExperienceDO;
import com.cyt.center.module.hrm.dal.mysql.employee.experience.HrmEmployeeEducationExperienceMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.hutool.core.util.ObjectUtil.notEqual;
import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.framework.common.util.date.LocalDateTimeUtils.getDayBeginTime;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.EMPLOYEE_EDUCATION_EXPERIENCE_NOT_EXISTS;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.EMPLOYEE_RESOURCE_BELONG_INVALID;
import static com.cyt.center.module.hrm.enums.LogRecordConstants.*;

/**
 * HRM 员工教育经历 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class HrmEmployeeEducationExperienceServiceImpl implements HrmEmployeeEducationExperienceService {

    @Resource
    private HrmEmployeeEducationExperienceMapper educationExperienceMapper;
    @Resource
    private HrmEmployeeService employeeService;

    @Override    public Long createEducationExperience(HrmEmployeeEducationExperienceSaveReqVO reqVO) {
        // 1. 校验员工
        employeeService.validateEmployeeExists(reqVO.getEmployeeId());

        // 2. 创建教育经历
        HrmEmployeeEducationExperienceDO educationExperience = BeanUtils.toBean(
                        reqVO, HrmEmployeeEducationExperienceDO.class)
                .setAdmissionTime(getDayBeginTime(reqVO.getAdmissionTime()))
                .setGraduationTime(getDayBeginTime(reqVO.getGraduationTime()));
        educationExperienceMapper.insert(educationExperience);;
        return educationExperience.getId();
    }

    @Override    public void updateEducationExperience(HrmEmployeeEducationExperienceSaveReqVO reqVO) {
        // 1. 校验员工和教育经历
        employeeService.validateEmployeeExists(reqVO.getEmployeeId());
        HrmEmployeeEducationExperienceDO educationExperience = validateEducationExperienceExists(reqVO.getId());
        if (notEqual(educationExperience.getEmployeeId(), reqVO.getEmployeeId())) {
            throw exception(EMPLOYEE_RESOURCE_BELONG_INVALID, "教育经历");
        }

        // 2. 更新教育经历
        educationExperienceMapper.updateById(BeanUtils.toBean(reqVO, HrmEmployeeEducationExperienceDO.class)
                .setEmployeeId(null)
                .setAdmissionTime(getDayBeginTime(reqVO.getAdmissionTime()))
                .setGraduationTime(getDayBeginTime(reqVO.getGraduationTime())));;
    }

    @Override    public void deleteEducationExperience(Long id) {
        // 1. 校验员工教育经历存在
        HrmEmployeeEducationExperienceDO educationExperience = validateEducationExperienceExists(id);

        // 2. 删除员工教育经历
        educationExperienceMapper.deleteById(id);;
    }

    @Override
    public HrmEmployeeEducationExperienceDO validateEducationExperienceExists(Long id) {
        HrmEmployeeEducationExperienceDO educationExperience = educationExperienceMapper.selectById(id);
        if (educationExperience == null) {
            throw exception(EMPLOYEE_EDUCATION_EXPERIENCE_NOT_EXISTS);
        }
        return educationExperience;
    }

    @Override
    public List<HrmEmployeeEducationExperienceDO> getEducationExperienceListByEmployeeId(Long employeeId) {
        return educationExperienceMapper.selectListByEmployeeId(employeeId);
    }

}
