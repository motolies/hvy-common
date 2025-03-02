package kr.hvy.common.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import kr.hvy.common.advice.dto.ApiResponse;
import kr.hvy.common.advice.dto.FieldValidation;
import kr.hvy.common.code.ApiResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
public abstract class ResponseWrapperConfigure extends ResponseEntityExceptionHandler implements ResponseBodyAdvice<Object> {

  private final ObjectMapper objectMapper;

  public ResponseWrapperConfigure(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    // MappingJackson2HttpMessageConverter를 사용하는 경우에만 적용합니다.
    return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
  }

  @Override
  public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
      ServerHttpResponse response) {
    String path = request.getURI().getPath();

    if (body instanceof ApiResponse) {
      return ((ApiResponse<?>) body).withPath(path);
    }

    // String 타입 반환 시, ApiResponse 객체를 JSON 문자열로 변환하여 반환합니다.
    if (body instanceof String) {
      try {
        return objectMapper.writeValueAsString(ApiResponse.builder()
            .status(ApiResponseStatus.SUCCESS)
            .path(path)
            .data(body)
            .build());
      } catch (JsonProcessingException e) {
        throw new RuntimeException("JSON 변환 중 오류 발생", e);
      }
    }

    // DTO, List<T> 등 그 외의 타입은 그대로 ApiResponse에 담아 반환합니다.
    return ApiResponse.builder()
        .status(ApiResponseStatus.SUCCESS)
        .path(path)
        .data(body)
        .build();
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    return handleException(ex, status);
  }

  private ResponseEntity<Object> handleException(Exception ex, HttpStatusCode status) {
    Set<FieldValidation> errors = new HashSet<>();

    switch (ex) {
      case MethodArgumentNotValidException e -> {
        e.getBindingResult().getFieldErrors().forEach(error -> {
          errors.add(FieldValidation.builder()
              .field(error.getField())
              .message(error.getDefaultMessage())
              .receivedValue(error.getRejectedValue())
              .build());
        });
      }
      default -> {
      }
    }

    return ResponseEntity.status(status)
        .body(ApiResponse.builder()
            .status(ApiResponseStatus.FAIL)
            .data(errors)
            .build());
  }


  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public ApiResponse<?> handleException(Exception ex) {

    // todo : slack 또는 email로 예외 발생 알림을 전송합니다.

    log.error("Exception : ", ex);
    return ApiResponse.builder()
        .status(ApiResponseStatus.FAIL)
        .message("서버 오류가 발생하였습니다.")
        .build();
  }

}