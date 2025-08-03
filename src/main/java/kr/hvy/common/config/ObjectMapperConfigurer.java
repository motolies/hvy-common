package kr.hvy.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
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
        .featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS) // 빈 객체 직렬화 시 실패하지 않도록 설정
        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // 날짜를 타임스탬프가 아닌 문자열로 직렬화
        .featuresToEnable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY) // 단일 값을 배열로 역직렬화 허용
        .featuresToEnable(SerializationFeature.INDENT_OUTPUT) // JSON 출력 시 들여쓰기 적용
        .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 알 수 없는 속성이 있어도 역직렬화 실패하지 않음
        // .featuresToEnable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL) // 알 수 없는 enum 값을 null로 처리
        .featuresToEnable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT) // 빈 문자열을 null 객체로 처리
        .serializationInclusion(JsonInclude.Include.NON_NULL) // null 값인 필드는 직렬화에서 제외
        .modules(new JavaTimeModule(), new Jdk8Module()); // Java 8 시간 API와 Optional 등 지원 모듈 추가
  }
}
