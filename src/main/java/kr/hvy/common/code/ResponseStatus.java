package kr.hvy.common.code;

import kr.hvy.common.code.base.EnumCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseStatus implements EnumCode<String> {

  SUCCESS("SUCCESS", "성공"),
  FAIL("FAIL", "실패");

  private final String code;
  private final String desc;

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public String getDesc() {
    return desc;
  }
}
