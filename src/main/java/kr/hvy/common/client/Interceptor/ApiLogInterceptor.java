package kr.hvy.common.client.Interceptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import kr.hvy.common.notify.Notify;
import kr.hvy.common.notify.NotifyRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Slf4j
//@Profile("!default")
//@Component
public class ApiLogInterceptor implements ClientHttpRequestInterceptor {

  protected final Optional<Notify> notify;
  protected final String defaultErrorChannel;


  public ApiLogInterceptor(Optional<Notify> notify, String defaultErrorChannel) {
    this.notify = notify;
    this.defaultErrorChannel = defaultErrorChannel;
  }

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    LocalDateTime createdOn = LocalDateTime.now();
    long start = System.currentTimeMillis();
    ClientHttpResponse response = null;

    // 요청 로깅
    log.info("=== Outbound Request ===");
    log.info("URI         : {}", request.getURI());
    log.info("Method      : {}", request.getMethod());
    log.info("Headers     : {}", request.getHeaders());
    log.info("RequestBody : {}", new String(body, StandardCharsets.UTF_8));

    try {
      ClientHttpResponse originResponse = execution.execute(request, body);
      response = wrapResponse(originResponse);
    } catch (Exception e) {
      throw e;
    } finally {
      long end = System.currentTimeMillis();

      try {
        // todo : 로깅 부분 작성해야 함 비동기로 저장시킴
        // 응답 로깅
        log.info("=== Outbound Response ===");
        log.info("Status code  : {}", response.getStatusCode());
        log.info("Headers      : {}", response.getHeaders());
        log.info("Response Body: {}", new String(StreamUtils.copyToByteArray(response.getBody()), StandardCharsets.UTF_8));
      } catch (Exception e) {
        log.error("OutboundApiLoggingInterceptor Exception: {}", e.getMessage(), e);
        notify.ifPresent(value -> value.sendMessage(NotifyRequest.builder()
            .channel(defaultErrorChannel)
            .exception(e)
            .build()));
      }
    }

    return response;
  }

  /**
   * 다시 읽을 수 있도록 래핑해서 반환.
   */
  private ClientHttpResponse wrapResponse(ClientHttpResponse response) throws IOException {
    return new ClientHttpResponseWrapper(response, StreamUtils.copyToByteArray(response.getBody()));
  }

  /**
   * 실제 응답 객체를 감싸서, Body를 우리가 가진 byte[]로 대체해주는 래퍼 클래스
   */
  private static class ClientHttpResponseWrapper implements ClientHttpResponse {

    private final ClientHttpResponse originalResponse;
    private final byte[] body;

    public ClientHttpResponseWrapper(ClientHttpResponse originalResponse, byte[] body) {
      this.originalResponse = originalResponse;
      this.body = body;
    }

    @Override
    public HttpStatus getStatusCode() throws IOException {
      return HttpStatus.valueOf(originalResponse.getStatusCode().value());
    }

    @Override
    public String getStatusText() throws IOException {
      return originalResponse.getStatusText();
    }

    @Override
    public void close() {
      // 원본 response 닫기
      originalResponse.close();
    }

    @Override
    public InputStream getBody() throws IOException {
      // 이미 읽어둔 body(byte[])로부터 새로 InputStream 생성
      return new ByteArrayInputStream(body);
    }

    @Override
    public org.springframework.http.HttpHeaders getHeaders() {
      return originalResponse.getHeaders();
    }
  }
}
