package kr.hvy.common.infrastructure.database.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 셀 인코딩 골든 테스트와 GFM 왕복 테스트. 왕복 테스트는 실제 파서(commonmark-java + tables·strikethrough 확장)로
 * 렌더링한 표를 읽어 셀이 원래 값 그대로(강조·코드·HTML·링크 없이) 보이는지 확인한다.
 */
class MarkdownCellTest {

  private static final int MAX = 150;

  @Test
  void of_SqlNullAndStringNull_RendersBareAndQuoted() {
    // When & Then: 따옴표 없는 null 만 SQL NULL 이고 문자열은 대소문자와 무관하게 따옴표로 감싼다
    assertThat(cell(null)).isEqualTo("null");
    assertThat(cell("null")).isEqualTo("\"null\"");
    assertThat(cell("NULL")).isEqualTo("\"NULL\"");
  }

  @Test
  void of_EmptyPaddedAndQuoteLeadingValues_WrapsInQuotes() {
    // When & Then: 안쪽 따옴표는 그대로 두어 바깥 한 쌍만 벗기면 원래 값이 된다
    assertThat(cell("")).isEqualTo("\"\"");
    assertThat(cell(" z ")).isEqualTo("\" z \"");
    assertThat(cell("\"abc\"")).isEqualTo("\"\"abc\"\"");
    assertThat(cell("\"\"")).isEqualTo("\"\"\"\"");
    assertThat(cell("abc\"")).isEqualTo("abc\"");
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("specialCharacterCases")
  void of_MarkdownSpecialCharacters_EscapesOnlyWhereSyntaxWouldApply(String raw, String expected) {
    // When & Then
    assertThat(cell(raw)).isEqualTo(expected);
  }

  /** (원문, 기대 셀 텍스트). 구문이 되는 자리만 역슬래시로 막고 흔한 DB 값은 그대로 둔다. */
  static Stream<Arguments> specialCharacterCases() {
    return Stream.of(
        Arguments.of("a|b", "a\\|b"),
        Arguments.of("a\\|b", "a\\\\\\|b"),
        Arguments.of("a\\\\b", "a\\\\\\\\b"),
        Arguments.of("\\\\|", "\\\\\\\\\\|"),
        Arguments.of("a\\", "a\\\\"),
        Arguments.of("*x*", "\\*x\\*"),
        Arguments.of("`c`", "\\`c\\`"),
        Arguments.of("~~d~~", "\\~\\~d\\~\\~"),
        Arguments.of("<pending>", "\\<pending>"),
        Arguments.of("</b>", "\\</b>"),
        Arguments.of("<a@b.c>", "\\<a@b.c>"),
        Arguments.of("<123@example.com>", "\\<123@example.com>"),
        Arguments.of("<=@x.y>", "\\<=@x.y>"),
        Arguments.of("<|@a.b>", "\\<\\|@a.b>"),
        Arguments.of("a@b.c", "a@b.c"),
        Arguments.of("a < b", "a < b"),
        Arguments.of("x<5", "x<5"),
        Arguments.of("<=", "<="),
        Arguments.of("[t](u)", "[t\\](u)"),
        Arguments.of("![i](s)", "![i\\](s)"),
        Arguments.of("[a][b]", "[a\\][b]"),
        Arguments.of("[1,2]", "[1,2]"),
        Arguments.of("[Info] done", "[Info] done"),
        Arguments.of("&amp;", "\\&amp;"),
        Arguments.of("&#65;", "\\&#65;"),
        Arguments.of("&#x41;", "\\&#x41;"),
        Arguments.of("a=1&b=2", "a=1&b=2"),
        Arguments.of("_em_", "\\_em\\_"),
        Arguments.of("__init__", "\\_\\_init\\_\\_"),
        Arguments.of("_a_b_", "\\_a_b\\_"),
        Arguments.of("a_ b", "a\\_ b"),
        Arguments.of("a_b_c", "a_b_c"),
        Arguments.of("SOME_ENUM_CONSTANT_VALUE", "SOME_ENUM_CONSTANT_VALUE"),
        Arguments.of("K20261030000001_000123_1", "K20261030000001_000123_1"),
        Arguments.of("가_나", "가_나"),
        Arguments.of("x\ny", "x\\ny"),
        Arguments.of("x\r\ny", "x\\ny"),
        Arguments.of("x\ry", "x\\ry"),
        Arguments.of("{\"a\":[1,2],\"b\":\"x\\\"y\"}", "{\"a\":[1,2],\"b\":\"x\\\\\"y\"}"),
        Arguments.of("https://x.y/p?a=1&b=2#f", "https://x.y/p?a=1&b=2#f"),
        Arguments.of("2026-11-01T20:11:55-06:00", "2026-11-01T20:11:55-06:00"));
  }

  @Test
  void of_TemporalValues_RendersIso8601WithJvmOffset() {
    // Given
    Timestamp ts = Timestamp.valueOf("2026-11-03 00:04:50.856763");
    String expected = ts.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime().toString();
    OffsetDateTime foreign = OffsetDateTime.parse("2026-11-01T20:11:55-06:00");
    String foreignExpected = foreign.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime().toString();
    // When & Then: 순간 값은 JVM 시간대 오프셋을 붙인 ISO-8601 로 통일하고 마이크로초를 유지한다
    assertThat(cell(ts)).isEqualTo(expected).contains("T").contains(".856763");
    assertThat(cell(foreign)).isEqualTo(foreignExpected);
    assertThat(cell(foreign.toInstant())).isEqualTo(foreignExpected);
    assertThat(cell(ZonedDateTime.ofInstant(foreign.toInstant(), ZoneId.of("Asia/Seoul"))))
        .isEqualTo(foreignExpected).doesNotContain("[");
    assertThat(cell(java.util.Date.from(foreign.toInstant()))).isEqualTo(foreignExpected);
    // 지역 값은 시간대 없는 ISO 그대로
    assertThat(cell(java.sql.Date.valueOf("2026-11-02"))).isEqualTo("2026-11-02");
    assertThat(cell(java.sql.Time.valueOf("02:11:56"))).isEqualTo("02:11:56");
    assertThat(cell(LocalDateTime.parse("2026-11-02T02:11:56.7693"))).isEqualTo("2026-11-02T02:11:56.769300");
  }

  @Test
  void of_TemporalValuesOutOfRange_FallBackToToString() {
    // When & Then: pgjdbc 가 infinity 를 OffsetDateTime.MAX/MIN 으로 돌려줘도 셀 하나 때문에 표를 잃지 않고 원래 표기로 남긴다
    assertThat(cell(OffsetDateTime.MAX)).isEqualTo(OffsetDateTime.MAX.toString());
    assertThat(cell(OffsetDateTime.MIN)).isEqualTo(OffsetDateTime.MIN.toString());
    assertThat(cell(Instant.MAX)).isEqualTo(Instant.MAX.toString());
    assertThat(cell(Instant.MIN)).isEqualTo(Instant.MIN.toString());
  }

  @Test
  void of_ObjectWhoseToStringIsNull_RendersEmptyString() {
    // Given: toString 이 null 을 돌려주는 드문 값 객체
    Object value = new Object() {
      @Override
      public String toString() {
        return null;
      }
    };
    // When & Then: NPE 로 표를 잃지 않고 빈 문자열로 남긴다
    assertThat(cell(value)).isEqualTo("\"\"");
  }

  @Test
  void of_LongValue_TruncatesRawTextBeforeEscaping() {
    // When & Then: 절단은 원문 기준이라 이스케이프 뒤에는 더 길어질 수 있다
    assertThat(cell("|".repeat(200))).isEqualTo("\\|".repeat(147) + "...");
    assertThat(cell("a".repeat(150))).isEqualTo("a".repeat(150));
  }

  @Test
  void of_KoreanValue_CountsCharactersNotDisplayWidth() {
    // When & Then: 문자 수 기준이라 표시폭 2칸인 한글도 한도까지 남는다
    assertThat(MarkdownCell.of("가".repeat(10), 10)).isEqualTo("가".repeat(10));
    assertThat(MarkdownCell.of("가".repeat(11), 10)).isEqualTo("가".repeat(7) + "...");
  }

  @Test
  void of_SurrogatePairAtCutPoint_KeepsPairIntact() {
    // Given: 147번째 문자가 서로게이트 쌍의 앞 절반에 걸친다
    String value = "a".repeat(146) + "😀" + "b".repeat(10);
    // When
    String cell = cell(value);
    // Then: 쌍을 가르지 않고 한 글자 덜 남긴다
    assertThat(cell).isEqualTo("a".repeat(146) + "...");
    assertThat(cell.chars().noneMatch(c -> Character.isSurrogate((char) c))).isTrue();
  }

  @Test
  void of_MaxLengthNotLongerThanEllipsis_LeavesOnlyEllipsis() {
    // When & Then: 남길 글자가 없어도 음수 인덱스 없이 잘렸다는 표시만 남긴다
    assertThat(MarkdownCell.of("abcdef", 3)).isEqualTo("...");
    assertThat(MarkdownCell.of("abcdef", 0)).isEqualTo("...");
    assertThat(MarkdownCell.of("abc", 3)).isEqualTo("abc");
  }

  @Test
  void of_UnlimitedLength_KeepsWholeValue() {
    // When & Then: 기본값(Integer.MAX_VALUE)은 자르지 않는다
    assertThat(MarkdownCell.of("a".repeat(10_000), Integer.MAX_VALUE)).hasSize(10_000);
  }

  @Test
  void render_SpecialValues_SurviveGfmParsing() {
    // Given: 라벨과 셀에 마크다운 구문·시간·NULL·빈 문자열·문자열 null 을 섞는다
    List<String> labels = List.of("p|q", "_x_", "<tag>", "status", "ts");
    Timestamp ts = Timestamp.valueOf("2026-11-03 00:04:50.856763");
    String tsText = ts.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime().toString();
    Object[] first = {"a\\|b", "*x* _y_ `z`", "<pending>", "null", ts};
    Object[] second = {"[t](u) &amp; ~~d~~", "", " z ", null, "SOME_ENUM"};
    List<String> firstText = List.of("a\\|b", "*x* _y_ `z`", "<pending>", "\"null\"", tsText);
    List<String> secondText = List.of("[t](u) &amp; ~~d~~", "\"\"", "\" z \"", "null", "SOME_ENUM");
    MarkdownTable horizontal = GfmTables.table(labels, first, second);
    MarkdownTable vertical = GfmTables.table(labels, first);

    // When: 실제 GFM 파서로 읽는다
    GfmTables.Parsed horizontalTable = GfmTables.parse(horizontal.render());
    GfmTables.Parsed verticalTable = GfmTables.parse(vertical.render());

    // Then: 가로표는 헤더·행이 그대로, 세로표는 (라벨, 값) 쌍이 그대로이며 어느 셀도 다른 인라인 노드로 해석되지 않는다
    assertThat(horizontalTable.head()).containsExactly(labels);
    assertThat(horizontalTable.body()).containsExactly(firstText, secondText);
    assertThat(verticalTable.head()).containsExactly(List.of("column", "value"));
    List<List<String>> pairs = new ArrayList<>();
    for (int i = 0; i < labels.size(); i++) {
      pairs.add(List.of(labels.get(i), firstText.get(i)));
    }
    assertThat(verticalTable.body()).containsExactlyElementsOf(pairs);
  }

  @Test
  void render_AllGoldenCases_SurviveGfmParsing() {
    // Given: 골든 케이스 원문 전부(따옴표 케이스 포함)를 열로 둔 가로표 2행과 세로표 1행. 기대 표시는 디코딩 규칙에서 유도한다
    List<String> raws = Stream.concat(
        specialCharacterCases().map(arguments -> (String) arguments.get()[0]),
        Stream.of("", " z ", "null", "NULL", "\"abc\"", "a".repeat(150))).toList();
    List<String> reversed = raws.reversed();
    List<String> labels = IntStream.range(0, raws.size()).mapToObj(i -> "c" + i).toList();
    MarkdownTable horizontal = GfmTables.table(labels, raws.toArray(), reversed.toArray());
    MarkdownTable vertical = GfmTables.table(labels, raws.toArray());

    // When
    GfmTables.Parsed horizontalTable = GfmTables.parse(horizontal.render());
    GfmTables.Parsed verticalTable = GfmTables.parse(vertical.render());

    // Then: 열이 갈라지거나 합쳐지지 않고 모든 셀이 원문 표시 그대로다
    assertThat(horizontalTable.head()).containsExactly(labels);
    assertThat(horizontalTable.body()).containsExactly(display(raws), display(reversed));
    List<List<String>> pairs = new ArrayList<>();
    for (int i = 0; i < labels.size(); i++) {
      pairs.add(List.of(labels.get(i), display(raws.get(i))));
    }
    assertThat(verticalTable.body()).containsExactlyElementsOf(pairs);
  }

  /** 테스트 기본 한도(150자)로 셀을 인코딩한다. */
  private static String cell(Object value) {
    return MarkdownCell.of(value, MAX);
  }

  /** 디코딩 규칙으로 기대 표시 텍스트를 만든다: 개행은 \n·\r 두 글자, 따옴표는 비었거나 null 이거나 따옴표 시작·앞뒤 공백일 때. */
  private static String display(String raw) {
    String text = raw.replace("\r\n", "\n").replace("\n", "\\n").replace("\r", "\\r");
    boolean quoted = raw.isEmpty() || raw.equalsIgnoreCase("null") || raw.startsWith("\"")
        || Character.isWhitespace(raw.charAt(0)) || Character.isWhitespace(raw.charAt(raw.length() - 1));
    return quoted ? "\"" + text + "\"" : text;
  }

  private static List<String> display(List<String> raws) {
    return raws.stream().map(MarkdownCellTest::display).toList();
  }
}
