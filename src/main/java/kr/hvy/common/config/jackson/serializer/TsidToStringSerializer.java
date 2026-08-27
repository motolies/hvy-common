package kr.hvy.common.config.jackson.serializer;

import kr.hvy.common.core.converter.TsidUtils;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * TSID(long) 를 Crockford Base32 문자열로 직렬화한다 (Jackson 3: JsonSerializer → ValueSerializer, IOException 없음)
 */
public class TsidToStringSerializer extends ValueSerializer<Long> {

  @Override
  public void serialize(Long value, JsonGenerator gen, SerializationContext ctxt) {
    if (value == null) {
      gen.writeNull();
    } else {
      gen.writeString(TsidUtils.getTsid(value).toString());
    }
  }
}
