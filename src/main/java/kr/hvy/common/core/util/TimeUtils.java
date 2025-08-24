package kr.hvy.common.core.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeUtils {

  // todo 사용하지 않으므로 삭제할 것
  public static Timestamp getUtcTimestamp() {
    LocalDateTime ldt = LocalDateTime.now();
    ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.systemDefault());
    ZonedDateTime gmt = zdt.withZoneSameInstant(ZoneId.of("GMT"));
    return Timestamp.valueOf(gmt.toLocalDateTime());
  }


}
