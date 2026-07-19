package kr.hvy.common.core.converter;

import io.hypersistence.tsid.TSID;
import java.time.Instant;

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

  // TSID 생성 시각을 절대시각(Instant)으로 반환 — JVM 타임존에 의존하지 않는다
  public static Instant getInstant(Long tsid) {
    return getTsid(tsid).getInstant();
  }

  public static Instant getInstant(String tsid) {
    return getTsid(tsid).getInstant();
  }

  public static Instant getInstant(TSID tsid) {
    return tsid.getInstant();
  }

}
