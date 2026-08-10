package kr.hvy.common.infrastructure.client.rest.Interceptor;

import brave.Span;
import brave.Tracer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import kr.hvy.common.aop.logging.dto.ApiLogCreate;
import kr.hvy.common.aop.logging.service.ApiLogService;
import kr.hvy.common.application.domain.vo.EventLog;
import kr.hvy.common.core.security.SecurityUtils;
import kr.hvy.common.core.util.ApplicationContextUtils;
import kr.hvy.common.infrastructure.notification.slack.Notify;
import kr.hvy.common.infrastructure.notification.slack.NotifyRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Slf4j
//@Profile("!default")
@Component
public class ApiLogInterceptor implements ClientHttpRequestInterceptor {

  /**
   * 본문을 제한 없이 저장한다
   */
  public static final int UNLIMITED_BODY_BYTES = -1;

  private static final String TRUNCATED_SUFFIX = "%n...[truncated] originalBytes=%d, savedBytes=%d";
  private static final String NO_RESPONSE_PREFIX = "[no response] ";

  private final Tracer tracer;
  private final Optional<ApiLogService> apiLogService;
  private final Optional<Notify> notify;
  private final Optional<String> defaultErrorChannel;
  /**
   * api_log 에 저장할 본문 상한(바이트). UNLIMITED_BODY_BYTES 면 제한 없음
   */
  private final int maxBodyBytes;

  /**
   * 스프링이 사용하는 생성자. 기본값이 무제한이라 기존 동작이 그대로 유지된다.
   *
   * 생성자가 둘이라 스프링이 자동으로 고르지 못하므로(private 생성자도 후보에 포함된다) 명시한다.
   */
  @Autowired
  public ApiLogInterceptor(Tracer tracer, Optional<ApiLogService> apiLogService,
      Optional<Notify> notify, Optional<String> defaultErrorChannel) {
    this(tracer, apiLogService, notify, defaultErrorChannel, UNLIMITED_BODY_BYTES);
  }

  private ApiLogInterceptor(Tracer tracer, Optional<ApiLogService> apiLogService,
      Optional<Notify> notify, Optional<String> defaultErrorChannel, int maxBodyBytes) {
    this.tracer = tracer;
    this.apiLogService = apiLogService;
    this.notify = notify;
    this.defaultErrorChannel = defaultErrorChannel;
    this.maxBodyBytes = maxBodyBytes;
  }

  /**
   * 같은 의존성으로 본문 상한만 다른 인스턴스를 만든다.
   *
   * 반환값은 스프링 빈이 아니므로, 타입으로 ApiLogInterceptor 를 주입받는 곳(RestClientConfigurer)과
   * 충돌하지 않는다. 렌더링된 HTML 처럼 응답이 큰 클라이언트에만 상한을 걸 때 쓴다.
   */
  public ApiLogInterceptor withMaxBodyBytes(int maxBodyBytes) {
    return new ApiLogInterceptor(tracer, apiLogService, notify, defaultErrorChannel, maxBodyBytes);
  }

  /**
   * 아웃바운드 호출을 api_log 에 적재한다.
   *
   * 호출자에게 돌려주는 응답은 항상 절삭되지 않은 전체 본문을 담는다. 상한은 로그로 저장할 문자열에만 적용된다.
   */
  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    ClientHttpResponse response = null;
    byte[] responseBody = null;
    String failureSummary = null;

    logRequestOnDefaultProfile(request, body);

    Instant requestTime = Instant.now();

    try {
      ClientHttpResponse originResponse = execution.execute(request, body);
      // 전체 본문을 읽어두고 다시 읽을 수 있도록 감싼다. 이 배열은 절삭하지 않는다
      responseBody = StreamUtils.copyToByteArray(originResponse.getBody());
      response = new ClientHttpResponseWrapper(originResponse, responseBody);
    } catch (Exception e) {
      // 연결 실패/타임아웃도 호출 이력으로 남긴다. 예외는 그대로 호출자에게 전파한다
      failureSummary = NO_RESPONSE_PREFIX + e.getClass().getSimpleName() + ": " + e.getMessage();
      throw e;
    } finally {
      saveApiLog(request, body, response, responseBody, failureSummary, requestTime);
    }

