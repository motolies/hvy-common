package kr.hvy.common.config;

import java.util.EnumSet;
import kr.hvy.common.Interceptor.LoggingRequestInterceptor;
import kr.hvy.common.Interceptor.UserAgentRequestInterceptor;
import kr.hvy.common.code.RestConfigType;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public class RestClientConfigurer {

  private static final int CONNECT_TIMEOUT = 30;
  private static final int READ_TIME_OUT = 60;
  private static final int MAX_TOTAL_CONNECTIONS = 100;
  private static final int MAX_CONNECTIONS_PER_ROUTE = 20;

  public RestClient restClient() {
    return restClient(CONNECT_TIMEOUT, READ_TIME_OUT, EnumSet.of(RestConfigType.NONE));
  }

  public RestClient restClient(int connectTimeout, int readTimeOut) {
    return restClient(connectTimeout, readTimeOut, EnumSet.of(RestConfigType.NONE));
  }

  public RestClient restClient(EnumSet<RestConfigType> restClientConfigType) {
    return restClient(CONNECT_TIMEOUT, READ_TIME_OUT, restClientConfigType);
  }

  public RestClient restClient(int connectTimeout, int readTimeOut, EnumSet<RestConfigType> restClientConfigType) {
    HttpComponentsClientHttpRequestFactory requestFactory = createRequestFactory(connectTimeout, readTimeOut);

    RestClient.Builder builder = RestClient.builder()
        .requestFactory(requestFactory);

    if (restClientConfigType.contains(RestConfigType.USER_AGENT)) {
      builder.requestInterceptor(userAgentRequestInterceptor());
    }

    if (restClientConfigType.contains(RestConfigType.LOGGING)) {
      builder.requestInterceptor(loggingRequestInterceptor());
    }

    return builder.build();
  }

  private HttpComponentsClientHttpRequestFactory createRequestFactory(int connectTimeout, int readTimeout) {
    int keepAliveTime = readTimeout + 1;

    // 소켓 설정 (리드 타임아웃 설정)
    SocketConfig socketConfig = SocketConfig.custom()
        .setSoTimeout(Timeout.ofSeconds(readTimeout)) // 리드 타임아웃
        .build();

    // 커넥션 풀 설정
    PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(MAX_TOTAL_CONNECTIONS) // 최대 전체 커넥션 수
        .setMaxConnPerRoute(MAX_CONNECTIONS_PER_ROUTE) // 라우트당 최대 커넥션 수
        .setDefaultSocketConfig(socketConfig)
        .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create().build())
        .build();

    // 커넥션 풀 내 커넥션의 유효 시간 설정
    connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
    connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
    connectionManager.setValidateAfterInactivity(TimeValue.ofSeconds(keepAliveTime));

    // 클라이언트 생성 (커넥션 타임아웃, 리드 타임아웃 설정)
    CloseableHttpClient httpClient = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(org.apache.hc.client5.http.config.RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(connectTimeout)) // 커넥션 타임아웃
            .setResponseTimeout(Timeout.ofSeconds(readTimeout))   // 리드 타임아웃
            .build())
        .build();

    return new HttpComponentsClientHttpRequestFactory(httpClient);
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
