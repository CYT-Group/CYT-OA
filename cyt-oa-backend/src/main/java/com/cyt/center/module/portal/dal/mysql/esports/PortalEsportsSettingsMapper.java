package com.cyt.center.module.portal.dal.mysql.esports;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.module.portal.dal.dataobject.esports.PortalEsportsSettingsDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 电竞室开放规则仓储（迁自 Portal 的 repositories/esports.py 的 settings()）。
 *
 * 单行表（id 恒 = 1）。所有写操作先锁这同一行再锁单据，串行化整间房的预约（口径：统一锁序 settings→booking→steps）。
 */
@Mapper
public interface PortalEsportsSettingsMapper extends BaseMapperX<PortalEsportsSettingsDO> {

    /** 主库读设置（id=1）。 */
    default PortalEsportsSettingsDO selectSettings() {
        return selectById(1L);
    }

    /** 持锁读设置行，写操作统一第一步（FOR UPDATE）。 */
    default PortalEsportsSettingsDO selectForUpdate() {
        return selectOne(new LambdaQueryWrapper<PortalEsportsSettingsDO>()
                .eq(PortalEsportsSettingsDO::getId, 1L)
                .last("FOR UPDATE"));
    }

}
