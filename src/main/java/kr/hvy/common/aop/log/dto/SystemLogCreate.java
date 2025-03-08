package kr.hvy.common.aop.log.dto;

import kr.hvy.common.code.ApiResponseStatus;
import kr.hvy.common.domain.vo.EventLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogCreate {

  private String requestUri;
  private String controllerName;
  private String methodName;
  private String httpMethodType;
  private String paramData;
  private String responseBody;
  private String remoteAddr;
  private EventLog created;
  private Long processTime;
  @Builder.Default
  private ApiResponseStatus status = ApiResponseStatus.SUCCESS;
  private String stackTrace;

}
