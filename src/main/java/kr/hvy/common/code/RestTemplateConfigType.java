package kr.hvy.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RestTemplateConfigType implements CommonEnumCode<String> {

  NONE("NONE", "NONE"),
  LOGGING("LOGGING", "LOGGING"),
  USER_AGENT("USER_AGENT", "USER_AGENT");

  private final String code;
  private final String desc;

}
