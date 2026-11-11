package com.cyt.center.module.oa.dal.redis.no;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.cyt.center.module.oa.dal.redis.RedisKeyConstants;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * OA 业务单号的 Redis DAO
 *
 * @author 芋道源码
 */
@Repository
public class OaNoRedisDAO {

    /**
     * 用品领用申请 {@link com.cyt.center.module.oa.dal.dataobject.supply.OaSupplyApplyDO}
     */
    public static final String SUPPLY_APPLY_NO_PREFIX = "YP";

    /**
     * 出差申请 {@link com.cyt.center.module.oa.dal.dataobject.travel.OaTravelApplyDO}
     */
    public static final String TRAVEL_APPLY_NO_PREFIX = "CC";
    /**
     * 出差报销 {@link com.cyt.center.module.oa.dal.dataobject.travel.OaTravelReimbursementDO}
     */
    public static final String TRAVEL_REIMBURSEMENT_NO_PREFIX = "CLBX";
    /**
     * 工作汇报 {@link com.cyt.center.module.oa.dal.dataobject.workreport.OaWorkReportDO}
     */
    public static final String WORK_REPORT_NO_PREFIX = "WR";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成序号，使用当前日期，格式为 {PREFIX} + yyyyMMdd + 6 位自增
     * 例如说：YC20260912000001 （没有中间空格）
     *
     * @param prefix 前缀
     * @return 序号
     */
    public String generate(String prefix) {
        // 递增序号
        String noPrefix = prefix + DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
        String key = RedisKeyConstants.NO + noPrefix;
        Long no = stringRedisTemplate.opsForValue().increment(key);
        // 设置过期时间
        stringRedisTemplate.expire(key, Duration.ofDays(1L));
        return noPrefix + String.format("%06d", no);
    }

}
