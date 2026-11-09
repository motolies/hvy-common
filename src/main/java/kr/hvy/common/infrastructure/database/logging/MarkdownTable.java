package kr.hvy.common.infrastructure.database.logging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ResultSet 행을 모아 GFM 마크다운 파이프 표로 렌더링한다.
 *
 * <p>값은 {@link #addRow} 시점에 {@link MarkdownCell#of}로 셀 텍스트(이스케이프·따옴표·시간 ISO-8601)로 바꿔 저장한다.
 * 출력은 요약 줄({@code rows=N}, 표시 한도를 넘으면 {@code rows=50 total=2345}), 빈 줄, 표 순서이며
 * 1행은 세로표({@code | column | value |}), 여러 행은 가로표다.
 */
final class MarkdownTable {

  private final List<String> labels;
  private final int maxRows;
  private final int maxValueLength;
  private final List<String[]> rows = new ArrayList<>();
  private int totalRows;

  /**
   * @param labels         컬럼 라벨(원문, 렌더링 시 중복 처리·인코딩)
   * @param maxRows        표에 담을 최대 행 수(그 이상은 개수만 센다)
   * @param maxValueLength 셀 값 최대 길이(UTF-16 단위)
   */
  MarkdownTable(List<String> labels, int maxRows, int maxValueLength) {
    this.labels = List.copyOf(labels);
    this.maxRows = maxRows;
    this.maxValueLength = maxValueLength;
  }

  /** 컬럼 수. 호출자는 이만큼 값을 읽어 {@link #addRow}로 넘긴다. */
  int columnCount() {
    return labels.size();
  }

  /** 행 하나를 센다. 표시 한도 안이면 true 이고, 호출자는 값을 읽어 {@link #addRow}로 넘긴다. */
  boolean countRow() {
    totalRows++;
    return rows.size() < maxRows;
  }

  /** 행 값을 셀 텍스트로 인코딩해 담는다. 컬럼 수보다 짧은 입력의 나머지 칸은 빈 칸이다. */
  void addRow(Object[] values) {
    String[] cells = new String[labels.size()];
    for (int i = 0; i < cells.length; i++) {
      cells[i] = i < values.length ? MarkdownCell.of(values[i], maxValueLength) : "";
    }
    rows.add(cells);
  }

  /** 요약 한 줄. 표시 한도로 잘렸을 때만 total 을 붙인다. */
  static String summary(int shown, int total) {
    return shown < total ? "rows=" + shown + " total=" + total : "rows=" + total;
  }

  /**
   * 요약, 빈 줄, GFM 표 순서로 렌더링한다. GFM 은 표 뒤 비어 있지 않은 줄을 행으로 흡수하므로 요약은 앞에 둔다.
   * 담긴 행이 없으면 빈 문자열이며 후행 개행은 없다.
   */
  String render() {
    if (labels.isEmpty() || rows.isEmpty()) {
      return "";
    }
    String[] headers = uniqueLabels();
    StringBuilder sb = new StringBuilder(1024)
        .append(summary(rows.size(), totalRows))
        .append("\n\n");
    if (rows.size() == 1) {                          // 1행: 세로표
      sb.append("| column | value |\n|---|---|\n");
      String[] row = rows.getFirst();
      for (int c = 0; c < headers.length; c++) {
        sb.append("| ").append(headers[c]).append(" | ").append(row[c]).append(" |\n");
      }
    } else {                                         // N행: 가로표
      appendRow(sb, headers);
      sb.append('|').append("---|".repeat(headers.length)).append('\n');
      rows.forEach(row -> appendRow(sb, row));
    }
    sb.setLength(sb.length() - 1);                   // 후행 개행 제거
    return sb.toString();
  }

  /** 한 행을 "| a | b |" 꼴로 붙인다. 값은 이미 셀 텍스트로 인코딩되어 있다. */
  private static void appendRow(StringBuilder sb, String[] cells) {
    sb.append('|');
    for (String cell : cells) {
      sb.append(' ').append(cell).append(" |");
    }
    sb.append('\n');
  }

  /**
   * 조인으로 같은 라벨이 반복되면 두 번째부터 ".2", ".3"을 붙인 뒤 셀 텍스트로 인코딩한다
   * (별칭 없는 식 라벨에 파이프 등이 올 수 있다). 비교는 대소문자를 무시한다.
   */
  private String[] uniqueLabels() {
    Map<String, Integer> seen = new HashMap<>();
    String[] result = new String[labels.size()];
    for (int i = 0; i < result.length; i++) {
      String label = labels.get(i);
      int n = seen.merge(label.toLowerCase(Locale.ROOT), 1, Integer::sum);
      result[i] = MarkdownCell.of(n == 1 ? label : label + "." + n, maxValueLength);
    }
    return result;
  }
}
