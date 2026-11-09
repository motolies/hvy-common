package kr.hvy.common.application.crypto.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import kr.hvy.common.application.crypto.DecryptionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AESCbcUtil - AES-256/CBC 레거시, 값마다 키 유도")
class AESCbcUtilTest {

  static final String KEY = "legacy-cbc-test-key";
  static final String PLAIN = "홍길동 010-1234-5678 서울시 강남구 😀";
  /** 리네임 전 원본 AESUtil(setSecretKey(KEY))로 PLAIN 을 암호화한 값. 포맷 호환의 기준점이다. */
  static final String LEGACY_VECTOR =
      "4UCOcCqlnFydzG5GYupIwM-YGZdUkgDq8_ocpapABAix7GQk2pWczqNvquh4EFltpmUnIKc2-nhwHOXnS0T15YiGnXomP9ZHTO51Z6I6JQ09hBrsWpwvdBYhZaNO7ivw";

  private final AESCbcUtil aesCbcUtil = new AESCbcUtil(KEY);

  @Test
  @DisplayName("기존 AESUtil 이 만든 암호문을 복호화한다 — 리네임 후에도 포맷이 같다")
  void decryptsLegacyVector() {
    assertThat(aesCbcUtil.decrypt(LEGACY_VECTOR)).isEqualTo(PLAIN);
  }

  @Test
  @DisplayName("암호화→복호화 왕복 시 원문이 보존되고, 암호문은 마커 없는 URL-safe Base64 다")
  void encryptDecryptRoundTrip() {
    String encrypted = aesCbcUtil.encrypt(PLAIN);

    assertThat(encrypted).matches("[A-Za-z0-9_-]+");
    assertThat(aesCbcUtil.decrypt(encrypted)).isEqualTo(PLAIN);
  }

  @Test
  @DisplayName("빈값/null 은 암복호화 시 그대로 통과한다")
  void blankPassThrough() {
    assertThat(aesCbcUtil.encrypt(null)).isNull();
    assertThat(aesCbcUtil.encrypt("")).isEqualTo("");
    assertThat(aesCbcUtil.decrypt(null)).isNull();
    assertThat(aesCbcUtil.decrypt("")).isEqualTo("");
  }

  @Test
  @DisplayName("동일 입력도 매번 다른 암호문이 된다(random salt·IV)")
  void nonDeterministic() {
    assertThat(aesCbcUtil.encrypt("홍길동")).isNotEqualTo(aesCbcUtil.encrypt("홍길동"));
  }

  @Test
  @DisplayName("다른 키로는 고정 벡터를 복호화하지 못하고 예외로 실패한다")
  void wrongKeyThrows() {
    // 랜덤 암호문 대신 고정 벡터를 쓴다 — CBC 는 무결성 검사가 없어 다른 키라도 1/256 확률로 패딩이 맞을 수 있다.
    AESCbcUtil other = new AESCbcUtil("another-legacy-key");

    assertThatThrownBy(() -> other.decrypt(LEGACY_VECTOR)).isInstanceOf(DecryptionException.class);
  }

  @Test
  @DisplayName("평문·Base64 아님·길이 부족 입력은 예외로 실패한다")
  void malformedInputThrows() {
    String tooShort = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[40]);

    assertThatThrownBy(() -> aesCbcUtil.decrypt("홍길동")).isInstanceOf(DecryptionException.class);
    assertThatThrownBy(() -> aesCbcUtil.decrypt("@@@")).isInstanceOf(DecryptionException.class);
    assertThatThrownBy(() -> aesCbcUtil.decrypt(tooShort)).isInstanceOf(DecryptionException.class);
  }

  @ParameterizedTest(name = "빈 키 거부: [{0}]")
  @NullAndEmptySource
  @ValueSource(strings = "   ")
  @DisplayName("빈 키는 생성 시 거부한다(기존 키 호환을 위해 길이 제한은 두지 않는다)")
  void constructorRejectsBlankKey(String secret) {
    assertThatThrownBy(() -> new AESCbcUtil(secret)).isInstanceOf(IllegalStateException.class);
  }
}
