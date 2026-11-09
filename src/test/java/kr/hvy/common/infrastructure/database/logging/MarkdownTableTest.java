package kr.hvy.common.infrastructure.database.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 마크다운 렌더러 골든 테스트. 출력 문자열을 텍스트 블록과 정확히 비교한다(후행 개행 없음). */
class MarkdownTableTest {

  @Test
  void render_SingleRow_RendersVerticalTable() {
    // Given
    MarkdownTable table = GfmTables.table(List.of("id", "name"), new Object[] {1, "홍길동"});
    // When & Then: 1행은 세로표
    assertThat(table.render()).isEqualTo("""
        rows=1

        | column | value |
        |---|---|
        | id | 1 |
        | name | 홍길동 |""");
  }

  @Test
  void render_MultipleRows_RendersHorizontalTableWithoutTrailingNewline() {
    // Given
    MarkdownTable table = GfmTables.table(List.of("a", "b"), new Object[] {"1", "2"}, new Object[] {"3", "4"});
    // When
    String rendered = table.render();
    // Then
    assertThat(rendered).isEqualTo("""
        rows=2

        | a | b |
        |---|---|
        | 1 | 2 |
        | 3 | 4 |""");
    assertThat(rendered).doesNotEndWith("\n");
  }

  @Test
  void render_MoreRowsThanLimit_ShowsLimitedRowsWithTotal() {
    // Given: 표시 한도 2행에 5행
    MarkdownTable table = new MarkdownTable(List.of("x"), 2, Integer.MAX_VALUE);
    for (int i = 1; i <= 5; i++) {
      if (table.countRow()) {
        table.addRow(new Object[] {i});
      }
    }
    // When & Then
    assertThat(table.render()).isEqualTo("""
        rows=2 total=5

        | x |
        |---|
        | 1 |
        | 2 |""");
  }

  @Test
  void render_DuplicateLabels_SuffixesFromSecondIgnoringCase() {
    // Given: 조인으로 같은 라벨이 반복된다
    MarkdownTable table = GfmTables.table(List.of("status", "STATUS", "status"), new Object[] {"a", "b", "c"});
    // When & Then
    assertThat(table.render()).isEqualTo("""
        rows=1

        | column | value |
        |---|---|
        | status | a |
        | STATUS.2 | b |
        | status.3 | c |""");
  }

  @Test
  void render_PipeNewlineAndPaddedValues_EscapesCellsAndLabels() {
    // Given: 헤더 라벨에도 파이프가 올 수 있다(별칭 없는 식 라벨)
    MarkdownTable table = GfmTables.table(List.of("p|q", "n", "s"), new Object[] {"a|b", "x\ny", " z "});
    // When & Then: 파이프는 \\|, 개행은 \\n, 앞뒤 공백은 따옴표
    assertThat(table.render()).isEqualTo("""
        rows=1

        | column | value |
        |---|---|
        | p\\|q | a\\|b |
        | n | x\\ny |
        | s | " z " |""");
  }

  @Test
  void render_NullEmptyAndStringNull_RendersDistinctCells() {
    // Given
    MarkdownTable table = GfmTables.table(List.of("n", "e", "s"),
        new Object[] {null, "", "null"}, new Object[] {"v", "w", "x"});
    // When & Then: SQL NULL 은 null, 빈 문자열은 "", 문자열 null 은 "null"
    assertThat(table.render()).isEqualTo("""
        rows=2

        | n | e | s |
        |---|---|---|
        | null | "" | "null" |
        | v | w | x |""");
  }

  @Test
  void render_ShorterRowThanColumns_LeavesRemainingCellsEmpty() {
    // Given
    MarkdownTable table = GfmTables.table(List.of("a", "b"), new Object[] {"1"}, new Object[] {"2", "3"});
    // When & Then
    assertThat(table.render()).isEqualTo("""
        rows=2

        | a | b |
        |---|---|
        | 1 |  |
        | 2 | 3 |""");
  }

  @Test
  void render_NoRowsOrNoColumns_RendersNothing() {
    // When & Then
    assertThat(new MarkdownTable(List.of("x"), 100, 150).render()).isEmpty();
    assertThat(GfmTables.table(List.of(), new Object[] {}).render()).isEmpty();
  }

  @Test
  void render_ZeroRowLimit_CountsButRendersNothing() {
    // Given: 표시 한도 0
    MarkdownTable table = new MarkdownTable(List.of("x"), 0, 150);
    // When & Then: 값을 담지 않으므로 출력할 표가 없다
    assertThat(table.countRow()).isFalse();
    assertThat(table.render()).isEmpty();
  }

  @Test
  void summary_ShownAndTotal_FormatsRowsWithTotalOnlyWhenTruncated() {
    // When & Then
    assertThat(MarkdownTable.summary(100, 2345)).isEqualTo("rows=100 total=2345");
    assertThat(MarkdownTable.summary(3, 3)).isEqualTo("rows=3");
  }
}
