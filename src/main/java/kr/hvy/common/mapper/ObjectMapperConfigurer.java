package kr.hvy.common.mapper;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

public class ObjectMapperConfigurer {

  public static ObjectMapper getObjectMapper() {
    return builder().build();
  }

  public static ObjectMapper getRedisObjectMapper() {
    ObjectMapper objectMapper = builder().build();
    objectMapper.activateDefaultTyping(
        LaissezFaireSubTypeValidator.instance,
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY
    );
    return objectMapper;
  }


  private static Jackson2ObjectMapperBuilder builder() {
    return Jackson2ObjectMapperBuilder
        .json()
        .featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .featuresToEnable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .modules(new JavaTimeModule(), new Jdk8Module());
  }
}
