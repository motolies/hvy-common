package kr.hvy.common.code;

import java.text.MessageFormat;

/**
 * The interface Common enum code.
 *
 * @param <T> the type parameter
 */
public interface CommonEnumCode<T> {
    /**
     * Gets code.
     *
     * @return the code
     */
    T getCode();

    /**
     * Gets desc.
     *
     * @return the desc
     */
    String getDesc();

    /**
     * Convert string string.
     *
     * @return the string
     */
    default String convertString() {
        return MessageFormat.format("{0}({1})", getDesc(), getCode());
    }
}
