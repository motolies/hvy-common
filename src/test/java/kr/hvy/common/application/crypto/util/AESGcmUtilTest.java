package kr.hvy.common.application.crypto.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import kr.hvy.common.application.crypto.DecryptionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AESGcmUtil - AES-256/GCM, 고정 키 1회 유도, 마커 없음")
class AESGcmUtilTest {

  static final String KEY = "unit-test-gcm-key-0123456789-abcdefghijklmnop";
  static final String SALT = "unit-test-kdf-salt";

  private static AESGcmUtil aesGcmUtil;

  /** 키 유도(PBKDF2 600k)는 비용이 크므로 클래스당 1회만 수행한다. */
  @BeforeAll
  static void setUp() {
    aesGcmUtil = new AESGcmUtil(KEY, SALT);
  }

  @Test
  @DisplayName("암호화→복호화 왕복 시 원문이 보존되고, 암호문은 마커 없는 URL-safe Base64 다")
  void encryptDecryptRoundTrip() {
    String plain = "홍길동 010-1234-5678 서울시 강남구 테헤란로 123";

    String encrypted = aesGcmUtil.encrypt(plain);

    assertThat(encrypted).matches("[A-Za-z0-9_-]+");
    assertThat(aesGcmUtil.decrypt(encrypted)).isEqualTo(plain);
  }

  @Test
  @DisplayName("이모지·장문도 왕복이 성립한다")
  void roundTripEmojiAndLongText() {
    String emoji = "받는분 😀🏠 101동 202호";
    String longText = "가".repeat(2000);

    assertThat(aesGcmUtil.decrypt(aesGcmUtil.encrypt(emoji))).isEqualTo(emoji);
    assertThat(aesGcmUtil.decrypt(aesGcmUtil.encrypt(longText))).isEqualTo(longText);
  }

  @Test
  @DisplayName("빈값/null 은 암복호화 시 그대로 통과한다")
  void blankPassThrough() {
    assertThat(aesGcmUtil.encrypt(null)).isNull();
    assertThat(aesGcmUtil.encrypt("")).isEqualTo("");
    assertThat(aesGcmUtil.decrypt(null)).isNull();
    assertThat(aesGcmUtil.decrypt("")).isEqualTo("");
  }

  @Test
  @DisplayName("동일 입력도 매번 다른 암호문이 된다(random IV)")
  void nonDeterministic() {
    String plain = "홍길동";

    assertThat(aesGcmUtil.encrypt(plain)).isNotEqualTo(aesGcmUtil.encrypt(plain));
  }

  @ParameterizedTest(name = "평문 거부: [{0}]")
  @ValueSource(strings = {
      "홍길동",                                        // Base64 아님
      "01012345678",                                   // Base64 로는 읽히지만 길이 부족
      "abcdefghijklmnopqrstuvwxyzabcdefghijklmnop"     // Base64 로 읽히고 길이도 충분하지만 태그 불일치
  })
  @DisplayName("마커가 없으므로 평문을 복호화하면 원문을 돌려주지 않고 예외로 실패한다")
  void decryptPlaintextThrows(String plain) {
    assertThatThrownBy(() -> aesGcmUtil.decrypt(plain)).isInstanceOf(DecryptionException.class);
  }

  @Test
  @DisplayName("암호문이 변조되면 복호화가 예외로 실패한다(GCM 인증 태그)")
  void tamperDetection() {
    String encrypted = aesGcmUtil.encrypt("010-1234-5678");
    byte[] payload = Base64.getUrlDecoder().decode(encrypted);
    payload[payload.length - 1] ^= 0x01; // 태그 마지막 바이트 변조
    String tampered = Base64.getUrlEncoder().withoutPadding().encodeToString(payload);

    assertThatThrownBy(() -> aesGcmUtil.decrypt(tampered)).isInstanceOf(DecryptionException.class);
  }

  @Test
  @DisplayName("길이가 부족하거나 Base64 가 아니면 예외로 실패한다")
  void malformedCipherTextThrows() {
    String tooShort = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[10]);

