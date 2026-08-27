package kr.hvy.common.config.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;

/**
 * Jackson 3(tools.jackson) 기반 공용 JsonMapper 팩토리
 * <p>
 * Jackson 3 의 ObjectMapper 는 불변이라 활성화 옵션(default typing 등)은 빌더 단계에서 확정한다.
 * annotation 패키지(com.fasterxml.jackson.annotation)는 Jackson 3 에서도 그대로 유지된다.
 */
public class ObjectMapperConfigurer {

  /**
   * 모든 서브타입을 허용하는 PTV — Jackson 3 에서 LaissezFaireSubTypeValidator 가 비공개(package-private)가 되어
   * 공개 API 인 BasicPolymorphicTypeValidator 로 동등하게 구성한다 (Redisson JsonJackson3Codec 과 같은 방식)
   */
  private static final PolymorphicTypeValidator ALLOW_ALL_SUBTYPES =
      BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build();

  /**
   * HTTP/일반 직렬화용 매퍼
   * Boot 4 는 JsonMapper 타입 빈만 auto-config 를 대체하므로 JsonMapper 를 반환한다.
   */
  public static JsonMapper getObjectMapper() {
    return builder().build();
  }

  /**
   * Redisson 코덱용 매퍼 — NON_FINAL 타입에 @class 프로퍼티를 심는다 (기존 JsonJacksonCodec 과 동일 와이어 포맷)
   */
  public static JsonMapper getRedisObjectMapper() {
    return builder()
        .activateDefaultTyping(ALLOW_ALL_SUBTYPES, DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
        .build();
  }

  /**
   * L2(Redis) 캐시 직렬화용 매퍼 — Jackson 2 의 EVERYTHING default typing 과 동일하게 final 클래스(@Value DTO)까지
   * 타입 정보를 JSON 에 포함시켜 readValue(json, Object.class) 가 LinkedHashMap 이 아닌 원래 타입으로 복원되게 한다.
   */
  public static JsonMapper getL2ObjectMapper() {
    return builder()
        .setDefaultTyping(new EverythingTypeResolverBuilder(ALLOW_ALL_SUBTYPES, JsonTypeInfo.As.PROPERTY))
        .build();
  }

  /**
   * Jackson 2 의 DefaultTyping.EVERYTHING 재현 — Jackson 3 에서 삭제된 모드.
   * <p>
   * NON_FINAL 은 final 클래스(lombok @Value DTO 등)에 타입 정보를 심지 않아 L2 캐시가 LinkedHashMap 으로 풀리므로,
   * primitive 를 제외한 모든 타입에 적용한다. natural 타입(String/Boolean/Integer/Double)은 직렬화기 단계에서
   * 타입 정보를 쓰지 않으므로 별도 제외가 필요 없다(Jackson 2 EVERYTHING 과 동일한 규칙).
   * 생성자는 MapperBuilder.activateDefaultTyping 이 쓰는 것과 같은 3-인자 생성자라 @class 프로퍼티 설정을 그대로 상속한다.
   */
  private static final class EverythingTypeResolverBuilder extends DefaultTypeResolverBuilder {

    private static final long serialVersionUID = 1L;

    private EverythingTypeResolverBuilder(PolymorphicTypeValidator ptv, JsonTypeInfo.As includeAs) {
      super(ptv, DefaultTyping.NON_FINAL, includeAs);
    }

    @Override
    public boolean useForType(JavaType t) {
      return !t.isPrimitive();
    }
  }

  /**
   * 공통 옵션이 적용된 빌더 — JavaTimeModule/Jdk8Module 은 Jackson 3 내장이라 등록하지 않는다
   */
  private static JsonMapper.Builder builder() {
    return JsonMapper.builder()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS) // 빈 객체 직렬화 시 실패하지 않도록 설정
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS) // 날짜를 타임스탬프가 아닌 문자열로 직렬화 (Jackson 3 에서 DateTimeFeature 로 이동)
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY) // 단일 값을 배열로 역직렬화 허용
        .enable(SerializationFeature.INDENT_OUTPUT) // JSON 출력 시 들여쓰기 적용
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 알 수 없는 속성이 있어도 역직렬화 실패하지 않음
        // .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL) // 알 수 없는 enum 값을 null로 처리
        .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT) // 빈 문자열을 null 객체로 처리
        .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL)); // null 값인 필드는 직렬화에서 제외
  }
}
