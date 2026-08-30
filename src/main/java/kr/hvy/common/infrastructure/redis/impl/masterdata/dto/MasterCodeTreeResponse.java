package kr.hvy.common.infrastructure.redis.impl.masterdata.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * 마스터코드 트리 응답 DTO (재귀 구조).
 * <p>
 * L2 캐시(Redis) 에 FQCN 과 함께 직렬화되므로 소유자(blog-back) 와 소비 모듈이
 * 반드시 동일 패키지의 이 클래스를 공유해야 한다.
 */
@Value
@Builder
@Jacksonized
public class MasterCodeTreeResponse {

  /**
   * TSID 문자열(Crockford Base32 13자). 0.3.1 에서 {@code Long} 에서 바뀌었다.
   * <p>
   * TSID 는 2^60 이상이라 JS Number(안전 범위 2^53)로는 무손실 표현이 불가능하다.
   * 숫자로 내려보내면 브라우저에서 값이 조용히 뭉개져 서로 다른 노드가 같다고 비교된다.
   */
  String id;
  String code;
  String name;
  String description;
  Map<String, Object> attributes;
  List<Map<String, String>> attributeSchema;
  Integer depth;
  Integer sort;
  Boolean isActive;

  @Builder.Default
  List<MasterCodeTreeResponse> children = new ArrayList<>();
}
