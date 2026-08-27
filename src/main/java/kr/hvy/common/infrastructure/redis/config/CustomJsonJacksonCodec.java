package kr.hvy.common.infrastructure.redis.config;

import org.redisson.codec.JsonJackson3Codec;
import tools.jackson.databind.ObjectMapper;

/**
 * Redisson Jackson 3 코덱 — 전달받은 매퍼를 Redisson 이 rebuild 하여 @class 타이핑 등을 재적용한다
 */
public class CustomJsonJacksonCodec extends JsonJackson3Codec {

  public CustomJsonJacksonCodec(ObjectMapper objectMapper) {
    super(objectMapper);
  }
}
