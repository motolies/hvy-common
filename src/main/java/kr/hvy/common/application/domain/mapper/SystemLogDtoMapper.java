package kr.hvy.common.application.domain.mapper;


import kr.hvy.common.aop.logging.dto.SystemLogCreate;
import kr.hvy.common.aop.logging.entity.SystemLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SystemLogDtoMapper {

  @Mapping(target = "id", ignore = true)
  SystemLog toEntity(SystemLogCreate systemLogCreate);

}
