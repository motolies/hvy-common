package kr.hvy.common.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hvy.common.advice.dto.ApiResponse;
import kr.hvy.common.code.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Slf4j
public abstract class ResponseWrapperConfigure implements ResponseBodyAdvice<Object> {

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

    // todo : dto Validation 에러 발생 시, ApiResponse 객체를 생성하여 반환합니다. 확인 필요.

    // String 타입 반환 시, ApiResponse 객체를 JSON 문자열로 변환하여 반환합니다.
    if (body instanceof String) {
      try {
        return objectMapper.writeValueAsString(ApiResponse.builder()
            .status(ResponseStatus.SUCCESS)
            .path(path)
            .data(body)
            .build());
      } catch (JsonProcessingException e) {
        throw new RuntimeException("JSON 변환 중 오류 발생", e);
      }
    }

    // DTO, List<T> 등 그 외의 타입은 그대로 ApiResponse에 담아 반환합니다.
    return ApiResponse.builder()
        .status(ResponseStatus.SUCCESS)
        .path(path)
        .data(body)
        .build();
  }


  @ExceptionHandler(Exception.class)
  public ApiResponse handleException(Exception ex) {

    // todo : slack 또는 email로 예외 발생 알림을 전송합니다.

    log.error("Exception : ", ex);
    return ApiResponse.builder()
        .status(ResponseStatus.FAIL)
        .message("서버 오류가 발생하였습니다.")
        .build();
  }

}