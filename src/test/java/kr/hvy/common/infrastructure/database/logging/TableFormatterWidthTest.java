package kr.hvy.common.infrastructure.database.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 한글/영문 표시 너비 계산 테스트
 */
class TableFormatterWidthTest {

  @Test
  void 한글_영문_혼합_표시_너비_테스트() {
    // Given
    List<String> columnNames = Arrays.asList("id", "fullName", "name");
    List<List<String>> rows = Arrays.asList(
        Arrays.asList("ROOT", "/전체글/", "전체글")
    );

    TableFormatter formatter = new TableFormatter(columnNames, rows, Integer.MAX_VALUE);

    // When
    String result = formatter.format();

    // Then
    System.out.println(result);

    // 각 라인이 같은 길이여야 함
    String[] lines = result.split("\n");
    int firstLineLength = lines[1].length(); // 첫 번째 구분선

    for (String line : lines) {
      if (!line.isEmpty()) {
        System.out.println("Line length: " + line.length() + " -> " + line);
        assertThat(line.length())
            .as("모든 라인의 길이가 동일해야 함")
            .isEqualTo(firstLineLength);
      }
    }
  }

  @Test
  void HTML_엔티티_포함_테스트() {
    // Given
    List<String> columnNames = Arrays.asList("content");
    List<List<String>> rows = Arrays.asList(
        Arrays.asList("한글로&nbsp;&nbsp;&nbsp;써보고")
    );

    TableFormatter formatter = new TableFormatter(columnNames, rows, Integer.MAX_VALUE);

    // When
    String result = formatter.format();

    // Then
    System.out.println(result);

    // &nbsp;는 6글자로 계산되므로 표시가 어긋날 것
    String[] lines = result.split("\n");
    for (String line : lines) {
      if (!line.isEmpty()) {
        System.out.println("Line: " + line);
      }
    }
  }

  @Test
  void 긴_HTML_내용_테스트() {
    // Given
    List<String> columnNames = Arrays.asList("body");
    List<List<String>> rows = Arrays.asList(
        Arrays.asList("<p>한글로 써보고&nbsp;</p><p>&nbsp;</p>")
    );

    TableFormatter formatter = new TableFormatter(columnNames, rows, 50);

    // When
    String result = formatter.format();

    // Then
    System.out.println(result);

    String[] lines = result.split("\n");
    int firstLineLength = lines[1].length();

    for (String line : lines) {
      if (!line.isEmpty()) {
        System.out.println("Line length: " + line.length() + " -> " + line);
        assertThat(line.length())
            .as("모든 라인의 길이가 동일해야 함")
            .isEqualTo(firstLineLength);
      }
    }
  }

  @Test
  void 한글_자모_표시_너비_테스트() {
    // Given
    List<String> columnNames = Arrays.asList("id", "jamo", "mixed");
    List<List<String>> rows = Arrays.asList(
        Arrays.asList("1", "ㄱㄴㄷ", "ㄱ가ㄴ나")
    );

    TableFormatter formatter = new TableFormatter(columnNames, rows, Integer.MAX_VALUE);

    // When
    String result = formatter.format();

    // Then
    System.out.println(result);

    String[] lines = result.split("\n");
    int firstLineLength = lines[1].length();

    for (String line : lines) {
      if (!line.isEmpty()) {
        System.out.println("Line length: " + line.length() + " -> " + line);
        assertThat(line.length())
            .as("모든 라인의 길이가 동일해야 함 (자모도 2칸으로 계산)")
            .isEqualTo(firstLineLength);
      }
    }
  }
}