package run.acloud.framework.handlers;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Java Boolean 값을 Database 상의 "Y/N" 값으로 변경하는 Mybastis 용 Type Handler
 * 
 * @author Morris
 *
 */
@MappedJdbcTypes(JdbcType.CHAR)
@MappedTypes(Boolean.class)
public class YesNoBooleanTypeHandler extends BaseTypeHandler<Boolean> {
	private static final String YES = "Y";
	private static final String NO = "N";

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i,
			Boolean parameter, JdbcType jdbcType) throws SQLException {
		ps.setString(i, parameter ? YES : NO);
	}

	@Override
	public Boolean getNullableResult(ResultSet rs, String columnName)
			throws SQLException {
		return convertStringToBooelan(rs.getString(columnName));
	}

	@Override
	public Boolean getNullableResult(ResultSet rs, int columnIndex)
			throws SQLException {
		return convertStringToBooelan(rs.getString(columnIndex));
	}

	@Override
	public Boolean getNullableResult(CallableStatement cs, int columnIndex)
			throws SQLException {
		return convertStringToBooelan(cs.getString(columnIndex));
	}

	private Boolean convertStringToBooelan(String strValue) throws SQLException {
		if (YES.equalsIgnoreCase(strValue)) {
			return Boolean.TRUE;
		} else if (NO.equalsIgnoreCase(strValue)) {
			return Boolean.FALSE;
		} else {
			throw new SQLException("Unexpected value " + strValue
					+ " found where " + YES + " or " + NO + " was expected.");
		}
	}
}
