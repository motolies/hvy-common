package kr.hvy.common.infrastructure.client.rest.Interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import brave.Tracing;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import kr.hvy.common.aop.logging.dto.ApiLogCreate;
import kr.hvy.common.aop.logging.service.ApiLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

@DisplayName("ApiLogInterceptor - 본문 절삭과 원본 보존")
class ApiLogInterceptorTest {

  private static final URI TARGET_URI = URI.create("http://chromium:3000/content?stealth=true");

  private Tracing tracing;
  private ApiLogService apiLogService;

  @BeforeEach
  void setUp() {
    // TraceContext 가 final 이라 mock 대신 실제 Tracer 를 쓴다
    tracing = Tracing.newBuilder().build();
    apiLogService = mock(ApiLogService.class);
  }

  @AfterEach
  void tearDown() {
    tracing.close();
  }

  /**
   * 지정한 본문 상한을 가진 인터셉터를 만든다.
   */
  private ApiLogInterceptor newInterceptor(int maxBodyBytes) {
    ApiLogInterceptor interceptor = new ApiLogInterceptor(
        tracing.tracer(), Optional.of(apiLogService), Optional.empty(), Optional.empty());
    return maxBodyBytes == ApiLogInterceptor.UNLIMITED_BODY_BYTES
        ? interceptor
        : interceptor.withMaxBodyBytes(maxBodyBytes);
  }

  private ApiLogCreate captureSavedLog() {
    ArgumentCaptor<ApiLogCreate> captor = ArgumentCaptor.forClass(ApiLogCreate.class);
    verify(apiLogService).save(captor.capture());
    return captor.getValue();
  }

  @Nested
  @DisplayName("truncateForLog")
  class TruncateForLog {

    @Test
    @DisplayName("상한 이하면 원본을 그대로 반환한다")
    void 상한이하_원본그대로() {
      byte[] body = "hello".getBytes(StandardCharsets.UTF_8);

      assertThat(ApiLogInterceptor.truncateForLog(body, 100)).isEqualTo("hello");
    }

    @Test
    @DisplayName("무제한이면 크기와 무관하게 원본을 반환한다")
    void 무제한_원본그대로() {
      byte[] body = "x".repeat(10_000).getBytes(StandardCharsets.UTF_8);

      String result = ApiLogInterceptor.truncateForLog(body, ApiLogInterceptor.UNLIMITED_BODY_BYTES);

      assertThat(result).hasSize(10_000).doesNotContain("truncated");
    }

    @Test
    @DisplayName("상한 초과 시 접미사에 원본 크기를 남긴다")
    void 상한초과_접미사에원본크기포함() {
      byte[] body = "x".repeat(1_000).getBytes(StandardCharsets.UTF_8);

      String result = ApiLogInterceptor.truncateForLog(body, 100);

      assertThat(result).startsWith("x".repeat(100));
      assertThat(result).contains("originalBytes=1000");
      assertThat(result).contains("savedBytes=100");
    }

    @Test
    @DisplayName("멀티바이트 문자 경계에서 잘려도 대체문자가 남지 않는다")
    void 상한초과_UTF8경계_대체문자없음() {
      // "가" 는 UTF-8 로 3바이트. 4바이트에서 자르면 두 번째 글자가 쪼개진다
      byte[] body = "가".repeat(10).getBytes(StandardCharsets.UTF_8);

      String result = ApiLogInterceptor.truncateForLog(body, 4);

      assertThat(result).doesNotContain("�");
      assertThat(result).startsWith("가");
    }

    @Test
    @DisplayName("null 과 빈 배열은 null 을 반환한다")
    void null과빈배열_null반환() {
      assertThat(ApiLogInterceptor.truncateForLog(null, 100)).isNull();
      assertThat(ApiLogInterceptor.truncateForLog(new byte[0], 100)).isNull();
    }
  }

  @Nested
  @DisplayName("intercept")
  class Intercept {

    @Test
    @DisplayName("응답 본문이 상한을 넘어도 호출자에게는 전체가 전달된다")
    void 상한초과여도_호출자에게는전체가전달된다() throws IOException {
      byte[] fullBody = "y".repeat(50_000).getBytes(StandardCharsets.UTF_8);
      ApiLogInterceptor interceptor = newInterceptor(1_000);

      ClientHttpResponse response = interceptor.intercept(
          new MockClientHttpRequest(HttpMethod.POST, TARGET_URI),
          "{}".getBytes(StandardCharsets.UTF_8),
          execution(fullBody, HttpStatus.OK));

      // 응답 경로: 절삭되지 않은 전체
      assertThat(response.getBody().readAllBytes()).hasSize(50_000);
      // 로그 경로: 절삭됨
      assertThat(captureSavedLog().getResponseBody())
          .contains("originalBytes=50000")
          .hasSizeLessThan(2_000);
    }