    return response;
  }

  /**
   * api_log 적재. 로그 적재 실패가 실제 호출을 깨뜨리면 안 되므로 모든 예외를 여기서 흡수한다.
   */
  private void saveApiLog(HttpRequest request, byte[] requestBody, ClientHttpResponse response,
      byte[] responseBody, String failureSummary, Instant requestTime) {
    long duration = Duration.between(requestTime, Instant.now()).toMillis();
    logResponseOnDefaultProfile(response, responseBody, failureSummary, duration);

    try {
      // 스케줄러 밖(수동 호출 등)에서는 현재 span 이 없을 수 있다.
      // 가드가 없으면 DTO 조립 전체가 NPE 로 끊겨 api_log 자체가 남지 않는다
      Span currentSpan = tracer.currentSpan();

      ApiLogCreate apiLogCreate = ApiLogCreate.builder()
          .traceId(currentSpan != null ? currentSpan.context().traceIdString() : null)
          .spanId(currentSpan != null ? currentSpan.context().spanIdString() : null)
          .requestUri(String.valueOf(request.getURI()))
          .httpMethodType(String.valueOf(request.getMethod()))
          .requestHeader(String.valueOf(request.getHeaders()))
          .requestParam(request.getURI().getQuery())
          .requestBody(truncateForLog(requestBody, maxBodyBytes))
          .responseStatus(response != null ? String.valueOf(response.getStatusCode()) : null)
          .responseBody(response != null ? truncateForLog(responseBody, maxBodyBytes) : failureSummary)
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

  /**
   * 로컬(default 프로필)에서만 요청 전문을 콘솔에 남긴다.
   *
   * ApplicationContextUtils 는 정적 컨텍스트에 의존해 기동 초기에는 비어 있을 수 있다.
   * 콘솔 로그 실패가 실제 호출이나 api_log 적재를 막으면 안 되므로 예외를 흡수한다.
   */
  private void logRequestOnDefaultProfile(HttpRequest request, byte[] body) {
    try {
      if (!ApplicationContextUtils.isDefaultProfileOnly()) {
        return;
      }
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
          truncateForLog(body, maxBodyBytes));
    } catch (Exception e) {
      log.debug("아웃바운드 요청 콘솔 로그 생략: {}", e.getMessage());
    }
  }

  /**
   * 로컬(default 프로필)에서만 응답 전문을 콘솔에 남긴다. 응답이 없으면 실패 요약을 출력한다.
   */
  private void logResponseOnDefaultProfile(ClientHttpResponse response, byte[] responseBody,
      String failureSummary, long duration) {
    try {
      if (!ApplicationContextUtils.isDefaultProfileOnly()) {
        return;
      }
      log.info("""
              === Outbound Response ===
              Process Time : {} ms
              Status code  : {}
              Headers      : {}
              Response Body: {}
              """,
          duration,
          response != null ? response.getStatusCode() : "N/A",
          response != null ? response.getHeaders() : "N/A",
          response != null ? truncateForLog(responseBody, maxBodyBytes) : failureSummary);
    } catch (Exception e) {
      log.debug("아웃바운드 응답 콘솔 로그 생략: {}", e.getMessage());
    }
  }

  /**
   * 로그로 저장할 본문 문자열을 만든다. 상한을 넘으면 잘라내고 원본 크기를 접미사로 남긴다.
   *
   * 저장/전송량을 줄이는 것이 목적이므로 문자수가 아니라 바이트 기준으로 자른다. 다만 임의 위치에서 자르면
   * UTF-8 멀티바이트 문자가 쪼개져 대체문자가 남으므로 온전하게 디코딩되는 문자까지만 취한다.
   */
  static String truncateForLog(byte[] body, int maxBodyBytes) {
    if (body == null || body.length == 0) {
      return null;
    }
    if (maxBodyBytes == UNLIMITED_BODY_BYTES || body.length <= maxBodyBytes) {
      return new String(body, StandardCharsets.UTF_8);
    }

    CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.IGNORE)
        .onUnmappableCharacter(CodingErrorAction.IGNORE);
    String head;
    try {
      head = decoder.decode(ByteBuffer.wrap(body, 0, maxBodyBytes)).toString();
    } catch (CharacterCodingException e) {
      head = new String(body, 0, maxBodyBytes, StandardCharsets.UTF_8);
    }
    return head + TRUNCATED_SUFFIX.formatted(body.length, maxBodyBytes);
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
    public HttpStatusCode getStatusCode() throws IOException {
      // 비표준 상태코드에서 예외가 나지 않도록 HttpStatus.valueOf 대신 위임한다
      return originalResponse.getStatusCode();
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
    public InputStream getBody() {
      // 이미 읽어둔 body(byte[])로부터 새로 InputStream 생성
      return new ByteArrayInputStream(body);
    }

    @Override
    public HttpHeaders getHeaders() {
      return originalResponse.getHeaders();
    }
  }
}
