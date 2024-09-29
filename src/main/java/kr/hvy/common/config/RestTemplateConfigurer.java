package kr.hvy.common.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import kr.hvy.common.Interceptor.LoggingRequestInterceptor;
import kr.hvy.common.Interceptor.UserAgentRequestInterceptor;
import kr.hvy.common.code.RestTemplateConfigType;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfigurer {

  private static final int CONNECT_TIMEOUT = 30;
  private static final int READ_TIME_OUT = 60;

  public RestTemplate restTemplate() {
    return restTemplate(CONNECT_TIMEOUT, READ_TIME_OUT, EnumSet.of(RestTemplateConfigType.NONE));
  }

  public RestTemplate restTemplate(int connectTimeout, int readTimeOut) {
    return restTemplate(connectTimeout, readTimeOut, EnumSet.of(RestTemplateConfigType.NONE));
  }

  public RestTemplate restTemplate(EnumSet<RestTemplateConfigType> restTemplateConfigType) {
    return restTemplate(CONNECT_TIMEOUT, READ_TIME_OUT, restTemplateConfigType);
  }

  public RestTemplate restTemplate(int connectTimeout, int readTimeOut, EnumSet<RestTemplateConfigType> restTemplateConfigType) {
    return restTemplate(connectTimeout, readTimeOut, null, restTemplateConfigType);
  }

  public RestTemplate restTemplate(int connectTimeout, int readTimeOut, List<ClientHttpRequestInterceptor> clientHttpRequestInterceptorList, EnumSet<RestTemplateConfigType> restTemplateConfigType) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

    if (clientHttpRequestInterceptorList == null) {
      clientHttpRequestInterceptorList = new ArrayList<>();
    }

    if (restTemplateConfigType.contains(RestTemplateConfigType.USER_AGENT)) {
      clientHttpRequestInterceptorList.add(userAgentRequestInterceptor());
    }

    if (restTemplateConfigType.contains(RestTemplateConfigType.LOGGING)) {
      clientHttpRequestInterceptorList.add(loggingRequestInterceptor());
    }

    return new RestTemplateBuilder()
        .requestFactory(() -> new BufferingClientHttpRequestFactory(requestFactory))
        .interceptors(clientHttpRequestInterceptorList)
        .setConnectTimeout(Duration.ofSeconds(connectTimeout))
        .setReadTimeout(Duration.ofSeconds(readTimeOut))
        .build();
  }

  @Bean
  public LoggingRequestInterceptor loggingRequestInterceptor() {
    return new LoggingRequestInterceptor();
  }

  @Bean
  public UserAgentRequestInterceptor userAgentRequestInterceptor() {
    return new UserAgentRequestInterceptor();
  }
}