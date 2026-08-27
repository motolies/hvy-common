package kr.hvy.common.config.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.hvy.common.infrastructure.redis.impl.masterdata.dto.MasterCodeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 3 전환 후 ObjectMapperConfigurer 의 세 매퍼가 기존(Jackson 2) 동작을 유지하는지 검증한다.
 * 특히 L2 캐시 매퍼는 EVERYTHING 타이핑을 직접 재현했으므로 final(@Value) DTO 복원이 회귀 대상이다.
 */
class ObjectMapperConfigurerTest {

  /**
   * 실제 L2 캐시에 저장되는 @Value @Builder @Jacksonized DTO 표본 — 컬렉션은 역직렬화 가능한 구현체(HashMap)를 쓴다
   */
  private static MasterCodeResponse sample() {
    return MasterCodeResponse.builder()
        .id(1L)
        .code("FAVORITE")
        .name("즐겨찾기")
        .attributes(new HashMap<>(Map.of("color", "red")))
        .depth(0)
        .sort(1)
        .isActive(true)
        .createdAt(Instant.parse("2026-08-27T00:00:00Z"))
        .build();
  }

  @Test
  @DisplayName("일반 매퍼: null 필드 제외 · ISO-8601 날짜 문자열 · 들여쓰기")
  void objectMapper_appliesCommonOptions() {
    JsonMapper mapper = ObjectMapperConfigurer.getObjectMapper();

    String json = mapper.writeValueAsString(sample());

    assertThat(json)
        .containsPattern("\"createdAt\"\\s*:\\s*\"2026-08-27T00:00:00Z\"")
        .doesNotContain("description")
        .contains("\n");
  }

  @Test
  @DisplayName("일반 매퍼: @Jacksonized DTO 를 Jackson 3 빌더로 역직렬화한다 (lombok.config jacksonVersion=3)")
  void objectMapper_deserializesJacksonizedBuilder() {
    JsonMapper mapper = ObjectMapperConfigurer.getObjectMapper();

    MasterCodeResponse restored = mapper.readValue(mapper.writeValueAsString(sample()), MasterCodeResponse.class);

    assertThat(restored).isEqualTo(sample());
  }

  @Test
  @DisplayName("L2 매퍼: final(@Value) DTO 에도 @class 가 심겨 Object.class 로 원래 타입이 복원된다")
  void l2ObjectMapper_restoresFinalDtoFromObjectClass() {
    JsonMapper mapper = ObjectMapperConfigurer.getL2ObjectMapper();

    String json = mapper.writeValueAsString(sample());
    Object restored = mapper.readValue(json, Object.class);

    assertThat(json).contains("\"@class\"").contains(MasterCodeResponse.class.getName());
    assertThat(restored).isInstanceOf(MasterCodeResponse.class).isEqualTo(sample());
  }

  @Test
  @DisplayName("L2 매퍼: 컬렉션 원소도 LinkedHashMap 이 아닌 원래 타입으로 복원된다")
  void l2ObjectMapper_restoresCollectionElements() {
    JsonMapper mapper = ObjectMapperConfigurer.getL2ObjectMapper();

    Object restored = mapper.readValue(mapper.writeValueAsString(new ArrayList<>(List.of(sample()))), Object.class);

    assertThat(restored).isInstanceOf(List.class);
    assertThat(((List<?>) restored).get(0)).isInstanceOf(MasterCodeResponse.class).isEqualTo(sample());
  }

  @Test
  @DisplayName("Redis 매퍼: NON_FINAL 타입에는 @class 를 심고 final DTO 루트에는 심지 않는다")
  void redisObjectMapper_usesNonFinalTyping() {
    JsonMapper mapper = ObjectMapperConfigurer.getRedisObjectMapper();

    assertThat(mapper.writeValueAsString(new ArrayList<>(List.of("a")))).contains("java.util.ArrayList");
    assertThat(mapper.writeValueAsString(sample())).doesNotContain(MasterCodeResponse.class.getName());
  }
}
