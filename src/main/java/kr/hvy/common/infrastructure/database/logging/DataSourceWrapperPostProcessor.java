package kr.hvy.common.infrastructure.database.logging;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSourceWrapperPostProcessor implements BeanPostProcessor {

  private final DataSourceProxySettingProperty property;

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (property.isEnableWrapper() && bean instanceof DataSource && property.getDataSourceNames().contains(beanName)) {
      ProxyDataSourceBuilder builder = ProxyDataSourceBuilder
          .create((DataSource) bean)
          .name("proxy-" + beanName)
          .listener(new CustomQueryLoggingListener(property));

      // ResultSet 테이블 로깅이 활성화된 경우 팩토리 추가
      if (property.isEnableResultSetLogging()) {
        builder.proxyResultSet(new ResultSetTableLoggerFactory(property));
      }

      return builder.build();
    }
    return bean;
  }
}