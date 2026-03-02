package kr.hvy.common.infrastructure.notification.slack.message;

import static com.slack.api.model.block.Blocks.context;
import static com.slack.api.model.block.Blocks.header;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import com.slack.api.model.Attachment;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.element.ImageElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Builder
public class HotDealMessage implements SlackMessage {

  private static final int MAX_HEADER_LENGTH = 150;

  private final String channel;
  @Builder.Default
  private final boolean notify = false;

  private final String siteName;
  private final String title;
  private final String price;
  private final String url;
  private final String thumbnailUrl;

  private final int recommendationCount;
  private final int unrecommendationCount;
  private final int viewCount;
  private final int commentCount;
  private final String dealCategory;

  @Override
  public boolean isNotify() {
    return notify;
  }

  @Override
  public String getFallbackText() {
    return String.format("[%s] %s", siteName, title);
  }

  @Override
  public List<LayoutBlock> toBlocks() {
    List<LayoutBlock> blocks = new ArrayList<>();

    String headerText = StringUtils.isNotBlank(price)
        ? String.format("🔥 [%s] %s (%s) 🔥", siteName, title, price)
        : String.format("🔥 [%s] %s 🔥", siteName, title);
    blocks.add(header(h -> h.text(plainText(truncate(headerText, MAX_HEADER_LENGTH)))));

    String sectionText = String.format("*구매링크*: <%s|바로가기>", url);
    if (StringUtils.isNotBlank(dealCategory)) {
      sectionText += String.format("\n*카테고리*: %s", dealCategory);
    }
    final String finalSectionText = sectionText;

    if (StringUtils.isNotBlank(thumbnailUrl)) {
      blocks.add(section(s -> s
          .text(markdownText(finalSectionText))
          .accessory(ImageElement.builder()
              .imageUrl(thumbnailUrl)
              .altText(title)
              .build())));
    } else {
      blocks.add(section(s -> s.text(markdownText(finalSectionText))));
    }

    String contextText = String.format("👍 %d  |  👎 %d  |  👁 %d  |  💬 %d",
        recommendationCount, unrecommendationCount, viewCount, commentCount);
    blocks.add(context(c -> c.elements(List.of(markdownText(contextText)))));

    return blocks;
  }

  @Override
  public List<Attachment> toAttachments() {
    Attachment attachment = Attachment.builder()
        .color(SlackColor.HOT_DEAL)
        .fallback(getFallbackText())
        .build();
    return Collections.singletonList(attachment);
  }

  private static String truncate(String text, int maxLength) {
    return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
  }
}
