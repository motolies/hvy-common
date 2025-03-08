package kr.hvy.common.db.log;

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
    if (property.isWrap() && bean instanceof DataSource && property.getNames().contains(beanName)) {
      return ProxyDataSourceBuilder
          .create((DataSource) bean)
          .name("proxy-" + beanName.toUpperCase())
          .listener(new CustomQueryLoggingListener(property))
          .build();
    }
    return bean;
  }
}