package kr.hvy.common.infrastructure.database.mybatis.interceptor;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("PageInterceptor.assertSafeColumn - ORDER BY 컬럼 화이트리스트")
class PageInterceptorTest {

  @Nested
  @DisplayName("허용되는 식별자")
  class Allowed {

    @ParameterizedTest(name = "[{index}] \"{0}\" 는 통과한다")
    @ValueSource(strings = {
        "name",
        "created_at",
        "_private",
        "col1",
        "t.created_at",
        "tb_post.title",
        "A.B"
    })
    @DisplayName("단순 식별자/한정 식별자(table.column)는 예외 없이 통과한다")
    void 안전한_컬럼은_통과한다(String column) {
      assertThatCode(() -> PageInterceptor.assertSafeColumn(column))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("차단되는 입력(인젝션/비식별자)")
  class Blocked {

    @ParameterizedTest(name = "[{index}] \"{0}\" 는 거부된다")
    @ValueSource(strings = {
        "name; DROP TABLE users",   // 세미콜론/멀티스테이트먼트
        "1=1",                       // 항등식
        "(SELECT 1)",                // 서브쿼리/괄호
        "name--",                    // 주석
        "name ASC",                  // 공백(방향은 enum 으로 별도 처리)
        "LOWER(name)",               // 함수 호출
        "\"name\"",                  // 따옴표 식별자
        "name,age",                  // 다중 컬럼 문자열
        "1name",                     // 숫자 시작
        "a..b",                      // 잘못된 한정자
        ""                           // 빈 문자열
    })
    @DisplayName("공백/따옴표/괄호/세미콜론/주석/함수/다중컬럼 등은 IllegalArgumentException")
    void 위험한_컬럼은_거부된다(String column) {
      assertThatThrownBy(() -> PageInterceptor.assertSafeColumn(column))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("유효하지 않은 정렬 컬럼");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("null 컬럼은 IllegalArgumentException")
    void null_컬럼은_거부된다() {
      assertThatThrownBy(() -> PageInterceptor.assertSafeColumn(null))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