    @Test
    @DisplayName("응답 본문을 두 번 이상 읽을 수 있다")
    void 응답본문_반복읽기가능() throws IOException {
      byte[] fullBody = "body".getBytes(StandardCharsets.UTF_8);
      ApiLogInterceptor interceptor = newInterceptor(ApiLogInterceptor.UNLIMITED_BODY_BYTES);

      ClientHttpResponse response = interceptor.intercept(
          new MockClientHttpRequest(HttpMethod.POST, TARGET_URI),
          new byte[0],
          execution(fullBody, HttpStatus.OK));

      // MockClientHttpResponse 는 1회용 스트림이므로, 반복해서 읽힌다면 래퍼가 실제로 동작한 것이다
      assertThat(response.getBody().readAllBytes()).isEqualTo(fullBody);
      assertThat(response.getBody().readAllBytes()).isEqualTo(fullBody);
    }

    @Test
    @DisplayName("현재 span 이 없으면 traceId 는 null 이지만 적재는 수행된다")
    void span없음_traceId는null이지만저장된다() throws IOException {
      ApiLogInterceptor interceptor = newInterceptor(ApiLogInterceptor.UNLIMITED_BODY_BYTES);

      interceptor.intercept(
          new MockClientHttpRequest(HttpMethod.POST, TARGET_URI),
          new byte[0],
          execution("ok".getBytes(StandardCharsets.UTF_8), HttpStatus.OK));

      ApiLogCreate saved = captureSavedLog();
      assertThat(saved.getTraceId()).isNull();
      assertThat(saved.getResponseStatus()).contains("200");
    }

    @Test
    @DisplayName("호출 중 IOException 은 그대로 전파되고 실패 이력이 남는다")
    void 호출실패_예외전파되고실패이력저장() {
      ApiLogInterceptor interceptor = newInterceptor(ApiLogInterceptor.UNLIMITED_BODY_BYTES);
      ClientHttpRequestExecution failing = (request, body) -> {
        throw new IOException("Connection refused");
      };

      assertThatThrownBy(() -> interceptor.intercept(
          new MockClientHttpRequest(HttpMethod.POST, TARGET_URI), new byte[0], failing))
          .isInstanceOf(IOException.class)
          .hasMessage("Connection refused");

      ApiLogCreate saved = captureSavedLog();
      assertThat(saved.getResponseStatus()).isNull();
      assertThat(saved.getResponseBody()).startsWith("[no response] IOException: Connection refused");
    }

    @Test
    @DisplayName("ApiLogService 가 없어도 응답을 정상 반환한다")
    void 서비스없음_응답정상반환() throws IOException {
      ApiLogInterceptor interceptor = new ApiLogInterceptor(
          tracing.tracer(), Optional.empty(), Optional.empty(), Optional.empty());

      ClientHttpResponse response = interceptor.intercept(
          new MockClientHttpRequest(HttpMethod.POST, TARGET_URI),
          new byte[0],
          execution("ok".getBytes(StandardCharsets.UTF_8), HttpStatus.OK));

      assertThat(response.getBody().readAllBytes()).isEqualTo("ok".getBytes(StandardCharsets.UTF_8));
      verify(apiLogService, org.mockito.Mockito.never()).save(any());
    }
  }

  @Test
  @DisplayName("withMaxBodyBytes 는 원본 인스턴스를 변경하지 않는다")
  void withMaxBodyBytes_원본불변() throws IOException {
    ApiLogInterceptor origin = new ApiLogInterceptor(
        tracing.tracer(), Optional.of(apiLogService), Optional.empty(), Optional.empty());
    ApiLogInterceptor limited = origin.withMaxBodyBytes(10);

    assertThat(limited).isNotSameAs(origin);

    byte[] fullBody = "z".repeat(1_000).getBytes(StandardCharsets.UTF_8);
    origin.intercept(new MockClientHttpRequest(HttpMethod.POST, TARGET_URI), new byte[0],
        execution(fullBody, HttpStatus.OK));

    // 원본은 여전히 무제한이라 절삭 접미사가 없어야 한다
    assertThat(captureSavedLog().getResponseBody()).hasSize(1_000).doesNotContain("truncated");
  }

  /**
   * 지정한 본문과 상태코드를 돌려주는 실행 stub. MockClientHttpResponse 는 스트림을 한 번만 제공한다.
   */
  private ClientHttpRequestExecution execution(byte[] responseBody, HttpStatus status) {
    return (request, body) -> new MockClientHttpResponse(responseBody, status);
  }
}
