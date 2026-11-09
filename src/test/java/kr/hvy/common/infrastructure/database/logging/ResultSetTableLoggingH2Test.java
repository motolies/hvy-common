package kr.hvy.common.infrastructure.database.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * 실제 JDBC 드라이버(H2) + datasource-proxy 경로에서 SELECT 결과가 GFM 마크다운 표로 로깅되고,
 * 그 표를 GFM 파서로 다시 읽으면 원래 값이 나오는지 확인한다.
 */
class ResultSetTableLoggingH2Test {

  private static final Timestamp CREATED = Timestamp.valueOf("2026-11-09 10:00:00.123456");

  private final Logger logger = (Logger) LoggerFactory.getLogger(ResultSetTableLogger.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private Level originalLevel;
  private DataSource proxied;

  @BeforeEach
  void setUp() throws SQLException {
    originalLevel = logger.getLevel();
    logger.setLevel(Level.INFO);
    appender.start();
    logger.addAppender(appender);

    JdbcDataSource h2 = new JdbcDataSource();
    h2.setURL("jdbc:h2:mem:rs-log-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    try (Connection connection = h2.getConnection(); Statement statement = connection.createStatement()) {
      statement.execute("CREATE TABLE item (id INT PRIMARY KEY, txt VARCHAR(100), memo VARCHAR(100), created TIMESTAMP(6))");
      try (PreparedStatement insert = connection.prepareStatement("INSERT INTO item VALUES (?, ?, ?, ?)")) {
        insertRow(insert, 1, "a|b *c*", "");
        insertRow(insert, 2, "<b>_x_</b>", null);
      }
    }

    DataSourceProxySettingProperty property = new DataSourceProxySettingProperty();
    property.setEnableResultSetLogging(true);
    proxied = ProxyDataSourceBuilder.create(h2)
        .name("test")
        .proxyResultSet(new ResultSetTableLoggerFactory(property))
        .build();
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
    logger.setLevel(originalLevel);
  }

  @Test
  void select_MultipleRows_LogsHorizontalTableThatRoundTripsThroughGfm() throws SQLException {
    // When
    int rows = query("SELECT id, txt, memo, created FROM item ORDER BY id");

    // Then: 앱은 2행을 모두 읽고, 로그 표를 GFM 으로 읽으면 원래 값(빈 문자열 "", SQL NULL null, ISO-8601 시간)이 나온다
    assertThat(rows).isEqualTo(2);
    String message = singleInfoMessage();
    assertThat(message).startsWith("ResultSet rows=2\n\n");
    String created = CREATED.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime().toString();
    GfmTables.Parsed table = GfmTables.parse(message);
    assertThat(table.head()).containsExactly(List.of("ID", "TXT", "MEMO", "CREATED"));
    assertThat(table.body()).containsExactly(
        List.of("1", "a|b *c*", "\"\"", created),
        List.of("2", "<b>_x_</b>", "null", created));
  }

  @Test
  void select_SingleRowWithDuplicateLabels_LogsVerticalTable() throws SQLException {
    // When
    query("SELECT 1 AS \"x\", 2 AS \"X\"");

    // Then
    assertThat(singleInfoMessage()).isEqualTo("""
        ResultSet rows=1

        | column | value |
        |---|---|
        | x | 1 |
        | X.2 | 2 |""");
  }

  /** 프록시 DataSource 로 조회해 앱처럼 끝까지 읽고 닫는다. 읽은 행 수를 돌려준다. */
  private int query(String sql) throws SQLException {
    int rows = 0;
    try (Connection connection = proxied.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      while (resultSet.next()) {
        rows++;
      }
    }
    return rows;
  }

  private static void insertRow(PreparedStatement insert, int id, String txt, String memo) throws SQLException {
    insert.setInt(1, id);
    insert.setString(2, txt);
    insert.setString(3, memo);
    insert.setTimestamp(4, CREATED);
    insert.executeUpdate();
  }

  /** INFO 로그가 정확히 하나인지 확인하고 그 메시지를 돌려준다. */
  private String singleInfoMessage() {
    List<String> messages = appender.list.stream()
        .filter(event -> event.getLevel() == Level.INFO)
        .map(ILoggingEvent::getFormattedMessage)
        .toList();
    assertThat(messages).hasSize(1);
    return messages.getFirst();
  }
}
