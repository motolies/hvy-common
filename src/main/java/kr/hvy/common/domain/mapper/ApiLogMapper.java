package kr.hvy.common.domain.mapper;


import kr.hvy.common.aop.log.dto.ApiLogCreate;
import kr.hvy.common.aop.log.model.ApiLogEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ApiLogMapper {

  ApiLogMapper INSTANCE = Mappers.getMapper(ApiLogMapper.class);

  @Mapping(target = "id", ignore = true)
  ApiLogEntity toEntity(ApiLogCreate apiLogCreate);

}
