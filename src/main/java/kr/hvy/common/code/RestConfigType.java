package kr.hvy.common.code;

import kr.hvy.common.code.base.EnumCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RestConfigType implements EnumCode<String> {

  NONE("NONE", "NONE"),
  LOGGING("LOGGING", "LOGGING"),
  USER_AGENT("USER_AGENT", "USER_AGENT");

  private final String code;
  private final String desc;

}
