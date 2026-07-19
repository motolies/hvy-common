package kr.hvy.common.core.time;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
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
   */
  public UtcDateRange toUtcDateRange(
      LocalDate from,
      LocalDate to,
      ResolvedClientTimeZone resolvedClientTimeZone
  ) {
    if (from == null && to == null) {
      return new UtcDateRange(null, null);
    }

    warnIfNeeded(resolvedClientTimeZone);

    Instant fromInclusive = from == null
        ? null
        : toInstantAtStartOfDay(from, resolvedClientTimeZone.zoneId());
    Instant toExclusive = to == null
        ? null
        : toInstantAtStartOfDay(to.plusDays(1), resolvedClientTimeZone.zoneId());

    return new UtcDateRange(fromInclusive, toExclusive);
  }

  private Instant toInstantAtStartOfDay(LocalDate browserDate, ZoneId zoneId) {
    return browserDate.atStartOfDay(zoneId).toInstant();
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
