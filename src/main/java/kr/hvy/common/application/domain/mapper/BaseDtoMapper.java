package kr.hvy.common.application.domain.mapper;

import kr.hvy.common.application.domain.vo.EventLog;
import org.mapstruct.ObjectFactory;

public interface BaseDtoMapper {

  @ObjectFactory
  default EventLog defaultEventLogInstance() {
    return EventLog.defaultValues();
  }
}
