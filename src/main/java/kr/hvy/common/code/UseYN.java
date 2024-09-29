package kr.hvy.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UseYN implements CommonEnumCode<String>{

    YES("Y", "사용"),
    NO("N", "미사용");

    private final String code;
    private final String desc;

}
