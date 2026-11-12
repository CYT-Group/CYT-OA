package com.cyt.center.module.me.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cyt.center.module.me.model.entity.Milestone;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MilestoneMapperTest {
    @BeforeAll static void mapping() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "milestone-test"), Milestone.class);
    }

    @Test void reusesOldestSameIdentityEvenWhenHistoricalRowsAreDuplicated() {
        var mapper = mock(MilestoneMapper.class, CALLS_REAL_METHODS);
        var first = new Milestone(); first.setId(10L);
        var duplicate = new Milestone(); duplicate.setId(22L);
        var historical = List.of(first, duplicate);
        doReturn(historical).when(mapper).selectListLimitN(any(Wrapper.class), eq(1));

        var date = LocalDate.of(2026, 11, 12);
        assertSame(first, mapper.selectByTypeAndDate(7L, "birthday", date));
        var query = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectListLimitN(query.capture(), eq(1));
        String sql = query.getValue().getSqlSegment();
        assertTrue(sql.contains("user_id ="));
        assertTrue(sql.contains("type ="));
        assertTrue(sql.contains("milestone_date ="));
        assertTrue(sql.contains("ORDER BY id ASC"));
        assertEquals(2, historical.size(), "历史重复行只读复用，不清除");
        verify(mapper, never()).delete(any(Wrapper.class));
        verify(mapper, never()).insert(any(Milestone.class));
    }

    @Test void absentIdentityRemainsAbsent() {
        var mapper = mock(MilestoneMapper.class, CALLS_REAL_METHODS);
        doReturn(List.of()).when(mapper).selectListLimitN(any(Wrapper.class), eq(1));
        assertNull(mapper.selectByTypeAndDate(7L, "birthday", LocalDate.of(2026, 11, 12)));
    }

    @Test void serializesByParameterizedAccountRowRatherThanMissingMilestoneRows() throws Exception {
        var sql = MilestoneMapper.class.getMethod("lockUserForSync", Long.class).getAnnotation(Select.class);
        assertNotNull(sql);
        assertEquals("SELECT id FROM sys_user WHERE id = #{userId} FOR UPDATE", sql.value()[0]);
    }
}
