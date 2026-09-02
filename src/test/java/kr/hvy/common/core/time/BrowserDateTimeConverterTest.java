package kr.hvy.common.core.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(OutputCaptureExtension.class)
class BrowserDateTimeConverterTest {

  private final BrowserDateTimeConverter browserDateTimeConverter =
      new BrowserDateTimeConverter(new ClientTimeZoneResolver());

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void convertsKstDateRangeToUtc(CapturedOutput output) {
    bindRequest("Asia/Seoul", "540");

    UtcDateRange utcDateRange = browserDateTimeConverter.toUtcDateRange(
        LocalDate.of(2026, 3, 8),
        LocalDate.of(2026, 3, 8)
    );

    assertEquals(Instant.parse("2026-03-07T15:00:00Z"), utcDateRange.fromInclusive());
    assertEquals(Instant.parse("2026-03-08T15:00:00Z"), utcDateRange.toExclusive());
    assertFalse(output.getOut().contains("Client timezone headers are incomplete or invalid"));
  }

  @Test
  void fallsBackToOffsetWhenTimeZoneHeaderIsInvalid(CapturedOutput output) {
    bindRequest("Invalid/Timezone", "540");

    UtcDateRange utcDateRange = browserDateTimeConverter.toUtcDateRange(
        LocalDate.of(2026, 3, 8),
        LocalDate.of(2026, 3, 8)
    );

    assertEquals(Instant.parse("2026-03-07T15:00:00Z"), utcDateRange.fromInclusive());
    assertEquals(Instant.parse("2026-03-08T15:00:00Z"), utcDateRange.toExclusive());
    assertTrue(output.getOut().contains("Invalid X-Client-Timezone header"));
  }

  @Test
  void fallsBackToUtcWhenHeadersAreMissing(CapturedOutput output) {
    bindRequest(null, null);

    UtcDateRange utcDateRange = browserDateTimeConverter.toUtcDateRange(
        LocalDate.of(2026, 3, 8),
        LocalDate.of(2026, 3, 8)
    );

    assertEquals(Instant.parse("2026-03-08T00:00:00Z"), utcDateRange.fromInclusive());
    assertEquals(Instant.parse("2026-03-09T00:00:00Z"), utcDateRange.toExclusive());
    assertTrue(output.getOut().contains("Missing both X-Client-Timezone and X-Client-Utc-Offset-Minutes headers"));
  }

  @Test
  void usesZoneIdForDstTransitionInsteadOfStaticOffset(CapturedOutput output) {
    bindRequest("America/Los_Angeles", "-480");

    UtcDateRange utcDateRange = browserDateTimeConverter.toUtcDateRange(
        LocalDate.of(2026, 3, 8),
        LocalDate.of(2026, 3, 8)
    );

    assertEquals(Instant.parse("2026-03-08T08:00:00Z"), utcDateRange.fromInclusive());
    assertEquals(Instant.parse("2026-03-09T07:00:00Z"), utcDateRange.toExclusive());
    assertFalse(output.getOut().contains("Client timezone headers are incomplete or invalid"));
  }

  @Test
  void convertsKstDateTimeRangeToUtc(CapturedOutput output) {
    bindRequest("Asia/Seoul", "540");

    UtcDateRange utcDateRange = browserDateTimeConverter.toUtcDateTimeRange(
        LocalDateTime.of(2026, 3, 8, 9, 30, 15),
        LocalDateTime.of(2026, 3, 8, 18, 45, 0)
    );

    assertEquals(Instant.parse("2026-03-08T00:30:15Z"), utcDateRange.fromInclusive());
    // 종료는 포함이므로 1초를 더한 값이 배타 상한이 된다
    assertEquals(Instant.parse("2026-03-08T09:45:01Z"), utcDateRange.toExclusive());
    assertFalse(output.getOut().contains("Client timezone headers are incomplete or invalid"));
  }

  /**
   * 하루 전체(00:00:00~23:59:59)를 일시로 보낸 결과가 날짜 단위 변환과 완전히 같아야 한다.
   * 화면이 날짜 프리셋을 고르는 한 조회 결과가 달라지지 않는다는 보증이며,
   * 이 단언이 깨지면 기존 화면의 검색 결과가 조용히 바뀐 것이다.
   */
  @Test
  void dateTimeRangeOfWholeDayEqualsDateRange() {
    bindRequest("Asia/Seoul", "540");
    LocalDate date = LocalDate.of(2026, 3, 8);

    UtcDateRange byDate = browserDateTimeConverter.toUtcDateRange(date, date);
    UtcDateRange byDateTime = browserDateTimeConverter.toUtcDateTimeRange(
        date.atStartOfDay(),
        date.atTime(23, 59, 59)
    );

    assertEquals(byDate.fromInclusive(), byDateTime.fromInclusive());
    assertEquals(byDate.toExclusive(), byDateTime.toExclusive());
  }

