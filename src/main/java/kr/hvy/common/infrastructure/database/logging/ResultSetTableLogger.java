package kr.hvy.common.infrastructure.database.logging;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.proxy.ProxyConfig;
import net.ttddyy.dsproxy.proxy.SimpleResultSetProxyLogic;

/**
 * 앱이 ResultSet 을 소비하는 동안 행을 수집하고, close() 시 GFM 마크다운 표로 로깅하는 프록시 로직.
 *
 * <p>위임·공통 메서드(getTarget·unwrap 등)·{@code InvocationTargetException} 해제는 {@link SimpleResultSetProxyLogic}에 맡기고
 * {@code next()}·{@code close()} 결과만 엿본다. 그래서 드라이버의 {@code SQLException} 은 앱에 그대로 전달된다.
 * 수집·렌더링 중 오류는 ERROR 로그만 남기고 앱에는 전파하지 않는다(Error 는 삼키지 않는다).
 *
 * <p>출력 형식과 셀 디코딩 규칙은 {@link MarkdownTable}·{@link MarkdownCell} 참고.
 */
@Slf4j
public class ResultSetTableLogger extends SimpleResultSetProxyLogic {

  private static final int STACK_TRACE_LIMIT = 10;
  private static final int FAILURE_LOG_INTERVAL = 1000;
  /** 수집·렌더링 실패 누적 건수. ResultSet 마다 인스턴스가 생기므로 전역으로 센다. */
  private static final AtomicLong FAILURES = new AtomicLong();

  private final ResultSet resultSet;
  private final DataSourceProxySettingProperty property;

  private MarkdownTable table;
  private boolean abandoned;
  private boolean logged;

  public ResultSetTableLogger(ResultSet resultSet, ConnectionInfo connectionInfo, ProxyConfig proxyConfig, DataSourceProxySettingProperty property) {
    super(resultSet, connectionInfo, proxyConfig);
    this.resultSet = resultSet;
    this.property = property;
  }

  /**
   * 원래 호출은 상위 클래스로 위임하고, next()==true 면 행을 수집, close() 면 위임 성공·실패와 무관하게 표를 한 번 남긴다.
   */
  @Override
  protected Object performProxyLogic(Object proxy, Method method, Object[] args, MethodExecutionContext methodContext) throws Throwable {
    String methodName = method.getName();
    if ("close".equals(methodName)) {
      try {
        return super.performProxyLogic(proxy, method, args, methodContext);
      } finally {
        logOnce();
      }
    }

    Object result = super.performProxyLogic(proxy, method, args, methodContext);
    if ("next".equals(methodName) && Boolean.TRUE.equals(result)) {
      collectSafely();
    }
    return result;
  }

  /**
   * 현재 행을 수집한다. 첫 행에서 메타데이터로 표를 만들고, 표시 한도 밖의 행은 값을 읽지 않고 개수만 센다.
   * 실패하면 이 ResultSet 의 수집을 포기하고(표 미출력) ERROR 만 남긴다.
   */
  private void collectSafely() {
    if (abandoned || logged || !property.isEnableResultSetLogging() || !log.isInfoEnabled()) {
      return;
    }
    try {
      if (table == null) {
        table = new MarkdownTable(extractLabels(), property.getMaxResultSetRows(), property.getMaxColumnValueLength());
      }
      if (table.countRow()) {
        table.addRow(readRow(table.columnCount()));
      }
    } catch (Exception e) {
      abandoned = true;
      table = null;
      logFailure("ResultSet 로그 수집 실패 (앱에는 전파하지 않음)", e);
    }
  }

  /** 메타데이터에서 컬럼 라벨을 최대 컬럼 수까지 추출한다. 드라이버가 null 을 주면 "Column{n}". */
  private List<String> extractLabels() throws SQLException {
    ResultSetMetaData metaData = resultSet.getMetaData();
    int columnCount = Math.min(metaData.getColumnCount(), property.getMaxResultSetColumns());
    List<String> labels = new ArrayList<>(Math.max(columnCount, 0));
    for (int i = 1; i <= columnCount; i++) {
      labels.add(Objects.requireNonNullElse(metaData.getColumnLabel(i), "Column" + i));
    }
    return labels;
  }

  /** 현재 행의 값을 원본 ResultSet 에서 읽는다. 칸 단위 SQLException 은 "[error: ...]" 텍스트로 남긴다. */
  private Object[] readRow(int columnCount) {
    Object[] values = new Object[columnCount];
    for (int i = 0; i < columnCount; i++) {
      try {
        values[i] = resultSet.getObject(i + 1);
      } catch (SQLException e) {
        values[i] = "[error: " + e.getMessage() + "]";
      }
    }
    return values;
  }

  /** 수집된 표를 한 번만 INFO 로 남기고 버퍼를 비운다. 렌더링 실패는 ERROR 만 남긴다. */
  private void logOnce() {
    if (logged) {
      return;
    }
    logged = true;
    MarkdownTable collected = table;
    table = null;
    if (collected == null || abandoned) {
      return;
    }
    try {
      String rendered = collected.render();
      if (!rendered.isEmpty()) {
        log.info("ResultSet {}", rendered);
      }
    } catch (Exception e) {
      logFailure("ResultSet 결과표 렌더링 실패 (앱에는 전파하지 않음)", e);
    }
  }

  /** 처음 10건은 스택 포함, 이후는 1,000건마다 건수만 남긴다. */
  private static void logFailure(String what, Exception e) {
    long n = FAILURES.incrementAndGet();
    if (n <= STACK_TRACE_LIMIT) {
      log.error("{}: {}", what, e.toString(), e);
    } else if (n % FAILURE_LOG_INTERVAL == 0) {
      log.error("{}: {} (누적 {}건, 스택 생략)", what, e.toString(), n);
    }
  }
}
