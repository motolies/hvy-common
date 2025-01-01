package kr.hvy.common.domain.mapper;

import kr.hvy.common.domain.embeddable.EventLogEntity;
import kr.hvy.common.domain.vo.EventLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventLogMapper extends BaseMapper {

  EventLog toDomain(EventLogEntity entity);

  EventLogEntity toEntity(EventLog vo);

}