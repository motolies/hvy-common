package kr.hvy.common.core.code;

import kr.hvy.common.core.code.base.EnumCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UseYN implements EnumCode<String> {

  YES("Y", "사용"),
  NO("N", "미사용");

  private final String code;
  private final String desc;

}
