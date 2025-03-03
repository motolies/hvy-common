package kr.hvy.common.aop.log.model;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.sql.Timestamp;
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
@Table(name = "system_log", indexes = {
    @Index(name = "idx_createdAt", columnList = "createdAt"),
    @Index(name = "idx_requestUri", columnList = "requestUri")
})
public class SystemLogEntity {

  @Id
  @Tsid
  private Long id;

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

  @Column(length = 64)
  private String remoteAddr;

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