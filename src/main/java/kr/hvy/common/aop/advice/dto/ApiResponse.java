package kr.hvy.common.aop.advice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import kr.hvy.common.core.code.ApiResponseStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.With;

@Getter
@Builder
@With
public class ApiResponse<T> {

  private final Instant timestamp = Instant.now();
  private String path;
  @NonNull
  private ApiResponseStatus status;
  private String message;
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private T data;
}
