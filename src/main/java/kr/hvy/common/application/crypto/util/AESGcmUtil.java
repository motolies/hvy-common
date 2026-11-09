package kr.hvy.common.application.crypto.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import kr.hvy.common.application.crypto.DecryptionException;
import kr.hvy.common.application.crypto.Decryptor;
import kr.hvy.common.application.crypto.EncryptionException;
import kr.hvy.common.application.crypto.Encryptor;
import org.springframework.util.StringUtils;

/**
 * 저장 데이터 암호화 유틸 — AES-256/GCM, 고정 키 1회 유도. 신규 프로젝트 권장.
 *
 * <p>설계:
 * <ul>
 *   <li>키는 기동 시 비밀값과 고정 salt 에서 PBKDF2 로 <b>1회만</b> 유도해 보관한다. 값마다 KDF 를 돌리는
 *       {@link AESCbcUtil}(필드당 수 ms)과 달리 암복호화 비용이 마이크로초 단위다.</li>
 *   <li>값마다 랜덤 12바이트 IV 를 쓰므로 같은 평문도 매번 다른 암호문이 되고(비결정적),
 *       인증 태그로 변조·키 불일치를 검출한다.</li>
 *   <li>암호문 포맷: {@code Base64url(no-pad)(iv ‖ ciphertext ‖ tag)}. 마커가 없으므로 비어 있지 않은 값은
 *       모두 암호문으로 간주하고, 복호화가 실패하면 {@link DecryptionException} 을 던진다(평문 혼재 불가).</li>
 *   <li>salt 나 비밀값을 바꾸면 유도 키가 달라져 기존 암호문을 읽을 수 없다 → 키 교체와 같이 전체 재암호화가 필요하다.</li>
 * </ul>
 *
 * <table border="1">
 *   <caption>CBC / GCM 비교</caption>
 *   <tr><th>항목</th><th>AESCbcUtil</th><th>AESGcmUtil (이 클래스)</th></tr>
 *   <tr><td>키 유도</td><td>값마다 랜덤 salt 16B 로 PBKDF2 8,192회</td><td>고정 salt(입력)로 기동 시 1회 PBKDF2 600,000회</td></tr>
 *   <tr><td>IV</td><td>16B 랜덤</td><td>12B 랜덤</td></tr>
 *   <tr><td>무결성</td><td>없음 — 변조·키 불일치를 검출하지 못하고 드물게 깨진 문자열을 반환할 수 있다</td><td>16B 인증 태그로 변조·키 불일치 검출</td></tr>
 *   <tr><td>암호문 포맷</td><td>Base64url(no-pad)(salt ‖ iv ‖ ciphertext)</td><td>Base64url(no-pad)(iv ‖ ciphertext ‖ tag)</td></tr>
 *   <tr><td>필드당 비용</td><td>수 ms (매번 키 유도) — 대량 조회 시 누적된다</td><td>수 µs</td></tr>
 *   <tr><td>키 검증</td><td>빈값만 거부 (기존 키 호환)</td><td>32자 이상 + salt 필수</td></tr>
 *   <tr><td>마커</td><td colspan="2">둘 다 없음 — 평문과 암호문을 구분할 수 없어 혼재 불가. 한 프로젝트는 둘 중 하나만 사용한다</td></tr>
 * </table>
 *
 * <p>암호문 길이 공식(DDL 산정용): {@code bytes = utf8len + 28}, {@code chars = ceil(bytes × 4 / 3)}.
 * 계산은 {@link #cipherTextLength(int)} 참고.
 */
public class AESGcmUtil implements Encryptor, Decryptor {

  /** 비밀값 최소 길이(문자) */
  public static final int MIN_KEY_LENGTH = 32;

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final int KDF_ITERATIONS = 600_000;
  private static final int KEY_BITS = 256;
  private static final int IV_LENGTH = 12;
  private static final int TAG_LENGTH = 16;
  private static final int TAG_BITS = TAG_LENGTH * 8;

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private final SecretKey key;

