package kr.hvy.common.domain.mapper;

import kr.hvy.common.domain.vo.EventLog;
import org.mapstruct.ObjectFactory;

public interface BaseMapper {

  @ObjectFactory
  default EventLog.EventLogBuilder eventLogBuilder() {
    return EventLog.builder();
  }
}
