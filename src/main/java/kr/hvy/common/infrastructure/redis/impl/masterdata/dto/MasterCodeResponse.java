package kr.hvy.common.infrastructure.redis.impl.masterdata.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * 마스터코드 단건 응답 DTO (플랫 구조).
 * <p>
 * hvy-common 의 2단계 캐시(Redis L2) 에 직렬화되므로 소비 모듈에서도 동일 FQCN 으로 참조 가능해야 한다.
 * blog-back 과 외부 소비 앱이 이 클래스를 공유한다.
 */
@Value
@Builder
@Jacksonized
public class MasterCodeResponse {

  Long id;
  String code;
  String name;
  String description;
  Map<String, Object> attributes;
  List<Map<String, String>> attributeSchema;
  Integer depth;
  Long parentId;
  Integer sort;
  Boolean isActive;
  LocalDateTime createdAt;
  String createdBy;
  LocalDateTime updatedAt;
  String updatedBy;
  Integer childCount;
}
