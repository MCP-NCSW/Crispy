package com.mcp.crispy.common.typehandler;

import com.mcp.crispy.employee.dto.EmpStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(EmpStatus.class)
public class EmpStatusTypeHandler extends BaseTypeHandler<EmpStatus> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, EmpStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getValue());
    }

    @Override
    public EmpStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : EmpStatus.fromValue(code);
    }

    @Override
    public EmpStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : EmpStatus.fromValue(code);
    }

    @Override
    public EmpStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : EmpStatus.fromValue(code);
    }
}
