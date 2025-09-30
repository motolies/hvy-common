package kr.hvy.common.infrastructure.database.logging;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.proxy.ProxyConfig;
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogic;

/**
 * ResultSet의 데이터를 수집하여 테이블 형식으로 로깅하는 프록시 로직
 */
@Slf4j
public class ResultSetTableLogger implements ResultSetProxyLogic {

  private final ResultSet resultSet;
  private final ConnectionInfo connectionInfo;
  private final ProxyConfig proxyConfig;
  private final DataSourceProxySettingProperty property;

  private List<String> columnNames;
  private final List<List<String>> rows = new ArrayList<>();
  private boolean metadataExtracted = false;
  private int rowCount = 0;

  public ResultSetTableLogger(ResultSet resultSet, ConnectionInfo connectionInfo, ProxyConfig proxyConfig, DataSourceProxySettingProperty property) {
    this.resultSet = resultSet;
    this.connectionInfo = connectionInfo;
    this.proxyConfig = proxyConfig;
    this.property = property;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();

    // next() 호출 시 데이터 수집
    if ("next".equals(methodName)) {
      boolean hasNext = (boolean) method.invoke(resultSet, args);

      if (hasNext) {
        // 메타데이터 추출 (최초 1회)
        if (!metadataExtracted) {
          extractMetadata();
          metadataExtracted = true;
        }

        // 행 수 제한 확인
        if (rowCount < property.getMaxResultSetRows()) {
          collectRowData();
          rowCount++;
        }
      }

      return hasNext;
    }

    // close() 호출 시 테이블 출력
    if ("close".equals(methodName)) {
      logResultSetTable();
      return method.invoke(resultSet, args);
    }

    // 다른 메서드는 그대로 위임
    return method.invoke(resultSet, args);
  }

  /**
   * ResultSet의 메타데이터에서 컬럼 정보 추출
   */
  private void extractMetadata() throws SQLException {
    ResultSetMetaData metaData = resultSet.getMetaData();
    int columnCount = Math.min(metaData.getColumnCount(), property.getMaxResultSetColumns());

    columnNames = new ArrayList<>(columnCount);
    for (int i = 1; i <= columnCount; i++) {
      String columnLabel = metaData.getColumnLabel(i);
      columnNames.add(columnLabel != null ? columnLabel : "Column" + i);
    }
  }

  /**
   * 현재 행의 데이터를 수집
   */
  private void collectRowData() throws SQLException {
    List<String> row = new ArrayList<>(columnNames.size());

    for (int i = 1; i <= columnNames.size(); i++) {
      try {
        Object value = resultSet.getObject(i);
        row.add(value != null ? value.toString() : "[null]");
      } catch (SQLException e) {
        row.add("[error: " + e.getMessage() + "]");
      }
    }

    rows.add(row);
  }

  /**
   * 수집된 데이터를 테이블 형식으로 로깅
   */
  private void logResultSetTable() {
    if (!property.isEnableResultSetLogging() || columnNames == null || rows.isEmpty()) {
      return;
    }

    TableFormatter formatter = new TableFormatter(columnNames, rows, property.getMaxColumnValueLength());
    String table = formatter.format();

    if (rowCount >= property.getMaxResultSetRows()) {
      log.info("ResultSet (showing {} of {} rows):{}", property.getMaxResultSetRows(), rowCount, table);
    } else {
      log.info("ResultSet ({} rows):{}", rows.size(), table);
    }
  }
}