package kr.hvy.common.infrastructure.database.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.proxy.ProxyConfig;
import net.ttddyy.dsproxy.proxy.ProxyJdbcObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * datasource-proxy 가 만든 실제 ResultSet 프록시(JDK Proxy)를 통해 ResultSetTableLogger 의 위임·가드 동작을 확인한다.
 * 원본 ResultSet 은 Mockito 목이다.
 */
class ResultSetTableLoggerTest {

  private final Logger logger = (Logger) LoggerFactory.getLogger(ResultSetTableLogger.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private Level originalLevel;

  private DataSourceProxySettingProperty property;
  private ResultSet target;
  private ResultSetMetaData metaData;

  @BeforeEach
  void setUp() throws SQLException {
    originalLevel = logger.getLevel();
    logger.setLevel(Level.INFO);
    appender.start();
    logger.addAppender(appender);

    property = new DataSourceProxySettingProperty();
    property.setEnableResultSetLogging(true);

    target = mock(ResultSet.class);
    metaData = mock(ResultSetMetaData.class);
    when(target.getMetaData()).thenReturn(metaData);
    when(metaData.getColumnCount()).thenReturn(2);
    when(metaData.getColumnLabel(1)).thenReturn("id");
    when(metaData.getColumnLabel(2)).thenReturn("name");
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
    logger.setLevel(originalLevel);
  }

  @Test
  void next_DriverThrowsSqlException_PropagatesOriginalException() throws SQLException {
    // Given
    SQLException failure = new SQLException("connection reset");
    when(target.next()).thenThrow(failure);
    ResultSet proxy = proxy();
    // When & Then: InvocationTargetException·UndeclaredThrowableException 으로 감싸지지 않고 원래 예외 그대로다
    assertThatThrownBy(proxy::next).isSameAs(failure);
  }

  @Test
  void getTarget_OnProxy_ReturnsOriginalResultSet() {
    // When & Then: ProxyJdbcObject 공통 메서드는 원본에 invoke 하지 않고 상위 로직이 처리한다
    assertThat(((ProxyJdbcObject) proxy()).getTarget()).isSameAs(target);
  }

  @Test
  void close_AfterRows_LogsMarkdownTableOnce() throws SQLException {
    // Given
    when(target.next()).thenReturn(true, true, false);
    when(target.getObject(1)).thenReturn(1L, 2L);
    when(target.getObject(2)).thenReturn("홍길동", (Object) null);
    ResultSet proxy = proxy();
    // When
    drain(proxy);
    proxy.close();
    proxy.close();
    // Then: 두 번 닫아도 표는 한 번, 원본 close 는 매번 위임
    assertThat(infoMessages()).containsExactly("""
        ResultSet rows=2

        | id | name |
        |---|---|
        | 1 | 홍길동 |
        | 2 | null |""");
    verify(target, times(2)).close();
  }

  @Test
  void close_MoreRowsThanLimit_LogsTotalAndSkipsReadingExtraRows() throws SQLException {
    // Given: 표시 한도 2행에 5행
    property.setMaxResultSetRows(2);
    when(target.next()).thenReturn(true, true, true, true, true, false);
    when(target.getObject(anyInt())).thenReturn("v");
    ResultSet proxy = proxy();
    // When
    drain(proxy);
    proxy.close();
    // Then: 전체 행 수는 5로 세고, 한도 밖 행의 값은 읽지 않는다
    assertThat(infoMessages()).singleElement().asString().startsWith("ResultSet rows=2 total=5\n\n");
    verify(target, times(2 * 2)).getObject(anyInt());
  }

  @Test
  void collect_GetObjectThrowsRuntimeException_DoesNotAffectAppAndLogsError() throws SQLException {
    // Given
    when(target.next()).thenReturn(true, true, false);
    when(target.getObject(1)).thenThrow(new IllegalStateException("boom"));
    ResultSet proxy = proxy();
    // When
    int rows = drain(proxy);
    proxy.close();
    // Then: 앱은 행을 모두 읽고 원본이 닫히며, 표 대신 ERROR 만 남는다
    assertThat(rows).isEqualTo(2);
    verify(target).close();
    assertThat(infoMessages()).isEmpty();
    assertThat(appender.list).anySatisfy(event -> {
      assertThat(event.getLevel()).isEqualTo(Level.ERROR);
      assertThat(event.getFormattedMessage()).contains("ResultSet 로그 수집 실패");
    });
  }

  @Test
  void collect_CellSqlException_KeepsErrorTextInCell() throws SQLException {
    // Given
    when(target.next()).thenReturn(true, false);
    when(target.getObject(1)).thenReturn(7);
    when(target.getObject(2)).thenThrow(new SQLException("bad|value"));
    ResultSet proxy = proxy();
    // When
    drain(proxy);
    proxy.close();
    // Then: 칸 단위 오류는 이스케이프된 텍스트로 남고 표는 유지된다
    assertThat(infoMessages()).containsExactly("""
        ResultSet rows=1

        | column | value |
        |---|---|
        | id | 7 |
        | name | [error: bad\\|value] |""");
  }

  @Test
  void close_TargetCloseThrows_StillLogsTableAndPropagates() throws SQLException {
    // Given
    when(target.next()).thenReturn(true, false);
    when(target.getObject(anyInt())).thenReturn("x");
    SQLException failure = new SQLException("close failed");
    doThrow(failure).when(target).close();
    ResultSet proxy = proxy();
    drain(proxy);
    // When & Then
    assertThatThrownBy(proxy::close).isSameAs(failure);
    assertThat(infoMessages()).singleElement().asString().startsWith("ResultSet rows=1");
  }

  @Test
  void collect_LoggingDisabled_DoesNotReadValues() throws SQLException {
    // Given
    property.setEnableResultSetLogging(false);
    when(target.next()).thenReturn(true, false);
    ResultSet proxy = proxy();
    // When
    drain(proxy);
    proxy.close();
    // Then
    verify(target, never()).getMetaData();
    verify(target, never()).getObject(anyInt());
    assertThat(appender.list).isEmpty();
  }

  @Test
  void close_NoRows_LogsNothing() throws SQLException {
    // Given
    when(target.next()).thenReturn(false);
    ResultSet proxy = proxy();
    // When
    drain(proxy);
    proxy.close();
    // Then
    assertThat(appender.list).isEmpty();
  }

  /** ResultSetTableLoggerFactory 를 등록한 ProxyConfig 로 원본을 감싼 JDK 프록시를 만든다. */
  private ResultSet proxy() {
    ProxyConfig config = ProxyConfig.Builder.create()
        .resultSetProxyLogicFactory(new ResultSetTableLoggerFactory(property))
        .build();
    return config.getJdbcProxyFactory().createResultSet(target, new ConnectionInfo(), config);
  }

  /** 앱처럼 next() 가 false 가 될 때까지 읽고 읽은 행 수를 돌려준다. */
  private static int drain(ResultSet resultSet) throws SQLException {
    int rows = 0;
    while (resultSet.next()) {
      rows++;
    }
    return rows;
  }

  /** INFO 로그 메시지만 모은다. */
  private List<String> infoMessages() {
    return appender.list.stream()
        .filter(event -> event.getLevel() == Level.INFO)
        .map(ILoggingEvent::getFormattedMessage)
        .toList();
  }
}
