package com.cyt.center.module.hrm.service.employee.experience;

import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.trainingexperience.HrmEmployeeTrainingExperienceSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.experience.HrmEmployeeTrainingExperienceDO;
import com.cyt.center.module.hrm.dal.postgresql.employee.experience.HrmEmployeeTrainingExperienceMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.hutool.core.util.ObjectUtil.notEqual;
import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.framework.common.util.date.LocalDateTimeUtils.getDayBeginTime;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.EMPLOYEE_RESOURCE_BELONG_INVALID;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.EMPLOYEE_TRAINING_EXPERIENCE_NOT_EXISTS;
import static com.cyt.center.module.hrm.enums.LogRecordConstants.*;

/**
 * HRM 员工培训经历 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class HrmEmployeeTrainingExperienceServiceImpl implements HrmEmployeeTrainingExperienceService {

    @Resource
    private HrmEmployeeTrainingExperienceMapper trainingExperienceMapper;

    @Resource
    private HrmEmployeeService employeeService;

    @Override    public Long createTrainingExperience(HrmEmployeeTrainingExperienceSaveReqVO reqVO) {
        // 1. 校验员工
        employeeService.validateEmployeeExists(reqVO.getEmployeeId());

        // 2. 创建培训经历
        HrmEmployeeTrainingExperienceDO trainingExperience = BeanUtils.toBean(
                        reqVO, HrmEmployeeTrainingExperienceDO.class)
                .setStartTime(getDayBeginTime(reqVO.getStartTime()))
                .setEndTime(getDayBeginTime(reqVO.getEndTime()));
        trainingExperienceMapper.insert(trainingExperience);;
        return trainingExperience.getId();
    }

    @Override    public void updateTrainingExperience(HrmEmployeeTrainingExperienceSaveReqVO reqVO) {
        // 1. 校验员工和培训经历
        employeeService.validateEmployeeExists(reqVO.getEmployeeId());
        HrmEmployeeTrainingExperienceDO trainingExperience = validateTrainingExperienceExists(reqVO.getId());
        if (notEqual(trainingExperience.getEmployeeId(), reqVO.getEmployeeId())) {
            throw exception(EMPLOYEE_RESOURCE_BELONG_INVALID, "培训经历");
        }

        // 2. 更新培训经历
        trainingExperienceMapper.updateById(BeanUtils.toBean(reqVO, HrmEmployeeTrainingExperienceDO.class)
                .setEmployeeId(null)
                .setStartTime(getDayBeginTime(reqVO.getStartTime()))
                .setEndTime(getDayBeginTime(reqVO.getEndTime())));;
    }

    @Override    public void deleteTrainingExperience(Long id) {
        // 1. 校验员工培训经历存在
        HrmEmployeeTrainingExperienceDO trainingExperience = validateTrainingExperienceExists(id);

        // 2. 删除员工培训经历
        trainingExperienceMapper.deleteById(id);;
    }

    @Override
    public HrmEmployeeTrainingExperienceDO validateTrainingExperienceExists(Long id) {
        HrmEmployeeTrainingExperienceDO trainingExperience = trainingExperienceMapper.selectById(id);
        if (trainingExperience == null) {
            throw exception(EMPLOYEE_TRAINING_EXPERIENCE_NOT_EXISTS);
        }
        return trainingExperience;
    }

    @Override
    public List<HrmEmployeeTrainingExperienceDO> getTrainingExperienceListByEmployeeId(Long employeeId) {
        return trainingExperienceMapper.selectListByEmployeeId(employeeId);
    }

}
