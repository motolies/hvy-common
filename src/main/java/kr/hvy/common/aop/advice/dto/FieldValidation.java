package kr.hvy.common.aop.advice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldValidation {

  private String field;
  private String message;
  private Object receivedValue;
}
