package kr.hvy.common.domain.mapper;

import kr.hvy.common.domain.vo.CreateUpdateDate;
import org.mapstruct.ObjectFactory;

public interface BaseMapper {

  @ObjectFactory
  default CreateUpdateDate.CreateUpdateDateBuilder createUpdateDateBuilder() {
    return CreateUpdateDate.builder();
  }
}
