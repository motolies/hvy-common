package kr.hvy.common.infrastructure.database.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class TableFormatterTest {

  @Test
  void 테이블_포맷팅_기본_테스트() {
    // Given
    List<String> columnNames = Arrays.asList("id", "name", "email");
    List<List<String>> rows = Arrays.asList(
        Arrays.asList("1", "홍길동", "hong@example.com"),
        Arrays.asList("2", "김철수", "kim@example.com")
    );

    TableFormatter formatter = new TableFormatter(columnNames, rows, Integer.MAX_VALUE);

    // When
    String result = formatter.format();

    // Then
    assertThat(result).isNotEmpty();
    assertThat(result).contains("id");
    assertThat(result).contains("name");
    assertThat(result).contains("email");
    assertThat(result).contains("홍길동");
    assertThat(result).contains("김철수");
    assertThat(result).contains("hong@example.com");
    assertThat(result).contains("kim@example.com");
  }

  @Test
  void null_값_처리_테스트() {
    // Given
    List<String> columnNames = Arrays.asList("id", "name", "email");
    List<List<String>> rows = Arrays.asList(
        Arrays.asList("1", null, "hong@example.com"),
        Arrays.asList("2", "김철수", null)
    );

    TableFormatter formatter = new TableFormatter(columnNames, rows, Integer.MAX_VALUE);

    // When
    String result = formatter.format();

    // Then
    assertThat(result).contains("[null]");
  }

  @Test
  void 긴_값_자르기_테스트() {
    // Given
    List<String> columnNames = Arrays.asList("id", "description");
    List<List<String>> rows = Arrays.asList(
        Arrays.asList("1", "This is a very long description that should be truncated")
    );

    TableFormatter formatter = new TableFormatter(columnNames, rows, 20);

    // When
    String result = formatter.format();

    // Then
    assertThat(result).contains("...");
  }

  @Test
  void 빈_테이블_처리_테스트() {
    // Given
    List<String> columnNames = Arrays.asList();
    List<List<String>> rows = Arrays.asList();

    TableFormatter formatter = new TableFormatter(columnNames, rows, Integer.MAX_VALUE);

    // When
    String result = formatter.format();

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void 컬럼만_있고_데이터_없는_경우_테스트() {
    // Given
    List<String> columnNames = Arrays.asList("id", "name", "email");
    List<List<String>> rows = Arrays.asList();

    TableFormatter formatter = new TableFormatter(columnNames, rows, Integer.MAX_VALUE);

    // When
    String result = formatter.format();

    // Then
    // 컬럼이 있으면 헤더 라인은 출력되어야 함
    assertThat(result).isNotEmpty();
    assertThat(result).contains("id");
    assertThat(result).contains("name");
    assertThat(result).contains("email");
  }

  @Test
  void 구분선_포함_테스트() {
    // Given
    List<String> columnNames = Arrays.asList("id", "name");
    List<List<String>> rows = Arrays.asList(
        Arrays.asList("1", "홍길동")
    );

    TableFormatter formatter = new TableFormatter(columnNames, rows, Integer.MAX_VALUE);

    // When
    String result = formatter.format();

    // Then
    // 구분선이 포함되어 있는지 확인 (|------|)
    assertThat(result).containsPattern("\\|[-]+\\|");
  }
}