package kr.hvy.common.core.time;

import java.time.Instant;

/**
 * 클라이언트 로컬 날짜 범위를 UTC 절대시각 반개구간 [fromInclusive, toExclusive)으로 변환한 결과.
 */
public record UtcDateRange(Instant fromInclusive, Instant toExclusive) {

}
