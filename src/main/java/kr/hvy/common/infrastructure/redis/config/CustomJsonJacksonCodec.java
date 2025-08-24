package kr.hvy.common.infrastructure.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.codec.JsonJacksonCodec;

public class CustomJsonJacksonCodec extends JsonJacksonCodec {

  public CustomJsonJacksonCodec(ObjectMapper objectMapper) {
    super(objectMapper);
  }
}