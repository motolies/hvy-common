package kr.hvy.common.application.domain.mapper;


import kr.hvy.common.aop.logging.dto.ApiLogCreate;
import kr.hvy.common.aop.logging.entity.ApiLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApiLogDtoMapper {

  @Mapping(target = "id", ignore = true)
  ApiLog toEntity(ApiLogCreate apiLogCreate);

}
