package kr.hvy.common.infrastructure.notification.slack.message;

import static com.slack.api.model.block.Blocks.header;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import com.slack.api.model.Attachment;
import com.slack.api.model.block.LayoutBlock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorMessage implements SlackMessage {

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final int MAX_HEADER_LENGTH = 150;

  private final String channel;
  @Builder.Default
  private final boolean notify = false;
  private final Exception exception;
  @Builder.Default
  private final String environment = "";

  @Override
  public boolean isNotify() {
    return notify;
  }

  @Override
  public String getFallbackText() {
    return exception.getMessage();
  }

  @Override
  public List<LayoutBlock> toBlocks() {
    StackTraceElement ste = exception.getStackTrace()[0];
    String packageName = ste.getClassName();
    String className = packageName.substring(packageName.lastIndexOf(".") + 1);
    String methodName = ste.getMethodName();
    int lineNumber = ste.getLineNumber();

    String headerText = String.format("❌ [%s] %s.%s 실패",
        exception.getClass().getSimpleName(), className, methodName);

    List<LayoutBlock> blocks = new ArrayList<>();
    if (notify) {
      blocks.add(section(s -> s.text(markdownText("<!channel>"))));
    }
    blocks.add(header(h -> h.text(plainText(truncate(headerText, MAX_HEADER_LENGTH)))));

    String contextText = String.format("*환경* %s  |  *발생시간* %s",
        environment.isEmpty() ? "-" : environment,
        LocalDateTime.now().format(TIME_FORMAT));
    blocks.add(section(s -> s.text(markdownText(contextText))));

    blocks.add(section(s -> s.fields(Arrays.asList(
        markdownText(String.format("*Package* %s", packageName)),
        markdownText(String.format("*Class* %s", className)),
        markdownText(String.format("*Method* %s", methodName)),
        markdownText(String.format("*Line* %d", lineNumber))
    ))));

    return blocks;
  }

  @Override
  public List<Attachment> toAttachments() {
    Attachment attachment = Attachment.builder()
        .fallback(exception.getMessage())
        .color(SlackColor.ERROR)
        .text(Arrays.toString(exception.getStackTrace()))
        .build();
    return Collections.singletonList(attachment);
  }

  private static String truncate(String text, int maxLength) {
    return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
  }
}
