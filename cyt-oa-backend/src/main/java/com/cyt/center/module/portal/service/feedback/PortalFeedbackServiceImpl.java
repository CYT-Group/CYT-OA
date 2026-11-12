package com.cyt.center.module.portal.service.feedback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.module.portal.controller.admin.feedback.vo.PortalFeedbackSubmitReqVO;
import com.cyt.center.module.portal.dal.dataobject.feedback.PortalFeedbackDO;
import com.cyt.center.module.portal.dal.postgresql.feedback.PortalFeedbackMapper;
import com.cyt.center.module.portal.service.support.PortalOperator;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 员工反馈用例实现（对齐 Portal 的 application/feedback.py）。
 */
@Service
public class PortalFeedbackServiceImpl implements PortalFeedbackService {

    @Resource
    private PortalFeedbackMapper feedbackMapper;
    @Resource
    private PortalOperatorResolver operators;

    @Override
    public PortalFeedbackDO submit(Long userId, PortalFeedbackSubmitReqVO reqVO) {
        PortalOperator operator = operators.resolve(userId);
        PortalFeedbackDO fb = new PortalFeedbackDO();
        fb.setUsername(operator.username());
        fb.setReporterName(operator.displayNameOrUsername());
        fb.setDepartment(operator.department());
        fb.setCategory(reqVO.getCategory().trim());
        fb.setSubCategory(blankToNull(reqVO.getSubCategory()));
        fb.setBrand(blankToNull(reqVO.getBrand()));
        fb.setProductName(blankToNull(reqVO.getProductName()));
        fb.setRawMessage(reqVO.getDescription());
        fb.setImageUrls(reqVO.getImageUrls());
        fb.setPriority("normal");
        fb.setStatus(PortalFeedbackDO.STATUS_PENDING);
        feedbackMapper.insert(fb);
        return fb;
    }

    @Override
    public PageResult<PortalFeedbackDO> page(PageParam pageParam) {
        LambdaQueryWrapper<PortalFeedbackDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PortalFeedbackDO::getCreatedAt);
        return feedbackMapper.selectPage(pageParam, wrapper);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
