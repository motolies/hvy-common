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
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
@Builder
public class HotDealMessage implements SlackMessage {

  private static final int MAX_HEADER_LENGTH = 150;
  private static final int MAX_KEYWORD_DISPLAY = 5;
  private static final String CHANNEL_MENTION = "<!channel>";

  private final String channel;
  @Builder.Default
  private final boolean notify = false;

  /**
   * 알림을 유발한 등록 키워드 원문 목록. notify=true일 때 멘션 블록에 함께 표기된다.
   */
  @Builder.Default
  private final List<String> matchedKeywords = Collections.emptyList();

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
    // 모바일 푸시 미리보기에서 키워드 알림임을 구분할 수 있게 접두어를 붙인다.
    // 멘션 문자열은 넣지 않는다 (blocks의 멘션과 중복 해석될 여지가 있음).
    return notify
        ? String.format("[키워드] [%s] %s", siteName, title)
        : String.format("[%s] %s", siteName, title);
  }

  @Override
  public List<LayoutBlock> toBlocks() {
    List<LayoutBlock> blocks = new ArrayList<>();

    // header 블록은 plain_text라 멘션이 렌더링되지 않는다.
    // <!channel>이 실제 멘션으로 동작하려면 반드시 mrkdwn section이어야 한다.
    if (notify) {
      final String mentionText = buildMentionText();
      blocks.add(section(s -> s.text(markdownText(mentionText))));
    }

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

  /**
   * @channel 멘션과 매칭 키워드 표기 텍스트를 만든다.
   * 키워드가 없으면 멘션만 출력한다 (notify=true인데 키워드가 비어 있는 호출부 대비).
   */
  private String buildMentionText() {
    List<String> valid = CollectionUtils.emptyIfNull(matchedKeywords).stream()
        .filter(StringUtils::isNotBlank)
        .toList();

    if (valid.isEmpty()) {
      return CHANNEL_MENTION;
    }

    String joined = valid.stream()
        .limit(MAX_KEYWORD_DISPLAY)
        .map(HotDealMessage::escapeMrkdwn)
        .map(k -> "`" + k + "`")
        .collect(Collectors.joining(", "));

    String suffix = valid.size() > MAX_KEYWORD_DISPLAY
        ? String.format(" 외 %d건", valid.size() - MAX_KEYWORD_DISPLAY)
        : "";

    return String.format("%s 🔔 *등록 키워드 알림*: %s%s", CHANNEL_MENTION, joined, suffix);
  }

  /**
   * Slack mrkdwn 특수문자를 이스케이프한다.
   * 키워드는 사용자 입력이므로 그대로 넣으면 {@code <http://evil|링크>}나 {@code <!here>} 같은
   * 링크·멘션 인젝션이 가능하다. 백틱은 코드 포맷을 깨뜨리므로 제거한다.
   */
  private static String escapeMrkdwn(String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("`", "");
  }

  private static String truncate(String text, int maxLength) {
    return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
  }
}
