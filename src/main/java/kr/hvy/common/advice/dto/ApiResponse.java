package kr.hvy.common.advice.dto;

import java.time.LocalDateTime;
import kr.hvy.common.code.ApiResponseStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.With;

@Getter
@Builder
@With
public class ApiResponse<T> {

  private final LocalDateTime timestamp = LocalDateTime.now();
  private String path;
  @NonNull
  private ApiResponseStatus status;
  private String message;
  private T data;
}
