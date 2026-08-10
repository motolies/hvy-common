package kr.hvy.common.infrastructure.client.config;

import java.util.List;
import kr.hvy.common.infrastructure.client.rest.Interceptor.ApiLogInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Slf4j
public class RestClientConfigurer {

  private static final int MAX_TOTAL_CONNECTIONS = 100;
  private static final int MAX_CONNECTIONS_PER_ROUTE = 20;
  private static final int CONNECT_TIMEOUT = 30;
  private static final int READ_TIME_OUT = 60;

  @Autowired(required = false)
  private ApiLogInterceptor apiLogInterceptor;

  protected RestClient restClient() {
    return restClient(CONNECT_TIMEOUT, READ_TIME_OUT);
  }

  protected RestClient restClient(int connectTimeout, int readTimeOut) {
    return restClient(MAX_TOTAL_CONNECTIONS, MAX_CONNECTIONS_PER_ROUTE, connectTimeout, readTimeOut, null, null);
  }

  protected RestClient restClient(int connectTimeout, int readTimeOut, String baseUrl) {
    return restClient(MAX_TOTAL_CONNECTIONS, MAX_CONNECTIONS_PER_ROUTE, connectTimeout, readTimeOut, baseUrl, null);
  }

  protected RestClient restClient(int connectTimeout, int readTimeOut, List<ClientHttpRequestInterceptor> interceptors) {
    return restClient(MAX_TOTAL_CONNECTIONS, MAX_CONNECTIONS_PER_ROUTE, connectTimeout, readTimeOut, null, interceptors);
  }

  protected RestClient restClient(int connectTimeout, int readTimeOut, String baseUrl, List<ClientHttpRequestInterceptor> interceptors) {
    return restClient(MAX_TOTAL_CONNECTIONS, MAX_CONNECTIONS_PER_ROUTE, connectTimeout, readTimeOut, baseUrl, interceptors);
  }

  protected RestClient restClient(int maxTotal, int maxPerRoute, int connectTimeout, int readTimeOut, String baseUrl) {
    return restClient(maxTotal, maxPerRoute, connectTimeout, readTimeOut, baseUrl, null);
  }

  /**
   * api_log 에 저장할 본문 상한(바이트)을 지정한다. 렌더링된 HTML 처럼 응답이 큰 대상에 사용한다.
   */
  protected RestClient restClient(int connectTimeout, int readTimeOut, String baseUrl, int maxLogBodyBytes) {
    return restClient(MAX_TOTAL_CONNECTIONS, MAX_CONNECTIONS_PER_ROUTE, connectTimeout, readTimeOut, baseUrl, null, maxLogBodyBytes);
  }

  protected RestClient restClient(int maxTotal, int maxPerRoute, int connectTimeout, int readTimeOut, String baseUrl, List<ClientHttpRequestInterceptor> interceptors) {
    return restClient(maxTotal, maxPerRoute, connectTimeout, readTimeOut, baseUrl, interceptors, ApiLogInterceptor.UNLIMITED_BODY_BYTES);
  }


  protected RestClient restClient(int maxTotal, int maxPerRoute, int connectTimeout, int readTimeOut, String baseUrl,
      List<ClientHttpRequestInterceptor> interceptors, int maxLogBodyBytes) {

    // TCP 연결 수립 타임아웃. RequestConfig 가 아니라 ConnectionConfig 소관이며,
    // 지정하지 않으면 기본 3분이 적용된다
    ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setConnectTimeout(Timeout.ofSeconds(connectTimeout))
        .build();

    // 커넥션 풀 생성
    PoolingHttpClientConnectionManager connectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(connectionConfig)
            .setMaxConnTotal(maxTotal)            // 전체 커넥션 풀 수
            .setMaxConnPerRoute(maxPerRoute)      // 특정 호스트 당 커넥션 풀 수
            .build();

    // 요청/응답 타임아웃 설정
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(Timeout.ofSeconds(connectTimeout))   // 풀에서 커넥션을 빌리기까지 대기 시간
        .setResponseTimeout(Timeout.ofSeconds(readTimeOut))     // 데이터 읽기(소켓) 타임아웃
        .build();

    // 커스텀 HttpClient 빌드
    // 기본 재시도 전략은 429/503 을 1초 뒤 재시도하는데, 재시도가 인터셉터 아래 계층에서 일어나
    // api_log 에 남지 않는다. 'api_log 1건 = 실제 호출 1회' 를 지키기 위해 끈다
    HttpClientBuilder clientBuilder = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .disableAutomaticRetries();

    // 실제 요청을 수행할 RestClient 구현체 생성
    // RestClient는 사용자가 정의한 커스텀 객체라고 가정
    // httpClient와 baseUrl 등을 주입해 초기화
    RestClient.Builder builder = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(new HttpComponentsClientHttpRequestFactory(clientBuilder.build()))
        .defaultStatusHandler(
            HttpStatusCode::is4xxClientError,
            (request, response) -> {
              log.error("Client Error - Code: {}, Message: {}", response.getStatusCode(), new String(response.getBody().readAllBytes()));
            })
        .defaultStatusHandler(
            HttpStatusCode::is5xxServerError,
            (request, response) -> {
              log.error("Server Error - Code: {}, Message: {}", response.getStatusCode(), new String(response.getBody().readAllBytes()));
            });

    // log interceptor 추가. 본문 상한이 지정되면 상한만 다른 복제 인스턴스를 붙인다
    if (apiLogInterceptor != null) {
      builder.requestInterceptor(maxLogBodyBytes == ApiLogInterceptor.UNLIMITED_BODY_BYTES
          ? apiLogInterceptor
          : apiLogInterceptor.withMaxBodyBytes(maxLogBodyBytes));
    }

    // 사용자 정의 interceptor 추가
    if (CollectionUtils.isNotEmpty(interceptors)) {
      builder.requestInterceptors(interceptorList -> interceptorList.addAll(interceptors));
    }

    return builder.build();
  }


}