  /**
   * 비밀값과 salt 를 검증하고 AES 키를 1회 유도한다. 검증 실패 시 컨텍스트 기동을 막는다(fail-fast).
   *
   * @param secret 비밀값 (32자 이상)
   * @param salt   키 유도용 고정 salt. 시스템마다 다르게 정하고, 정한 뒤에는 바꾸지 않는다
   * @throws IllegalStateException 비밀값·salt 가 유효하지 않을 때
   */
  public AESGcmUtil(String secret, String salt) {
    validate(secret, salt);
    this.key = deriveKey(secret, salt);
  }

  /** 빈 비밀값·짧은 비밀값·빈 salt 를 거부한다. */
  static void validate(String secret, String salt) {
    if (!StringUtils.hasText(secret)) {
      throw new IllegalStateException("암호화 키가 설정되지 않았습니다.");
    }
    if (secret.length() < MIN_KEY_LENGTH) {
      throw new IllegalStateException(
          "암호화 키는 " + MIN_KEY_LENGTH + "자 이상이어야 합니다. (현재 " + secret.length() + "자)");
    }
    if (!StringUtils.hasText(salt)) {
      throw new IllegalStateException("암호화 키 유도용 salt 가 설정되지 않았습니다.");
    }
  }

  /** PBKDF2 로 256비트 AES 키를 유도한다. 생성자에서 1회만 호출된다. */
  private static SecretKey deriveKey(String secret, String salt) {
    try {
      SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
      byte[] keyBytes = factory
          .generateSecret(new PBEKeySpec(secret.toCharArray(), salt.getBytes(StandardCharsets.UTF_8),
              KDF_ITERATIONS, KEY_BITS))
          .getEncoded();
      return new SecretKeySpec(keyBytes, "AES");
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("암호화 키 유도에 실패했습니다.", e);
    }
  }

  /**
   * 평문을 암호화한다. 빈값/null 은 그대로 반환한다.
   *
   * @throws EncryptionException 암호화 실패
   */
  @Override
  public String encrypt(String data) {
    if (!StringUtils.hasText(data)) {
      return data;
    }
    byte[] iv = new byte[IV_LENGTH];
    RANDOM.nextBytes(iv);
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      byte[] cipherText = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

      byte[] payload = new byte[IV_LENGTH + cipherText.length];
      System.arraycopy(iv, 0, payload, 0, IV_LENGTH);
      System.arraycopy(cipherText, 0, payload, IV_LENGTH, cipherText.length);
      return ENCODER.encodeToString(payload);
    } catch (GeneralSecurityException e) {
      throw new EncryptionException("GCM 암호화 실패", e);
    }
  }

  /**
   * 암호문을 복호화한다. 빈값/null 은 그대로 반환한다.
   *
   * @throws DecryptionException Base64 아님·길이 부족·태그 불일치(키 불일치·변조·손상·평문 입력)
   */
  @Override
  public String decrypt(String encryptedData) {
    if (!StringUtils.hasText(encryptedData)) {
      return encryptedData;
    }
    byte[] payload;
    try {
      payload = DECODER.decode(encryptedData);
    } catch (IllegalArgumentException e) {
      throw new DecryptionException("암호문 Base64 디코딩 실패 (length=" + encryptedData.length() + ")", e);
    }
    if (payload.length < IV_LENGTH + TAG_LENGTH) {
      throw new DecryptionException("암호문 길이 부족 (bytes=" + payload.length + ")");
    }
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, payload, 0, IV_LENGTH));
      byte[] plain = cipher.doFinal(payload, IV_LENGTH, payload.length - IV_LENGTH);
      return new String(plain, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException e) {
      // AEADBadTagException 포함: 키 불일치·변조·손상·평문 입력. 입력 문자열을 결과로 흘리지 않고 명시적으로 실패시킨다.
      throw new DecryptionException("GCM 복호화 실패 (bytes=" + payload.length + ")", e);
    }
  }

  /**
   * 평문 UTF-8 바이트 길이에 대한 암호문 문자열 길이. DDL 컬럼 길이 산정과 테스트에서 사용한다.
   * {@code bytes = 12(IV) + utf8len + 16(tag)}, Base64url(no-pad) 문자수 = {@code ceil(bytes × 4 / 3)}.
   */
  public static int cipherTextLength(int utf8Length) {
    int bytes = IV_LENGTH + utf8Length + TAG_LENGTH;
    return (bytes * 4 + 2) / 3;
  }
}
