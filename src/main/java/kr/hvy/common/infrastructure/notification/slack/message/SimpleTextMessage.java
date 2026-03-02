package kr.hvy.common.infrastructure.notification.slack.message;

import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;

import com.slack.api.model.block.LayoutBlock;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SimpleTextMessage implements SlackMessage {

  private final String channel;
  private final boolean notify;
  private final String message;

  @Override
  public boolean isNotify() {
    return notify;
  }

  @Override
  public String getFallbackText() {
    return message;
  }

  @Override
  public List<LayoutBlock> toBlocks() {
    List<LayoutBlock> blocks = new ArrayList<>();
    if (notify) {
      blocks.add(section(s -> s.text(markdownText("<!channel>"))));
    }
    blocks.add(section(s -> s.text(markdownText(message))));
    return blocks;
  }
}
