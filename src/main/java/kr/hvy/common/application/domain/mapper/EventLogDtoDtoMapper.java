package kr.hvy.common.application.domain.mapper;

import kr.hvy.common.application.domain.embeddable.EventLogEntity;
import kr.hvy.common.application.domain.vo.EventLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventLogDtoDtoMapper extends BaseDtoMapper {

  EventLog toDomain(EventLogEntity entity);

  EventLogEntity toEntity(EventLog vo);

}