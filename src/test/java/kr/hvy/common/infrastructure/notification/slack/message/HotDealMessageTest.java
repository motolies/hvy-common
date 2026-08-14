package kr.hvy.common.infrastructure.notification.slack.message;

import static org.assertj.core.api.Assertions.assertThat;

import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HotDealMessageTest {

  private static HotDealMessage.HotDealMessageBuilder baseMessage() {
    return HotDealMessage.builder()
        .channel("#hvy-hotdeal")
        .siteName("뽐뿌")
        .title("닌텐도 스위치 OLED 특가")
        .url("https://www.ppomppu.co.kr/view.php?id=12345")
        .recommendationCount(12)
        .unrecommendationCount(0)
        .viewCount(3021)
        .commentCount(15);
  }

  @Nested
  @DisplayName("하위호환")
  class BackwardCompatibility {

    @Test
    @DisplayName("notify를 지정하지 않으면 멘션 블록이 없다")
    void toBlocks_withoutNotify_hasNoMentionBlock() {
      // Given
      HotDealMessage message = baseMessage().build();

      // When
      List<LayoutBlock> blocks = message.toBlocks();

      // Then - 첫 블록이 곧바로 header여야 한다
      assertThat(blocks.get(0)).isInstanceOf(HeaderBlock.class);
    }

    @Test
    @DisplayName("matchedKeywords를 지정하지 않아도 NPE가 발생하지 않는다")
    void toBlocks_withoutMatchedKeywords_doesNotThrow() {
      // Given
      HotDealMessage message = baseMessage().notify(true).build();

      // When
      List<LayoutBlock> blocks = message.toBlocks();

      // Then
      assertThat(blocks).isNotEmpty();
      assertThat(message.getMatchedKeywords()).isEmpty();
    }
  }

  @Nested
  @DisplayName("@channel 멘션")
  class ChannelMention {

    @Test
    @DisplayName("notify=true면 첫 블록이 section이고 <!channel>을 포함한다")
    void toBlocks_notifyTrue_firstBlockContainsChannelMention() {
      // Given
      HotDealMessage message = baseMessage()
          .notify(true)
          .matchedKeywords(List.of("닌텐도"))
          .build();

      // When
      LayoutBlock first = message.toBlocks().get(0);

      // Then
      assertThat(first).isInstanceOf(SectionBlock.class);
      assertThat(((SectionBlock) first).getText().getText()).contains("<!channel>");
    }

    @Test
    @DisplayName("멘션 블록의 텍스트 타입이 mrkdwn이다")
    void toBlocks_mentionBlock_usesMrkdwn() {
      // Given - plain_text면 <!channel>이 멘션으로 렌더링되지 않고 문자열로 표시된다
      HotDealMessage message = baseMessage()
          .notify(true)
          .matchedKeywords(List.of("닌텐도"))
          .build();

      // When
      SectionBlock mention = (SectionBlock) message.toBlocks().get(0);

      // Then
      assertThat(mention.getText().getType()).isEqualTo("mrkdwn");
    }

    @Test
    @DisplayName("notify=true인데 matchedKeywords가 비면 멘션만 출력한다")
    void toBlocks_notifyWithoutKeywords_outputsMentionOnly() {
      // Given
      HotDealMessage message = baseMessage().notify(true).build();

      // When
      SectionBlock mention = (SectionBlock) message.toBlocks().get(0);

      // Then
      assertThat(mention.getText().getText()).isEqualTo("<!channel>");
    }
  }

  @Nested
  @DisplayName("키워드 표기")
  class KeywordDisplay {

    @Test
    @DisplayName("매칭 키워드가 백틱 코드로 표기된다")
    void toBlocks_matchedKeywords_renderedAsCode() {
      // Given
      HotDealMessage message = baseMessage()
          .notify(true)
          .matchedKeywords(List.of("닌텐도", "스위치"))
          .build();

      // When
      String text = ((SectionBlock) message.toBlocks().get(0)).getText().getText();

      // Then
      assertThat(text).contains("`닌텐도`").contains("`스위치`");
    }

    @Test
    @DisplayName("키워드가 5개를 넘으면 외 N건으로 축약된다")
    void toBlocks_manyKeywords_truncatesWithCount() {
      // Given
      List<String> many = IntStream.rangeClosed(1, 8).mapToObj(i -> "키워드" + i).toList();
      HotDealMessage message = baseMessage().notify(true).matchedKeywords(many).build();

      // When
      String text = ((SectionBlock) message.toBlocks().get(0)).getText().getText();

      // Then
      assertThat(text).contains("외 3건");
      assertThat(text).doesNotContain("`키워드6`");
    }

    @Test
    @DisplayName("공백 키워드는 표기에서 제외된다")
    void toBlocks_blankKeyword_isFiltered() {
      // Given
      HotDealMessage message = baseMessage()
          .notify(true)
          .matchedKeywords(java.util.Arrays.asList("닌텐도", "  ", null))
          .build();

      // When
      String text = ((SectionBlock) message.toBlocks().get(0)).getText().getText();

      // Then
      assertThat(text).contains("`닌텐도`");
    }
  }

  @Nested
  @DisplayName("mrkdwn 이스케이프")
  class MrkdwnEscaping {

    @Test
    @DisplayName("키워드의 꺾쇠괄호가 이스케이프되어 링크 인젝션을 막는다")
    void toBlocks_keywordWithAngleBrackets_isEscaped() {
      // Given - 이스케이프하지 않으면 슬랙이 링크나 추가 멘션으로 해석한다
      HotDealMessage message = baseMessage()
          .notify(true)
          .matchedKeywords(List.of("<https://evil.example|링크>"))
          .build();

      // When
      String text = ((SectionBlock) message.toBlocks().get(0)).getText().getText();

      // Then - 우리가 의도한 <!channel> 외에는 살아있는 꺾쇠가 없어야 한다
      assertThat(text.replace("<!channel>", "")).doesNotContain("<").doesNotContain(">");
      assertThat(text).contains("&lt;").contains("&gt;");
    }

    @Test
    @DisplayName("키워드의 백틱이 제거되어 코드 포맷이 깨지지 않는다")
    void toBlocks_keywordWithBacktick_removesBacktick() {
      // Given
      HotDealMessage message = baseMessage()
          .notify(true)
          .matchedKeywords(List.of("닌`텐`도"))
          .build();

      // When
      String text = ((SectionBlock) message.toBlocks().get(0)).getText().getText();

      // Then - 감싸는 백틱 2개만 남아야 한다
      assertThat(text.chars().filter(c -> c == '`').count()).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("fallback 텍스트")
  class FallbackText {

    @Test
    @DisplayName("notify=true면 키워드 접두어가 붙는다")
    void getFallbackText_notify_hasKeywordPrefix() {
      // Given
      HotDealMessage message = baseMessage().notify(true).build();

      // When / Then
      assertThat(message.getFallbackText()).startsWith("[키워드]");
    }

    @Test
    @DisplayName("fallback 텍스트에는 멘션 문자열이 들어가지 않는다")
    void getFallbackText_doesNotContainMention() {
      // Given
      HotDealMessage message = baseMessage().notify(true).build();

      // When / Then
      assertThat(message.getFallbackText()).doesNotContain("<!channel>");
    }

    @Test
    @DisplayName("notify=false면 기존 형식을 유지한다")
    void getFallbackText_withoutNotify_keepsOriginalFormat() {
      // Given
      HotDealMessage message = baseMessage().build();

      // When / Then
      assertThat(message.getFallbackText()).isEqualTo("[뽐뿌] 닌텐도 스위치 OLED 특가");
    }
  }

  @Nested
  @DisplayName("기존 동작 유지")
  class ExistingBehavior {

    @Test
    @DisplayName("멘션 블록이 추가돼도 header와 본문 블록은 그대로 뒤따른다")
    void toBlocks_withMention_keepsRemainingBlocks() {
      // Given
      HotDealMessage withMention = baseMessage()
          .notify(true).matchedKeywords(List.of("닌텐도")).build();
      HotDealMessage withoutMention = baseMessage().build();

      // When
      List<LayoutBlock> mentioned = withMention.toBlocks();
      List<LayoutBlock> plain = withoutMention.toBlocks();

      // Then - 멘션 블록 1개만 늘어나야 한다
      assertThat(mentioned).hasSize(plain.size() + 1);
      assertThat(mentioned.get(1)).isInstanceOf(HeaderBlock.class);
    }

    @Test
    @DisplayName("가격이 있으면 header에 가격이 포함된다")
    void toBlocks_withPrice_includesPriceInHeader() {
      // Given
      HotDealMessage message = baseMessage().price("398,000원").build();

      // When
      HeaderBlock header = (HeaderBlock) message.toBlocks().get(0);

      // Then
      assertThat(header.getText().getText()).contains("398,000원");
    }

    @Test
    @DisplayName("attachment 색상은 핫딜 색상을 유지한다")
    void toAttachments_keepsHotDealColor() {
      // Given
      HotDealMessage message = baseMessage().notify(true).build();

      // When / Then
      assertThat(message.toAttachments().get(0).getColor()).isEqualTo(SlackColor.HOT_DEAL);
    }
  }
}