    assertThatThrownBy(() -> aesGcmUtil.decrypt(tooShort)).isInstanceOf(DecryptionException.class);
    assertThatThrownBy(() -> aesGcmUtil.decrypt("@@@")).isInstanceOf(DecryptionException.class);
  }

  @Test
  @DisplayName("다른 키 또는 다른 salt 로 만든 암호문은 복호화가 예외로 실패한다")
  void wrongKeyOrSaltThrows() {
    String byOtherKey = new AESGcmUtil("another-unit-test-key-9876543210-zyxwvutsrq", SALT).encrypt("홍길동");
    String byOtherSalt = new AESGcmUtil(KEY, "another-kdf-salt").encrypt("홍길동");

    assertThatThrownBy(() -> aesGcmUtil.decrypt(byOtherKey)).isInstanceOf(DecryptionException.class);
    assertThatThrownBy(() -> aesGcmUtil.decrypt(byOtherSalt)).isInstanceOf(DecryptionException.class);
  }

  @ParameterizedTest(name = "잘못된 설정 거부: key=[{0}] salt=[{1}]")
  @MethodSource("invalidSettings")
  @DisplayName("빈 키·32자 미만 키·빈 salt 는 생성 시 거부한다")
  void constructorRejectsInvalidSettings(String secret, String salt) {
    assertThatThrownBy(() -> new AESGcmUtil(secret, salt)).isInstanceOf(IllegalStateException.class);
  }

  static Stream<Arguments> invalidSettings() {
    return Stream.of(
        Arguments.of(null, SALT),
        Arguments.of("", SALT),
        Arguments.of("   ", SALT),
        Arguments.of("a".repeat(AESGcmUtil.MIN_KEY_LENGTH - 1), SALT),
        Arguments.of(KEY, null),
        Arguments.of(KEY, ""),
        Arguments.of(KEY, "   "));
  }

  @Test
  @DisplayName("다른 구현(블로그 /util/crypto JS 도구)과 같은 벡터를 복호화한다 — 알고리즘 바이트 호환")
  void decryptsCrossImplementationVector() {
    // 블로그 JS 테스트(JAVA_GCM_VECTOR)와 같은 더미 키/salt. 블로그 출력의 접두사(enc:v1:)를 뗀 payload 다.
    AESGcmUtil vectorUtil = new AESGcmUtil("blog-test-only-secret-key-0123456789", "test-kdf-salt-v1");
    String cipherText = "2ra9JHLNDOmfRk_NEcTy01X-ia2p9u1SgYd2FfHuHDOrjL-5VWfteVMcGys7FzLjRmi4y7bDaH_uhbBxOpB3WpVEto09WAzmwO_nJA";

    assertThat(vectorUtil.decrypt(cipherText)).isEqualTo("홍길동 010-1234-5678 서울시 강남구 😀");
  }

  @Test
  @DisplayName("암호문 길이는 공식 ceil((utf8len+28)×4/3) 과 정확히 일치한다")
  void ciphertextLengthMatchesFormula() {
    for (String plain : List.of("a", "홍길동", "010-1234-5678", "서울시 강남구 테헤란로 123 101동 202호", "x".repeat(1000))) {
      int utf8Length = plain.getBytes(StandardCharsets.UTF_8).length;

      assertThat(aesGcmUtil.encrypt(plain)).hasSize(AESGcmUtil.cipherTextLength(utf8Length));
    }
  }

  @ParameterizedTest(name = "한글 {0}자 → 암호문 {1}자")
  @CsvSource({"10, 78", "20, 118", "60, 278", "200, 838", "400, 1638"})
  @DisplayName("컬럼 길이 산정 예시: 한글(3바이트) n자 입력의 암호문 길이")
  void koreanWorstCaseLengths(int koreanChars, int expectedLength) {
    String plain = "가".repeat(koreanChars);

    assertThat(aesGcmUtil.encrypt(plain)).hasSize(expectedLength);
    assertThat(AESGcmUtil.cipherTextLength(koreanChars * 3)).isEqualTo(expectedLength);
  }

  @Test
  @DisplayName("1,200건 복호화가 2초 이내(기대 ~10ms). 호출마다 키를 유도하는 회귀를 잡는다")
  void decryptPerformance() {
    List<String> encrypted = new ArrayList<>();
    for (int i = 0; i < 1200; i++) {
      encrypted.add(aesGcmUtil.encrypt("서울시 강남구 테헤란로 " + i + " 101동 202호"));
    }

    long start = System.nanoTime();
    for (String value : encrypted) {
      aesGcmUtil.decrypt(value);
    }
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;

    assertThat(elapsedMs).isLessThan(2000);
  }
}
