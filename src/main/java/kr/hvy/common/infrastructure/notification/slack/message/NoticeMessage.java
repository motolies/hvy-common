package kr.hvy.common.infrastructure.notification.slack.message;

import static com.slack.api.model.block.Blocks.header;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import com.slack.api.model.Attachment;
import com.slack.api.model.block.LayoutBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NoticeMessage implements SlackMessage {

  private final String channel;
  @Builder.Default
  private final boolean notify = false;
  private final String title;
  private final String content;

  @Override
  public boolean isNotify() {
    return notify;
  }

  @Override
  public String getFallbackText() {
    return title;
  }

  @Override
  public List<LayoutBlock> toBlocks() {
    List<LayoutBlock> blocks = new ArrayList<>();
    if (notify) {
      blocks.add(section(s -> s.text(markdownText("<!channel>"))));
    }
    blocks.add(header(h -> h.text(plainText(String.format("⭐ %s ⭐", title)))));
    blocks.add(section(s -> s.text(markdownText(content))));
    return blocks;
  }

  @Override
  public List<Attachment> toAttachments() {
    Attachment attachment = Attachment.builder()
        .color(SlackColor.NOTICE)
        .fallback(title)
        .build();
    return Collections.singletonList(attachment);
  }
}
