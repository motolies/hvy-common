package kr.hvy.common.core.converter;

import io.hypersistence.tsid.TSID;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TsidUtils {

  public static TSID getFastTsid() {
    return TSID.fast();
  }

  public static TSID getTsid() {
    return TSID.Factory.getTsid256();
  }

  public static TSID getTsid(String tsid) {
    return TSID.from(tsid);
  }

  public static TSID getTsid(Long tsid) {
    return TSID.from(tsid);
  }

  public static LocalDateTime getLocalDateTime(Long tsid) {
    return getTsid(tsid).getInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
  }

  public static LocalDateTime getLocalDateTime(String tsid) {
    return getTsid(tsid).getInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
  }

  public static LocalDateTime getLocalDateTime(TSID tsid) {
    return tsid.getInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
  }

}
