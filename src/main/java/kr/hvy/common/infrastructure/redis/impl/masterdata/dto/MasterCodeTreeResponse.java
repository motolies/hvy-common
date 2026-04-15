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

  Long id;
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
