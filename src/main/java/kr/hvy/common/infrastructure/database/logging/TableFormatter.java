package kr.hvy.common.infrastructure.database.logging;

import java.util.ArrayList;
import java.util.List;

/**
 * ResultSet 데이터를 log4jdbc 스타일의 테이블 형식으로 포맷팅하는 유틸리티 클래스
 */
public class TableFormatter {

  private final List<String> columnNames;
  private final List<List<String>> rows;
  private final int maxColumnValueLength;

  public TableFormatter(List<String> columnNames, List<List<String>> rows, int maxColumnValueLength) {
    this.columnNames = columnNames;
    this.rows = rows;
    this.maxColumnValueLength = maxColumnValueLength;
  }

  /**
   * 테이블 형식의 문자열을 생성
   */
  public String format() {
    if (columnNames.isEmpty()) {
      return "";
    }

    // 각 컬럼의 최대 너비 계산
    int[] columnWidths = calculateColumnWidths();

    // 헤더와 모든 행을 먼저 생성하여 실제 문자열 길이 파악
    List<String> formattedColumnNames = new ArrayList<>();
    for (int i = 0; i < columnNames.size(); i++) {
      String name = truncateValue(columnNames.get(i));
      formattedColumnNames.add(padToDisplayWidth(name, columnWidths[i] - 2));
    }

    List<List<String>> formattedRows = new ArrayList<>();
    for (List<String> row : rows) {
      List<String> formattedRow = new ArrayList<>();
      for (int i = 0; i < columnWidths.length; i++) {
        String value = i < row.size() ? row.get(i) : "";
        if (value == null) {
          value = "[null]";
        }
        value = truncateValue(value);
        formattedRow.add(padToDisplayWidth(value, columnWidths[i] - 2));
      }
      formattedRows.add(formattedRow);
    }

    // 이제 실제 문자열 길이를 계산
    int[] actualLengths = new int[columnWidths.length];
    for (int i = 0; i < columnWidths.length; i++) {
      actualLengths[i] = formattedColumnNames.get(i).length() + 2; // 양쪽 공백 포함
    }
    for (List<String> formattedRow : formattedRows) {
      for (int i = 0; i < formattedRow.size(); i++) {
        actualLengths[i] = Math.max(actualLengths[i], formattedRow.get(i).length() + 2);
      }
    }

    // 최종 출력
    StringBuilder sb = new StringBuilder("\n");

    // 상단 구분선
    appendSeparatorLineByLength(sb, actualLengths);

    // 헤더 행
    appendFormattedRow(sb, formattedColumnNames, actualLengths);

    // 헤더 구분선
    appendSeparatorLineByLength(sb, actualLengths);

    // 데이터 행들
    for (List<String> formattedRow : formattedRows) {
      appendFormattedRow(sb, formattedRow, actualLengths);
    }

    // 하단 구분선
    appendSeparatorLineByLength(sb, actualLengths);

    return sb.toString();
  }

  /**
   * 각 컬럼의 최대 너비를 계산 (헤더와 모든 행의 값을 고려)
   * 실제 터미널 표시 너비 기준으로 계산 (한글=2칸, 영문=1칸)
   */
  private int[] calculateColumnWidths() {
    int[] widths = new int[columnNames.size()];

    // 헤더 표시 너비로 초기화 (칼럼명도 길이 제한 적용)
    for (int i = 0; i < columnNames.size(); i++) {
      String columnName = truncateValue(columnNames.get(i));
      widths[i] = getDisplayWidth(columnName);
    }

    // 모든 행의 값 표시 너비 확인
    for (List<String> row : rows) {
      for (int i = 0; i < Math.min(row.size(), widths.length); i++) {
        String value = row.get(i);
        if (value != null) {
          int valueWidth = getDisplayWidth(truncateValue(value));
          widths[i] = Math.max(widths[i], valueWidth);
        }
      }
    }

    // 양쪽 패딩(각 1칸)을 고려하여 너비에 2 추가
    for (int i = 0; i < widths.length; i++) {
      widths[i] += 2;
    }

    return widths;
  }

  /**
   * 실제 문자열 길이 기준으로 구분선 추가
   */
  private void appendSeparatorLineByLength(StringBuilder sb, int[] actualLengths) {
    for (int length : actualLengths) {
      sb.append('|');
      sb.append("-".repeat(length));
    }
    sb.append("|\n");
  }

