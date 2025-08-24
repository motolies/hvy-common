package kr.hvy.common.core.code.base;

import jakarta.persistence.AttributeConverter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

public abstract class AbstractEnumCodeConverter<E extends Enum<E> & EnumCode<T>, T>
    implements AttributeConverter<E, T>, TypeHandler<EnumCode<T>> {

  private final Class<E> enumClass;

  protected AbstractEnumCodeConverter(Class<E> enumClass) {
    this.enumClass = enumClass;
  }

  /* ==================================
   * JPA/Hibernate 쪽 AttributeConverter 구현부
   * ================================== */
  @Override
  public T convertToDatabaseColumn(E attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.getCode();
  }

  @Override
  public E convertToEntityAttribute(T dbData) {
    if (dbData == null) {
      return null;
    }
    return Stream.of(enumClass.getEnumConstants())
        .filter(e -> Objects.equals(e.getCode(), dbData))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("알 수 없는 코드: " + dbData));
  }

  /* ==================================
   * MyBatis 쪽 TypeHandler<EnumCode<T>> 구현부
   * ================================== */

  /**
   * (1) setParameter(): Java -> DB
   * <p>   PreparedStatement에 EnumCode<T> 값을 세팅할 때 호출됩니다.</p>
   */
  @Override
  public void setParameter(PreparedStatement ps, int i, EnumCode<T> parameter, JdbcType jdbcType) throws SQLException {
    if (parameter == null) {
      // 여기서는 T가 String인 상황을 가정하므로 VARCHAR 처리
      ps.setNull(i, Types.VARCHAR);
    } else {
      // T가 String이라면 그대로 setString(...)
      // 만약 T가 Integer 등이면 적절히 변경
      ps.setString(i, parameter.getCode().toString());
    }
  }

  /**
   * (2) getResult(ResultSet rs, String columnName): DB -> Java
   * <p>   SELECT 결과에서 컬럼명을 통해 값을 꺼낼 때 호출됩니다.</p>
   */
  @Override
  public EnumCode<T> getResult(ResultSet rs, String columnName) throws SQLException {
    // T가 String이라 가정
    String code = rs.getString(columnName);
    if (rs.wasNull() || code == null) {
      return null;
    }
    // convertToEntityAttribute() 활용 (unchecked 캐스팅 필요)
    return convertToEntityAttribute(castToT(code));
  }

  /**
   * (3) getResult(ResultSet rs, int columnIndex): DB -> Java
   * <p>   SELECT 결과에서 컬럼 인덱스를 통해 값을 꺼낼 때 호출됩니다.</p>
   */
  @Override
  public EnumCode<T> getResult(ResultSet rs, int columnIndex) throws SQLException {
    // T가 String이라 가정
    String code = rs.getString(columnIndex);
    if (rs.wasNull() || code == null) {
      return null;
    }
    return convertToEntityAttribute(castToT(code));
  }

  /**
   * (4) getResult(CallableStatement cs, int columnIndex): DB -> Java
   * <p>   프로시저 호출 결과 등을 받을 때 호출됩니다.</p>
   */
  @Override
  public EnumCode<T> getResult(CallableStatement cs, int columnIndex) throws SQLException {
    // T가 String이라 가정
    String code = cs.getString(columnIndex);
    if (cs.wasNull() || code == null) {
      return null;
    }
    return convertToEntityAttribute(castToT(code));
  }

  /**
   * 내부적으로 T가 String임을 전제하고, 안전하게 캐스팅하기 위한 헬퍼 메서드.
   * <p>T가 실제로 String이 아니라면 추가 변환 로직이 필요할 수 있습니다.</p>
   */
  @SuppressWarnings("unchecked")
  private T castToT(String code) {
    return (T) code;
  }
}