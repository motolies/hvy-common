package kr.hvy.common.client.Interceptor;

import brave.Tracer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import kr.hvy.common.aop.log.dto.ApiLogCreate;
import kr.hvy.common.aop.log.service.ApiLogService;
import kr.hvy.common.domain.vo.EventLog;
import kr.hvy.common.notify.Notify;
import kr.hvy.common.notify.NotifyRequest;
import kr.hvy.common.security.SecurityUtils;
import kr.hvy.common.util.ApplicationContextUtils;
import lombok.RequiredArgsConstructor;
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
@Component
@RequiredArgsConstructor
public class ApiLogInterceptor implements ClientHttpRequestInterceptor {

  private final Tracer tracer;
  private final Optional<ApiLogService> apiLogService;
  private final Optional<Notify> notify;
  private final Optional<String> defaultErrorChannel;


  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    ClientHttpResponse response = null;

    if (ApplicationContextUtils.isDefaultProfileOnly()) {
      log.info("""
              === Outbound Request ===
              URI         : {}
              Method      : {}
              Headers     : {}
              RequestBody : {}
              """,
          request.getURI(),
          request.getMethod(),
          request.getHeaders(),
          new String(body, StandardCharsets.UTF_8));
    }

    LocalDateTime requestTime = LocalDateTime.now();
    ZonedDateTime start = ZonedDateTime.now();

    try {
      ClientHttpResponse originResponse = execution.execute(request, body);
      response = wrapResponse(originResponse);
    } catch (Exception e) {
      throw e;
    } finally {
      try {
        long duration = Duration.between(start, ZonedDateTime.now()).toMillis();

        if (ApplicationContextUtils.isDefaultProfileOnly()) {
          log.info("""
                  === Outbound Response ===
                  Process Time : {} ms
                  Status code  : {}
                  Headers      : {}
                  Response Body: {}
                  """,
              duration,
              response.getStatusCode(),
              response.getHeaders(),
              new String(StreamUtils.copyToByteArray(response.getBody()), StandardCharsets.UTF_8));
        }

        ApiLogCreate apiLogCreate = ApiLogCreate.builder()
            .traceId(tracer.currentSpan().context().traceIdString())
            .spanId(tracer.currentSpan().context().spanIdString())
            .requestUri(String.valueOf(request.getURI()))
            .httpMethodType(String.valueOf(request.getMethod()))
            .requestHeader(String.valueOf(request.getHeaders()))
            .requestParam(request.getURI().getQuery())
            .requestBody(new String(body, StandardCharsets.UTF_8))
            .responseStatus(response != null ? response.getStatusCode().toString() : null)
            .responseBody(response != null ? new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8) : null)
            .created(EventLog.builder()
                .at(requestTime)
                .by(SecurityUtils.getUsername())
                .build())
            .processTime(duration)
            .build();

        apiLogService.ifPresent(apiLog -> apiLog.save(apiLogCreate));

      } catch (Exception e) {
        log.error("OutboundApiLoggingInterceptor Exception: {}", e.getMessage(), e);
        notify.ifPresent(value -> value.sendMessage(NotifyRequest.builder()
            .channel(defaultErrorChannel.orElse("#hvy-error"))
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
