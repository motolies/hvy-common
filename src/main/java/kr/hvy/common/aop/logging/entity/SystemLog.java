package kr.hvy.common.aop.logging.entity;

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
import kr.hvy.common.application.domain.embeddable.EventLogEntity;
import kr.hvy.common.core.code.ApiResponseStatus;
import kr.hvy.common.core.code.converter.ApiResponseStatusConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "system_log", indexes = {
    @Index(name = "idx_system_log_traceId", columnList = "traceId"),
    @Index(name = "idx_system_log_spanId", columnList = "spanId"),
    @Index(name = "idx_system_log_createdAt", columnList = "createdAt"),
    @Index(name = "idx_system_log_requestUri", columnList = "requestUri")
})
public class SystemLog {

  @Id
  @Tsid
  private Long id;

  @Column(length = 32)
  private String traceId;

  @Column(length = 16)
  private String spanId;

  @Column(length = 1024)
  private String requestUri;

  @Column(length = 512)
  private String controllerName;

  @Column(length = 512)
  private String methodName;

  @Column(length = 8)
  private String httpMethodType;

  @Column(columnDefinition = "TEXT")
  private String paramData;

  @Column(columnDefinition = "TEXT")
  private String responseBody;

  @Column(columnDefinition = "TEXT")
  private String stackTrace;

  @Column(length = 64)
  private String remoteAddr;

  @Column
  private Long processTime;

  @Convert(converter = ApiResponseStatusConverter.class)
  @Column(length = 4, nullable = false)
  private ApiResponseStatus status;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "at", column = @Column(name = "createdAt", columnDefinition = "DATETIME(6)", nullable = false)),
      @AttributeOverride(name = "by", column = @Column(name = "createdBy"))
  })
  @Builder.Default
  private EventLogEntity created = EventLogEntity.defaultValues();

}