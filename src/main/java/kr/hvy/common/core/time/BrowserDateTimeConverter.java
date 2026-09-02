package kr.hvy.common.core.time;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrowserDateTimeConverter {

  private static final String WARNING_LOGGED_ATTRIBUTE =
      BrowserDateTimeConverter.class.getName() + ".warningLogged";

  private final ClientTimeZoneResolver clientTimeZoneResolver;

  public UtcDateRange toUtcDateRange(LocalDate from, LocalDate to) {
    return toUtcDateRange(from, to, clientTimeZoneResolver.resolve());
  }

  /**
   * 클라이언트 로컬 날짜 범위를 해당 존의 자정 경계 기준 절대시각 반개구간으로 변환한다.
   * 종료일은 포함(inclusive)이므로 다음 날 자정을 배타 상한으로 삼는다.
   */
  public UtcDateRange toUtcDateRange(
      LocalDate from,
      LocalDate to,
      ResolvedClientTimeZone resolvedClientTimeZone
  ) {
    return toUtcRange(
        from == null ? null : from.atStartOfDay(),
        to == null ? null : to.plusDays(1).atStartOfDay(),
        resolvedClientTimeZone
    );
  }

  public UtcDateRange toUtcDateTimeRange(LocalDateTime from, LocalDateTime to) {
    return toUtcDateTimeRange(from, to, clientTimeZoneResolver.resolve());
  }

  /**
   * 클라이언트 로컬 일시 범위를 해당 존 기준 절대시각 반개구간으로 변환한다.
   * 날짜 단위({@link #toUtcDateRange})와 달리 시·분·초까지 그대로 쓴다.
   *
   * <p>종료 일시는 <b>포함(inclusive)</b>으로 해석해 1초를 더한 값을 배타 상한으로 삼는다.
   * 하루 전체를 뜻하는 {@code 23:59:59} 가 다음 날 자정이 되어 날짜 단위 변환과 결과가 같아진다 —
   * 화면이 날짜 프리셋을 골랐을 때 조회 결과가 달라지지 않게 하는 것이 이 규칙의 목적이다.
   *
   * <p>⚠️ 이 등식은 <b>초 정밀도 입력</b>에서만 성립한다. 프런트엔드가 분 정밀도(`HH:mm`)로
   * 보내면 {@code 23:59} + 1초 = {@code 23:59:01} 이 되어 하루의 마지막 59초가 잘린다.
   */
  public UtcDateRange toUtcDateTimeRange(
      LocalDateTime from,
      LocalDateTime to,
      ResolvedClientTimeZone resolvedClientTimeZone
  ) {
    return toUtcRange(from, to == null ? null : to.plusSeconds(1), resolvedClientTimeZone);
  }

  /**
   * 두 변환의 공통 코어 — 배타 상한이 이미 계산된 로컬 일시 쌍을 절대시각으로 옮긴다.
   * 경계를 어떻게 잡을지(자정이냐 +1초냐)는 호출자가 정하고, 여기서는 존 적용만 한다.
   */
  private UtcDateRange toUtcRange(
      LocalDateTime fromInclusive,
      LocalDateTime toExclusive,
      ResolvedClientTimeZone resolvedClientTimeZone
  ) {
    if (fromInclusive == null && toExclusive == null) {
      return new UtcDateRange(null, null);
    }

    warnIfNeeded(resolvedClientTimeZone);

    ZoneId zoneId = resolvedClientTimeZone.zoneId();
    return new UtcDateRange(toInstant(fromInclusive, zoneId), toInstant(toExclusive, zoneId));
  }

  private Instant toInstant(LocalDateTime browserDateTime, ZoneId zoneId) {
    return browserDateTime == null ? null : browserDateTime.atZone(zoneId).toInstant();
  }

  private void warnIfNeeded(ResolvedClientTimeZone resolvedClientTimeZone) {
    if (!resolvedClientTimeZone.hasWarning()) {
      return;
    }

    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
      HttpServletRequest request = servletRequestAttributes.getRequest();
      if (Boolean.TRUE.equals(request.getAttribute(WARNING_LOGGED_ATTRIBUTE))) {
        return;
      }
      request.setAttribute(WARNING_LOGGED_ATTRIBUTE, Boolean.TRUE);
    }

    log.warn(
        "Client timezone headers are incomplete or invalid. {}. Using zone [{}] from [{}].",
        resolvedClientTimeZone.warningReason(),
        resolvedClientTimeZone.zoneId(),
        resolvedClientTimeZone.source()
    );
  }
}
