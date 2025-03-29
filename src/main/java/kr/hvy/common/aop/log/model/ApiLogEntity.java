package kr.hvy.common.aop.log.model;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kr.hvy.common.code.ApiResponseStatus;
import kr.hvy.common.code.converter.ApiResponseStatusConverter;
import kr.hvy.common.domain.embeddable.EventLogEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "api_log", indexes = {
    @Index(name = "idx_api_log_traceId", columnList = "traceId"),
    @Index(name = "idx_api_log_spanId", columnList = "spanId"),
    @Index(name = "idx_api_log_createdAt", columnList = "createdAt"),
    @Index(name = "idx_api_log_requestUri", columnList = "requestUri")
})
public class ApiLogEntity {

  @Id
  @Tsid
  private Long id;

  @Column(length = 32)
  private String traceId;

  @Column(length = 16)
  private String spanId;

  @Column(length = 1024)
  private String requestUri;

  @Column(length = 8)
  private String httpMethodType;

  @Column(length = 8192)
  private String requestHeader;

  @Column(length = 4096)
  private String requestParam;

  @Column(columnDefinition = "TEXT")
  private String requestBody;

  @Column(length = 128)
  private String responseStatus;

  @Column(columnDefinition = "TEXT")
  private String responseBody;

  @Column
  private Long processTime;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "at", column = @Column(name = "createdAt", columnDefinition = "DATETIME(6)", nullable = false)),
      @AttributeOverride(name = "by", column = @Column(name = "createdBy"))
  })
  @Builder.Default
  private EventLogEntity created = EventLogEntity.defaultValues();

}