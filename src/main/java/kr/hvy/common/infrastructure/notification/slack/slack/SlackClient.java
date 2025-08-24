package kr.hvy.common.infrastructure.notification.slack.slack;

import static com.slack.api.model.block.Blocks.asBlocks;
import static com.slack.api.model.block.Blocks.header;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.Attachment;
import com.slack.api.model.block.LayoutBlock;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import kr.hvy.common.infrastructure.notification.slack.Notify;
import kr.hvy.common.infrastructure.notification.slack.NotifyRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackClient implements Notify {

  private final SlackProperty slackProperty;
  private MethodsClient client;

  @PostConstruct
  public void init() {
    client = Slack.getInstance().methods(slackProperty.getToken());
  }

  @Override
  public void sendMessage(NotifyRequest request) {
    ChatPostMessageRequest chatPostMessageRequest = buildChatPostMessageRequest(request);
    send(chatPostMessageRequest);
  }

  private void send(ChatPostMessageRequest request) {
    try {
      client.chatPostMessage(request);
    } catch (SlackApiException | IOException e) {
      log.error("Slack 메시지 전송 실패: {}", e.getMessage(), e);
    }
  }

  private ChatPostMessageRequest buildChatPostMessageRequest(NotifyRequest request) {
    if (ObjectUtils.isNotEmpty(request.getException())) {
      return buildErrorMessageRequest(request);
    } else {
      return buildTextMessageRequest(request);
    }
  }

  private ChatPostMessageRequest buildErrorMessageRequest(NotifyRequest request) {
    List<LayoutBlock> blocks = buildErrorBlocks(request.getException(), request.isNotify());
    Attachment attachment = Attachment.builder()
        .fallback(request.getException().getMessage())
        .color("#ff0000")
        .text(Arrays.toString(request.getException().getStackTrace()))
        .build();

    return ChatPostMessageRequest.builder()
        .channel(request.getChannel())
        .text(request.getException().getMessage())
        .blocks(blocks)
        .attachments(Collections.singletonList(attachment))
        .build();
  }

  private ChatPostMessageRequest buildTextMessageRequest(NotifyRequest request) {
    List<LayoutBlock> blocks = buildTextBlocks(request.getMessage(), request.isNotify());
    return ChatPostMessageRequest.builder()
        .channel(request.getChannel())
        .text(request.getMessage())
        .blocks(blocks)
        .build();
  }

  private List<LayoutBlock> buildTextBlocks(String message, boolean isChannel) {
    List<LayoutBlock> blocks = new ArrayList<>();
    if (isChannel) {
      blocks.add(section(section -> section.text(markdownText("<!channel>"))));
    }
    blocks.add(section(section -> section.text(markdownText(message))));
    return blocks;
  }

  private List<LayoutBlock> buildErrorBlocks(Exception e, boolean isChannel) {
    String packageName = e.getStackTrace()[0].getClassName();
    String className = packageName.substring(packageName.lastIndexOf(".") + 1);
    String methodName = e.getStackTrace()[0].getMethodName();
    int lineNumber = e.getStackTrace()[0].getLineNumber();

    return asBlocks(
        Stream.of(
                isChannel ? section(section -> section.text(markdownText("<!channel>"))) : null,
                header(header -> header.text(plainText(e.getMessage()))),
                section(section -> section.text(markdownText(MessageFormat.format("*Package* {0}", packageName)))),
                section(section -> section.fields(Arrays.asList(
                    markdownText(MessageFormat.format("*Class* {0}", className)),
                    markdownText(MessageFormat.format("*Method* {0}", methodName)),
                    markdownText(MessageFormat.format("*Line* {0}", lineNumber))
                )))
            )
            .filter(Objects::nonNull)
            .toArray(LayoutBlock[]::new)
    );
  }
}