  /**
   * "24시간" 프리셋 — 자정 경계와 무관하게 현재 시각에서 24시간을 거슬러 조회한다.
   * 날짜 단위로는 표현할 수 없는 구간이라는 점이 이 변환을 추가한 이유다.
   */
  @Test
  void convertsSlidingLast24HoursWindow() {
    bindRequest("Asia/Seoul", "540");
    LocalDateTime now = LocalDateTime.of(2026, 3, 8, 0, 30, 0);

    UtcDateRange utcDateRange =
        browserDateTimeConverter.toUtcDateTimeRange(now.minusHours(24), now);

    // KST 자정 직후 조회라 시작이 전날로 넘어간다 — 날짜 필터로는 어제 저녁이 잡히지 않는다
    assertEquals(Instant.parse("2026-03-06T15:30:00Z"), utcDateRange.fromInclusive());
    assertEquals(Instant.parse("2026-03-07T15:30:01Z"), utcDateRange.toExclusive());
  }

  @Test
  void dateTimeRangeFallsBackToUtcWhenHeadersAreMissing(CapturedOutput output) {
    bindRequest(null, null);

    UtcDateRange utcDateRange = browserDateTimeConverter.toUtcDateTimeRange(
        LocalDateTime.of(2026, 3, 8, 9, 0, 0),
        LocalDateTime.of(2026, 3, 8, 17, 0, 0)
    );

    assertEquals(Instant.parse("2026-03-08T09:00:00Z"), utcDateRange.fromInclusive());
    assertEquals(Instant.parse("2026-03-08T17:00:01Z"), utcDateRange.toExclusive());
    assertTrue(output.getOut().contains("Missing both X-Client-Timezone and X-Client-Utc-Offset-Minutes headers"));
  }

  /**
   * DST 로 건너뛴 시각(2026-03-08 02:30 PT 는 존재하지 않는다)은 건너뛴 폭(1시간)만큼 밀려
   * 03:30 PDT 로 해석된다. 정적 오프셋으로 계산하면 존재하지 않는 벽시계를 그대로 변환해 어긋난다.
   */
  @Test
  void dateTimeRangeShiftsNonExistentLocalTimeAcrossDstGap() {
    bindRequest("America/Los_Angeles", "-480");

    UtcDateRange utcDateRange = browserDateTimeConverter.toUtcDateTimeRange(
        LocalDateTime.of(2026, 3, 8, 2, 30, 0),
        LocalDateTime.of(2026, 3, 8, 12, 0, 0)
    );

    // 02:30 은 없는 시각이라 03:30 PDT(UTC-7) 로 밀린다
    assertEquals(Instant.parse("2026-03-08T10:30:00Z"), utcDateRange.fromInclusive());
    assertEquals(Instant.parse("2026-03-08T19:00:01Z"), utcDateRange.toExclusive());
  }

  @Test
  void returnsEmptyRangeWhenBothDateTimesAreNull() {
    bindRequest("Asia/Seoul", "540");

    UtcDateRange utcDateRange = browserDateTimeConverter.toUtcDateTimeRange(null, null);

    assertNull(utcDateRange.fromInclusive());
    assertNull(utcDateRange.toExclusive());
  }

  /** 한쪽만 지정한 조회 — 열린 구간이 그대로 유지되어야 한다. */
  @Test
  void keepsOpenEndedRangeWhenOnlyOneSideIsGiven() {
    bindRequest("Asia/Seoul", "540");

    UtcDateRange fromOnly = browserDateTimeConverter.toUtcDateTimeRange(
        LocalDateTime.of(2026, 3, 8, 9, 0, 0), null);
    UtcDateRange toOnly = browserDateTimeConverter.toUtcDateTimeRange(
        null, LocalDateTime.of(2026, 3, 8, 9, 0, 0));

    assertEquals(Instant.parse("2026-03-08T00:00:00Z"), fromOnly.fromInclusive());
    assertNull(fromOnly.toExclusive());
    assertNull(toOnly.fromInclusive());
    assertEquals(Instant.parse("2026-03-08T00:00:01Z"), toOnly.toExclusive());
  }

  private void bindRequest(String timeZone, String offsetMinutes) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    if (timeZone != null) {
      request.addHeader(ClientTimeZoneResolver.TIMEZONE_HEADER, timeZone);
    }
    if (offsetMinutes != null) {
      request.addHeader(ClientTimeZoneResolver.OFFSET_HEADER, offsetMinutes);
    }

    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }
}
