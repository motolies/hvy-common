package kr.hvy.common.db.log;

import java.util.List;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.listener.logging.QueryLogEntryCreator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CustomQueryLoggingListener implements QueryExecutionListener {

  protected Log log = LogFactory.getLog(CustomQueryLoggingListener.class);

  private final DataSourceProxySettingProperty property;
  protected QueryLogEntryCreator queryLogEntryCreator;

  public CustomQueryLoggingListener(DataSourceProxySettingProperty property) {
    this.property = property;
    this.queryLogEntryCreator = new BoundQueryLogEntryCreator(property.isFormat());
  }

  protected boolean writeDataSourceName = true;
  protected boolean writeConnectionId = true;
  protected boolean writeIsolation;
  // 슬로우 쿼리 임계치 (ms)
  private long slowQueryThreshold = 1000; // 예: 1000ms 이상이면 slow query로 간주

  @Override
  public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    // 필요시 beforeQuery 로직 추가
  }

  @Override
  public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    final String entry = getEntry(execInfo, queryInfoList);
    // 쿼리 실행 시간이 임계치 이상이면 warn, 그렇지 않으면 debug 로깅
    if (execInfo.getElapsedTime() >= slowQueryThreshold) {
      log.warn("Slow Query: " + entry);
    } else {
      log.debug(entry);
    }
  }

  protected String getEntry(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    return this.queryLogEntryCreator.getLogEntry(execInfo, queryInfoList, this.writeDataSourceName, this.writeConnectionId, this.writeIsolation);
  }

}