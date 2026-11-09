package kr.hvy.common.infrastructure.database.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;

/**
 * 마크다운 결과표를 실제 GFM 파서(commonmark-java + tables·strikethrough)로 읽어 셀 텍스트로 돌려주는 테스트 도우미.
 * 셀 안 인라인 노드가 Text 가 아니면(강조·코드·HTML·링크·취소선) 단언 실패로 처리한다.
 */
final class GfmTables {

  private static final Parser GFM = Parser.builder()
      .extensions(List.of(TablesExtension.create(), StrikethroughExtension.create()))
      .build();

  /** 파싱된 표의 헤더 행들과 바디 행들(각 셀은 표시 텍스트). */
  record Parsed(List<List<String>> head, List<List<String>> body) {
  }

  private GfmTables() {
  }

  /** 표시 한도 100행·셀 150자로 표를 만들고 행들을 순서대로 담는다. */
  static MarkdownTable table(List<String> labels, Object[]... rows) {
    MarkdownTable table = new MarkdownTable(labels, 100, 150);
    for (Object[] row : rows) {
      table.countRow();
      table.addRow(row);
    }
    return table;
  }

  /** 렌더링 결과를 GFM 으로 파싱해 표 하나를 돌려준다(요약 줄 뒤의 표가 표로 인식되어야 한다). */
  static Parsed parse(String markdown) {
    List<Node> nodes = new ArrayList<>();
    collect(GFM.parse(markdown), nodes);
    List<TableBlock> tables = nodes.stream().filter(TableBlock.class::isInstance).map(TableBlock.class::cast).toList();
    assertThat(tables).hasSize(1);
    TableBlock table = tables.getFirst();
    assertThat(table.getFirstChild()).isInstanceOf(TableHead.class);
    assertThat(table.getLastChild()).isInstanceOf(TableBody.class);
    return new Parsed(rows(table.getFirstChild()), rows(table.getLastChild()));
  }

  /** 헤더·바디의 각 행을 셀 텍스트 목록으로 바꾼다. */
  private static List<List<String>> rows(Node section) {
    List<List<String>> rows = new ArrayList<>();
    for (Node row = section.getFirstChild(); row != null; row = row.getNext()) {
      List<String> cells = new ArrayList<>();
      for (Node cell = row.getFirstChild(); cell != null; cell = cell.getNext()) {
        cells.add(literal(cell));
      }
      rows.add(cells);
    }
    return rows;
  }

  /** 셀 안의 인라인 노드가 전부 Text 인지 확인하고 글자를 이어 붙인다. */
  private static String literal(Node cell) {
    List<Node> inline = new ArrayList<>();
    collect(cell, inline);
    assertThat(inline).allSatisfy(node -> assertThat(node).isInstanceOf(Text.class));
    return inline.stream().map(node -> ((Text) node).getLiteral()).collect(Collectors.joining());
  }

  /** 하위 노드를 깊이 우선으로 모은다. */
  private static void collect(Node node, List<Node> out) {
    for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
      out.add(child);
      collect(child, out);
    }
  }
}
