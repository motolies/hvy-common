package kr.hvy.common.code.converter;

import jakarta.persistence.Converter;
import kr.hvy.common.code.ApiResponseStatus;
import kr.hvy.common.code.base.AbstractEnumCodeConverter;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

@Converter(autoApply = true)
@MappedTypes(ApiResponseStatus.class)
@MappedJdbcTypes(value = JdbcType.VARCHAR)
public class ApiResponseStatusConverter extends AbstractEnumCodeConverter<ApiResponseStatus, String> {

  public ApiResponseStatusConverter() {
    super(ApiResponseStatus.class);
  }


}
