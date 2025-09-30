package kr.hvy.common.infrastructure.database.logging;

import java.sql.ResultSet;
import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.proxy.ProxyConfig;
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogic;
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogicFactory;

/**
 * ResultSetTableLogger를 생성하는 팩토리 클래스
 */
public class ResultSetTableLoggerFactory implements ResultSetProxyLogicFactory {

  private final DataSourceProxySettingProperty property;

  public ResultSetTableLoggerFactory(DataSourceProxySettingProperty property) {
    this.property = property;
  }

  @Override
  public ResultSetProxyLogic create(ResultSet resultSet, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
    return new ResultSetTableLogger(resultSet, connectionInfo, proxyConfig, property);
  }
}