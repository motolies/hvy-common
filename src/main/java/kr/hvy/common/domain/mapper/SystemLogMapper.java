package kr.hvy.common.domain.mapper;


import kr.hvy.common.aop.log.dto.SystemLogCreate;
import kr.hvy.common.aop.log.model.SystemLogEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SystemLogMapper {

  SystemLogMapper INSTANCE = Mappers.getMapper(SystemLogMapper.class);

  @Mapping(target = "id", ignore = true)
  SystemLogEntity toEntity(SystemLogCreate systemLogCreate);

}
