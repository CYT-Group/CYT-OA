package com.cyt.center.framework.mybatis.core.type;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** PostgreSQL 的 SMALLINT 开关列沿用 0/1，避免 JDBC 将 Boolean 绑定为原生 boolean。 */
public class NumericBooleanTypeHandler extends BaseTypeHandler<Boolean> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Boolean value, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, value ? 1 : 0);
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value != 0;
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, int column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value != 0;
    }

    @Override
    public Boolean getNullableResult(CallableStatement cs, int column) throws SQLException {
        int value = cs.getInt(column);
        return cs.wasNull() ? null : value != 0;
    }
}
