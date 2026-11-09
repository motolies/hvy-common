package kr.hvy.common.application.crypto.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import kr.hvy.common.application.crypto.DecryptionException;
import kr.hvy.common.application.crypto.Decryptor;
import kr.hvy.common.application.crypto.EncryptionException;
import kr.hvy.common.application.crypto.Encryptor;
import org.springframework.util.StringUtils;

/**
 * 저장 데이터 암호화 유틸 — AES-256/CBC, 값마다 PBKDF2 키 유도 (레거시).
 *
 * <p>이전 {@code AESUtil} 의 알고리즘과 암호문 포맷을 그대로 유지한다. 기존 CBC 암호문을 가진 프로젝트의 호환용이며,
 * 신규 프로젝트는 {@link AESGcmUtil} 을 쓴다. 한 프로젝트는 둘 중 하나만 사용한다(암호문 마커가 없어 서로 구분할 수 없다).
 *
 * <table border="1">
 *   <caption>CBC / GCM 비교</caption>
 *   <tr><th>항목</th><th>AESCbcUtil (이 클래스)</th><th>AESGcmUtil</th></tr>
 *   <tr><td>키 유도</td><td>값마다 랜덤 salt 16B 로 PBKDF2 8,192회</td><td>고정 salt(입력)로 기동 시 1회 PBKDF2 600,000회</td></tr>
 *   <tr><td>IV</td><td>16B 랜덤</td><td>12B 랜덤</td></tr>
 *   <tr><td>무결성</td><td>없음 — 변조·키 불일치를 검출하지 못하고 드물게 깨진 문자열을 반환할 수 있다</td><td>16B 인증 태그로 변조·키 불일치 검출</td></tr>
 *   <tr><td>암호문 포맷</td><td>Base64url(no-pad)(salt ‖ iv ‖ ciphertext)</td><td>Base64url(no-pad)(iv ‖ ciphertext ‖ tag)</td></tr>
 *   <tr><td>필드당 비용</td><td>수 ms (매번 키 유도) — 대량 조회 시 누적된다</td><td>수 µs</td></tr>
 *   <tr><td>키 검증</td><td>빈값만 거부 (기존 키 호환)</td><td>32자 이상 + salt 필수</td></tr>
 *   <tr><td>마커</td><td colspan="2">둘 다 없음 — 평문과 암호문을 구분할 수 없어 혼재 불가. 비어 있지 않은 값은 모두 암호문으로 간주한다</td></tr>
 * </table>
 */
public class AESCbcUtil implements Encryptor, Decryptor {

  private static final String KEY_ALGORITHM = "AES";
  private static final String FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

  private static final int KEY_LENGTH = 256;
  private static final int ITERATION_COUNT = 8192;
  private static final int IV_LENGTH = 16;
  private static final int SALT_LENGTH = 16;
  /** AES 블록 크기. PKCS5 패딩 때문에 암호문 본문은 최소 1블록이다. */
  private static final int BLOCK_SIZE = 16;

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private final String secretKey;

  /**
   * 비밀값을 검증해 보관한다. 키 유도는 값마다 랜덤 salt 로 수행하므로 여기서는 하지 않는다.
   *
   * @throws IllegalStateException 비밀값이 비어 있을 때 (컨텍스트 기동 실패)
   */
  public AESCbcUtil(String secretKey) {
    if (!StringUtils.hasText(secretKey)) {
      throw new IllegalStateException("암호화 키가 설정되지 않았습니다.");
    }
    this.secretKey = secretKey;
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
    byte[] salt = randomBytes(SALT_LENGTH);
    byte[] iv = randomBytes(IV_LENGTH);
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, deriveKey(salt), new IvParameterSpec(iv));
      byte[] cipherText = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

      byte[] payload = new byte[SALT_LENGTH + IV_LENGTH + cipherText.length];
      System.arraycopy(salt, 0, payload, 0, SALT_LENGTH);
      System.arraycopy(iv, 0, payload, SALT_LENGTH, IV_LENGTH);
      System.arraycopy(cipherText, 0, payload, SALT_LENGTH + IV_LENGTH, cipherText.length);
      return ENCODER.encodeToString(payload);
    } catch (GeneralSecurityException e) {
      throw new EncryptionException("CBC 암호화 실패", e);
    }
  }

  /**
   * 암호문을 복호화한다. 빈값/null 은 그대로 반환한다.
   *
   * @throws DecryptionException Base64 아님·길이 부족·패딩 오류(키 불일치·손상·평문 입력)
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
    if (payload.length < SALT_LENGTH + IV_LENGTH + BLOCK_SIZE) {
      throw new DecryptionException("암호문 길이 부족 (bytes=" + payload.length + ")");
    }
    try {
      byte[] salt = new byte[SALT_LENGTH];
      System.arraycopy(payload, 0, salt, 0, SALT_LENGTH);

      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, deriveKey(salt), new IvParameterSpec(payload, SALT_LENGTH, IV_LENGTH));
      int offset = SALT_LENGTH + IV_LENGTH;
      byte[] plain = cipher.doFinal(payload, offset, payload.length - offset);
      return new String(plain, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException e) {
      throw new DecryptionException("CBC 복호화 실패 (bytes=" + payload.length + ")", e);
    }
  }

  /** 값마다 받은 salt 로 PBKDF2 256비트 키를 유도한다. 암복호화 호출마다 실행되는 비용의 대부분이다. */
  private SecretKey deriveKey(byte[] salt) throws GeneralSecurityException {
    SecretKeyFactory factory = SecretKeyFactory.getInstance(FACTORY_ALGORITHM);
    byte[] keyBytes = factory
        .generateSecret(new PBEKeySpec(secretKey.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH))
        .getEncoded();
    return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
  }

  /** 지정 길이의 난수 바이트를 만든다(salt·IV 용). */
  private static byte[] randomBytes(int length) {
    byte[] bytes = new byte[length];
    RANDOM.nextBytes(bytes);
    return bytes;
  }
}
