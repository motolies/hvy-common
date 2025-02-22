package kr.hvy.common.advice.dto;

import java.time.LocalDateTime;
import kr.hvy.common.code.ResponseStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.With;
import org.springframework.http.HttpStatus;

@Getter
@Builder
@With
public class ApiResponse<T> {

  private final LocalDateTime timestamp = LocalDateTime.now();
  private String path;
  @NonNull
  private ResponseStatus status;
  private String message;
  private T data;
}
