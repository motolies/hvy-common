package kr.hvy.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
public enum ResponseStatus implements CommonEnumCode<String> {

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
