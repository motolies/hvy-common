package kr.hvy.common.crypto.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class HashUtil {

  /**
   * 주어진 문자열에 대해 SHA3-256 해시 값을 16진수 문자열로 반환합니다.
   *
   * @param input 해시를 생성할 입력 문자열
   * @return SHA3-256 해시값(16진수 문자열)
   */
  public static String SHA3_256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA3-256");
      byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        hexString.append(String.format("%02x", b));
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA3-256 Algorithm cannot be used.", e);
    }
  }

  public static String toSHA256(String... values) {
    if (ObjectUtils.isEmpty(values)) {
      return null;
    }

    String concatValue = Arrays.stream(values)
        .filter(StringUtils::isNotBlank)
        .map(value -> value.replaceAll("\\s", StringUtils.EMPTY))
        .collect(Collectors.joining(""));

    return SHA3_256(concatValue);
  }
}