  /**
   * 이미 포맷팅된 값들을 출력 (실제 길이에 맞춰 패딩)
   */
  private void appendFormattedRow(StringBuilder sb, List<String> formattedValues, int[] actualLengths) {
    for (int i = 0; i < actualLengths.length; i++) {
      sb.append('|');
      String value = i < formattedValues.size() ? formattedValues.get(i) : "";

      // 양쪽 공백 1칸씩 추가하고, 남은 공간은 오른쪽에 공백으로 채움
      sb.append(' ');
      sb.append(value);
      int padding = actualLengths[i] - value.length() - 2; // 양쪽 공백 1칸씩 제외
      if (padding > 0) {
        sb.append(" ".repeat(padding));
      }
      sb.append(' ');
    }
    sb.append("|\n");
  }

  /**
   * 표시 너비에 맞춰 문자열을 패딩 (한글/영문 고려)
   */
  private String padToDisplayWidth(String value, int targetDisplayWidth) {
    int currentWidth = getDisplayWidth(value);
    if (currentWidth >= targetDisplayWidth) {
      return value;
    }
    // 목표 표시 너비에 도달할 때까지 공백 추가
    return value + " ".repeat(targetDisplayWidth - currentWidth);
  }

  /**
   * 값이 최대 표시 길이를 초과하면 자르기
   * 실제 터미널 표시 너비 기준으로 자름 (한글=2칸, 영문=1칸)
   */
  private String truncateValue(String value) {
    if (value == null) {
      return "[null]";
    }

    if (maxColumnValueLength == Integer.MAX_VALUE) {
      return value;
    }

    // 현재 표시 너비 확인
    int currentWidth = getDisplayWidth(value);
    if (currentWidth <= maxColumnValueLength) {
      return value;
    }

    // 표시 너비 기준으로 자르기
    StringBuilder result = new StringBuilder();
    int accumulatedWidth = 0;
    int targetWidth = maxColumnValueLength - 3; // "..." 공간 확보

    for (int i = 0; i < value.length(); i++) {
      int codePoint = value.codePointAt(i);

      // Surrogate pair 처리
      if (Character.isSupplementaryCodePoint(codePoint)) {
        i++;
      }

      int charWidth = isFullWidth(codePoint) ? 2 : 1;

      if (accumulatedWidth + charWidth > targetWidth) {
        break;
      }

      result.appendCodePoint(codePoint);
      accumulatedWidth += charWidth;
    }

    return result.toString() + "...";
  }

  /**
   * 문자열의 실제 터미널 표시 너비를 계산
   * 한글, 한자, 전각문자 = 2칸, 영문, 숫자 = 1칸
   */
  private int getDisplayWidth(String str) {
    if (str == null) {
      return 0;
    }

    int width = 0;
    for (int i = 0; i < str.length(); i++) {
      int codePoint = str.codePointAt(i);

      // Surrogate pair 처리
      if (Character.isSupplementaryCodePoint(codePoint)) {
        i++;
      }

      // 전각 문자는 2칸, 반각 문자는 1칸
      if (isFullWidth(codePoint)) {
        width += 2;
      } else {
        width += 1;
      }
    }
    return width;
  }

  /**
   * 전각 문자(2칸 차지) 여부 판단
   */
  private boolean isFullWidth(int codePoint) {
    // 한글 음절 (가-힣)
    if (codePoint >= 0xAC00 && codePoint <= 0xD7AF) {
      return true;
    }

    // 한글 자모
    if ((codePoint >= 0x1100 && codePoint <= 0x11FF) ||
        (codePoint >= 0x3130 && codePoint <= 0x318F)) {
      return true;
    }

    // CJK 통합 한자
    if ((codePoint >= 0x4E00 && codePoint <= 0x9FFF) ||
        (codePoint >= 0x3400 && codePoint <= 0x4DBF) ||
        (codePoint >= 0x20000 && codePoint <= 0x2A6DF)) {
      return true;
    }

    // 전각 문자 (Full-width)
    if (codePoint >= 0xFF00 && codePoint <= 0xFFEF) {
      return true;
    }

    // 이모지 및 기타 심볼
    if ((codePoint >= 0x1F300 && codePoint <= 0x1F9FF) ||
        (codePoint >= 0x2600 && codePoint <= 0x26FF) ||
        (codePoint >= 0x2700 && codePoint <= 0x27BF)) {
      return true;
    }

    return false;
  }
}