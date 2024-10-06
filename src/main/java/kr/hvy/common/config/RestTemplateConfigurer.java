package kr.hvy.common.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import kr.hvy.common.Interceptor.LoggingRequestInterceptor;
import kr.hvy.common.Interceptor.UserAgentRequestInterceptor;
import kr.hvy.common.code.RestConfigType;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;


@Deprecated(since = "Spring 6.1 부터 RestClient로 변경")
@Configuration
public class RestTemplateConfigurer {

  private static final int CONNECT_TIMEOUT = 30;
  private static final int READ_TIME_OUT = 60;

  public RestTemplate restTemplate() {
    return restTemplate(CONNECT_TIMEOUT, READ_TIME_OUT, EnumSet.of(RestConfigType.NONE));
  }

  public RestTemplate restTemplate(int connectTimeout, int readTimeOut) {
    return restTemplate(connectTimeout, readTimeOut, EnumSet.of(RestConfigType.NONE));
  }

  public RestTemplate restTemplate(EnumSet<RestConfigType> restConfigType) {
    return restTemplate(CONNECT_TIMEOUT, READ_TIME_OUT, restConfigType);
  }

  public RestTemplate restTemplate(int connectTimeout, int readTimeOut, EnumSet<RestConfigType> restConfigType) {
    return restTemplate(connectTimeout, readTimeOut, null, restConfigType);
  }

  public RestTemplate restTemplate(int connectTimeout, int readTimeOut, List<ClientHttpRequestInterceptor> clientHttpRequestInterceptorList, EnumSet<RestConfigType> restConfigType) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

    if (clientHttpRequestInterceptorList == null) {
      clientHttpRequestInterceptorList = new ArrayList<>();
    }

    if (restConfigType.contains(RestConfigType.USER_AGENT)) {
      clientHttpRequestInterceptorList.add(userAgentRequestInterceptor());
    }

    if (restConfigType.contains(RestConfigType.LOGGING)) {
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