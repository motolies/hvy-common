package kr.hvy.common.infrastructure.notification.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.ErrorResponseMetadata;
import io.micrometer.tracing.Tracer;
import java.io.IOException;
import java.util.List;
import kr.hvy.common.infrastructure.notification.slack.message.SimpleTextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 전송 결과 계약 — Slack 이 메시지를 받았을 때만 true 이고, 실패의 두 모양(예외 · HTTP 200 의 ok=false) 모두 예외 없이 false 다.
 * Slack 은 호출하지 않는다: Web API 클라이언트를 목으로 바꿔 응답만 정한다.
 */
@DisplayName("SlackClient - 전송 결과를 돌려주고 전송 실패로는 예외를 던지지 않는다")
class SlackClientTest {

  private final MethodsClient methods = mock(MethodsClient.class);
  private final Tracer tracer = mock(Tracer.class);
  private SlackClient client;

  @BeforeEach
  void setUp() {
    client = new SlackClient(new SlackProperty(), tracer);
    // 실제 클라이언트는 @PostConstruct 가 토큰으로 만든다 — 테스트에서는 목으로 바꾼다
    ReflectionTestUtils.setField(client, "client", methods);
  }

  /** chat.postMessage 응답 스텁 */
  private static ChatPostMessageResponse response(boolean ok, String error) {
    ChatPostMessageResponse response = new ChatPostMessageResponse();
    response.setOk(ok);
    response.setError(error);
    return response;
  }

  private static SimpleTextMessage text() {
    return SimpleTextMessage.builder().channel("#hvy-notify").message("안녕").build();
  }

  @Test
  @DisplayName("Slack 이 ok=true 로 답하면 true — 채널·대체 텍스트가 요청에 실린다")
  void ok면_true() throws Exception {
    when(methods.chatPostMessage(any(ChatPostMessageRequest.class))).thenReturn(response(true, null));

    assertThat(client.sendMessage(text())).isTrue();

    ArgumentCaptor<ChatPostMessageRequest> captor = ArgumentCaptor.forClass(ChatPostMessageRequest.class);
    verify(methods).chatPostMessage(captor.capture());
    assertThat(captor.getValue().getChannel()).isEqualTo("#hvy-notify");
    assertThat(captor.getValue().getText()).contains("안녕");
  }

  @Test
  @DisplayName("HTTP 200 이어도 ok=false(channel_not_found·invalid_blocks 등)면 false — 예외는 던지지 않는다")
  void 거부면_false() throws Exception {
    ChatPostMessageResponse rejected = response(false, "invalid_blocks");
    ErrorResponseMetadata metadata = new ErrorResponseMetadata();
    metadata.setMessages(List.of("[ERROR] must provide a string [json-pointer:/blocks/0/text/text]"));
    rejected.setResponseMetadata(metadata);
    when(methods.chatPostMessage(any(ChatPostMessageRequest.class))).thenReturn(rejected, response(false, "channel_not_found"));

    assertThat(client.sendMessage(text())).as("상세 사유(response_metadata)가 있는 거부").isFalse();
    assertThat(client.sendMessage(text())).as("상세 사유가 없는 거부").isFalse();
  }

  @Test
  @DisplayName("전송 예외(네트워크 IOException · HTTP 오류 SlackApiException)는 삼키고 false")
  void 예외면_false() throws Exception {
    when(methods.chatPostMessage(any(ChatPostMessageRequest.class)))
        .thenThrow(new IOException("connection reset"))
        .thenThrow(mock(SlackApiException.class));

    assertThat(client.sendMessage(text())).isFalse();
    assertThat(client.sendMessage(text())).isFalse();
  }

  @Test
  @DisplayName("NotifyRequest 경로도 같은 결과를 돌려준다 — 텍스트 요청과 예외가 실린 요청(추적 span 이 없어도 됨) 모두")
  void 요청_경로도_결과를_돌려준다() throws Exception {
    when(methods.chatPostMessage(any(ChatPostMessageRequest.class))).thenReturn(response(true, null), response(false, "invalid_auth"));

    boolean delivered = client.sendMessage(NotifyRequest.builder().channel("#hvy-notify").message("텍스트").build());
    boolean rejected = client.sendMessage(NotifyRequest.builder().channel("#hvy-error").exception(new IllegalStateException("boom")).isNotify(true).build());

    assertThat(delivered).isTrue();
    assertThat(rejected).isFalse();
  }
}
