package kr.hvy.common.infrastructure.notification.slack.message;

import static com.slack.api.model.block.Blocks.header;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import com.slack.api.model.Attachment;
import com.slack.api.model.block.LayoutBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Builder
public class DeployMessage implements SlackMessage {

  private final String channel;
  @Builder.Default
  private final boolean notify = false;
  private final String description;
  private final String version;
  private final String commitMessage;
  private final String deployer;
  private final String serviceUrl;

  @Override
  public boolean isNotify() {
    return notify;
  }

  @Override
  public String getFallbackText() {
    return String.format("배포 완료: %s (%s)", description, version);
  }

  @Override
  public List<LayoutBlock> toBlocks() {
    List<LayoutBlock> blocks = new ArrayList<>();
    if (notify) {
      blocks.add(section(s -> s.text(markdownText("<!channel>"))));
    }
    blocks.add(header(h -> h.text(plainText(String.format("✅ %s (%s)", description, version)))));

    blocks.add(section(s -> s.fields(Arrays.asList(
        markdownText(String.format("*Commit*\n%s", StringUtils.defaultIfBlank(commitMessage, "-"))),
        markdownText(String.format("*배포자*\n%s", StringUtils.defaultIfBlank(deployer, "-")))
    ))));

    if (StringUtils.isNotBlank(serviceUrl)) {
      blocks.add(section(s -> s.text(markdownText(String.format("*서비스*: <%s|바로가기>", serviceUrl)))));
    }

    return blocks;
  }

  @Override
  public List<Attachment> toAttachments() {
    Attachment attachment = Attachment.builder()
        .color(SlackColor.DEPLOY)
        .fallback(getFallbackText())
        .build();
    return Collections.singletonList(attachment);
  }
}
