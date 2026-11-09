package com.cyt.center.module.hrm.service.employee.experience;

import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.workexperience.HrmEmployeeWorkExperienceSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.experience.HrmEmployeeWorkExperienceDO;
import com.cyt.center.module.hrm.dal.mysql.employee.experience.HrmEmployeeWorkExperienceMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.hutool.core.util.ObjectUtil.notEqual;
import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.framework.common.util.date.LocalDateTimeUtils.getDayBeginTime;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.EMPLOYEE_RESOURCE_BELONG_INVALID;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.EMPLOYEE_WORK_EXPERIENCE_NOT_EXISTS;
import static com.cyt.center.module.hrm.enums.LogRecordConstants.*;

/**
 * HRM 员工工作经历 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class HrmEmployeeWorkExperienceServiceImpl implements HrmEmployeeWorkExperienceService {

    @Resource
    private HrmEmployeeWorkExperienceMapper workExperienceMapper;

    @Resource
    private HrmEmployeeService employeeService;

    @Override    public Long createWorkExperience(HrmEmployeeWorkExperienceSaveReqVO reqVO) {
        // 1. 校验员工
        employeeService.validateEmployeeExists(reqVO.getEmployeeId());

        // 2. 创建工作经历
        HrmEmployeeWorkExperienceDO workExperience = BeanUtils.toBean(reqVO, HrmEmployeeWorkExperienceDO.class)
                .setStartTime(getDayBeginTime(reqVO.getStartTime()))
                .setEndTime(getDayBeginTime(reqVO.getEndTime()));
        workExperienceMapper.insert(workExperience);;
        return workExperience.getId();
    }

    @Override    public void updateWorkExperience(HrmEmployeeWorkExperienceSaveReqVO reqVO) {
        // 1. 校验员工和工作经历
        employeeService.validateEmployeeExists(reqVO.getEmployeeId());
        HrmEmployeeWorkExperienceDO workExperience = validateWorkExperienceExists(reqVO.getId());
        if (notEqual(workExperience.getEmployeeId(), reqVO.getEmployeeId())) {
            throw exception(EMPLOYEE_RESOURCE_BELONG_INVALID, "工作经历");
        }

        // 2. 更新工作经历
        workExperienceMapper.updateById(BeanUtils.toBean(reqVO, HrmEmployeeWorkExperienceDO.class)
                .setEmployeeId(null)
                .setStartTime(getDayBeginTime(reqVO.getStartTime()))
                .setEndTime(getDayBeginTime(reqVO.getEndTime())));;
    }

    @Override    public void deleteWorkExperience(Long id) {
        // 1. 校验员工工作经历存在
        HrmEmployeeWorkExperienceDO workExperience = validateWorkExperienceExists(id);

        // 2. 删除员工工作经历
        workExperienceMapper.deleteById(id);;
    }

    @Override
    public HrmEmployeeWorkExperienceDO validateWorkExperienceExists(Long id) {
        HrmEmployeeWorkExperienceDO workExperience = workExperienceMapper.selectById(id);
        if (workExperience == null) {
            throw exception(EMPLOYEE_WORK_EXPERIENCE_NOT_EXISTS);
        }
        return workExperience;
    }

    @Override
    public List<HrmEmployeeWorkExperienceDO> getWorkExperienceListByEmployeeId(Long employeeId) {
        return workExperienceMapper.selectListByEmployeeId(employeeId);
    }

}
