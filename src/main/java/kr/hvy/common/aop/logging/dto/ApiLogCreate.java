package kr.hvy.common.aop.logging.dto;

import kr.hvy.common.application.domain.vo.EventLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiLogCreate {

  private String traceId;
  private String spanId;
  private String requestUri;
  private String httpMethodType;
  private String requestHeader;
  private String requestParam;
  private String requestBody;
  private String responseStatus;
  private String responseBody;
  private EventLog created;
  private Long processTime;
}
