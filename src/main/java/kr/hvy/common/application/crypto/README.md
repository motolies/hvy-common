# 암호화 모듈 사용방법

MyBatis 인터셉터 + 어노테이션으로 저장 데이터(at-rest)를 AES-256 으로 암호화한다.
암호화 방식은 **CBC(레거시)** 와 **GCM(권장)** 두 가지이며, **한 프로젝트는 둘 중 하나만 사용**한다.

## 1. 구성요소

| 구성      | 클래스                                                                   | 역할                                                                     |
|---------|-----------------------------------------------------------------------|------------------------------------------------------------------------|
| 암호화 유틸  | `util.AESGcmUtil`                                                     | AES-256/GCM. 기동 시 키 1회 유도, 값마다 랜덤 12B IV, 인증 태그로 변조 검출 (신규 권장)            |
|         | `util.AESCbcUtil`                                                     | AES-256/CBC. 값마다 랜덤 salt 로 키 유도 (이전 `AESUtil` 과 포맷 동일, 기존 데이터 호환용)         |
| 쓰기 인터셉터 | `infrastructure.database.mybatis.interceptor.EncryptionInterceptor`   | `ParameterHandler.setParameters` 를 가로채 `@EncryptedField` 암호화 → 바인딩 → 평문 복원 |
| 읽기 인터셉터 | `infrastructure.database.mybatis.interceptor.DecryptionInterceptor`   | `ResultSetHandler.handleResultSets` 를 가로채 `@EncryptedField` 복호화        |
| 예외      | `CryptoException` ← `EncryptionException`, `DecryptionException`       | 암복호화 실패(런타임). 인터셉터는 삼키지 않고 전파한다                                     |
| 마킹      | `@SecretData`(클래스), `@EncryptedField`(필드)                             | 암호화 대상 표시                                                              |
| 해시      | `util.HashUtil`                                                       | SHA3-256. 검색용 blind index 컬럼 생성(§7)                                    |

인터셉터는 `Encryptor`/`Decryptor` 인터페이스에만 의존하므로 CBC·GCM 어느 쪽이든 끼울 수 있다.

## 2. CBC / GCM 비교

| 항목      | `AESCbcUtil`                                  | `AESGcmUtil`                             |
|---------|-----------------------------------------------|------------------------------------------|
| 키 유도    | 값마다 랜덤 salt 16B 로 PBKDF2 8,192회                | 고정 salt(설정값)로 **기동 시 1회** PBKDF2 600,000회 |
| IV      | 16B 랜덤                                        | 12B 랜덤                                   |
| 무결성     | 없음 — 변조·키 불일치를 검출하지 못하고 드물게 깨진 문자열을 반환할 수 있다 | 16B 인증 태그로 변조·키 불일치 검출                    |
| 암호문 포맷  | `Base64url(no-pad)(salt ‖ iv ‖ ciphertext)`   | `Base64url(no-pad)(iv ‖ ciphertext ‖ tag)` |
| 필드당 비용  | 수 ms (매번 키 유도) — 대량 조회 시 순차로 누적된다             | 수 µs                                     |
| 키 검증    | 빈값만 거부 (기존 키 호환)                              | 32자 이상 + salt 필수                         |
| 용도      | 이미 CBC 로 암호화된 데이터가 있는 프로젝트                     | 신규 프로젝트                                  |

**공통: 암호문에 마커가 없다.** 평문과 암호문을 구분할 수 없으므로 비어 있지 않은 값은 모두 암호문으로 간주하고,
복호화에 실패하면 `DecryptionException` 을 던진다(평문을 그대로 통과시키지 않는다).

## 3. 설정

`application.yml` (프로퍼티 이름은 예시. 생성자에 값을 넘기기만 하면 된다)

```yaml
system:
  crypto:
    # GCM 사용 시
    aes-gcm:
      key: ${SYSTEM_CRYPTO_AES_GCM_KEY}     # 32자 이상. 기본값을 두지 않아 미주입 시 기동 실패
      salt: ${SYSTEM_CRYPTO_AES_GCM_SALT}   # 시스템마다 고유하게 정하고 이후 변경 금지
    # CBC(레거시) 사용 시
    aes:
      key: ${SYSTEM_CRYPTO_AES_KEY}
```

Bean 등록 — **방식 하나, 인터셉터 쌍 하나만** 등록한다 (GCM 예시)

```java
@Configuration
public class CryptoConfig {

  @Bean
  public AESGcmUtil aesGcmUtil(@Value("${system.crypto.aes-gcm.key}") String key,
      @Value("${system.crypto.aes-gcm.salt}") String salt) {
    return new AESGcmUtil(key, salt);
  }

  // MeterRegistry 는 ObjectProvider 로 지연 해석해 SqlSessionFactory 초기화 순서 문제를 피한다
  @Bean
  public EncryptionInterceptor encryptionInterceptor(AESGcmUtil aesGcmUtil,
      ObjectProvider<MeterRegistry> meterRegistry) {
    return new EncryptionInterceptor(aesGcmUtil, meterRegistry);
  }

  @Bean
  public DecryptionInterceptor decryptionInterceptor(AESGcmUtil aesGcmUtil,
      ObjectProvider<MeterRegistry> meterRegistry) {
    return new DecryptionInterceptor(aesGcmUtil, meterRegistry);
  }
}
```

CBC 는 `new AESCbcUtil(key)` 로 바꾸면 된다. 메트릭이 필요 없으면 `new EncryptionInterceptor(util)` 처럼 단일 인자 생성자를 쓴다.

> mybatis-spring-boot-starter 자동설정이 컨텍스트의 `Interceptor` 타입 빈을 SqlSessionFactory 에 자동 등록한다.

## 4. 사용법 (MyBatis DTO 마킹)

```java
@SecretData                  // 이 클래스(또는 슈퍼클래스)에 암호화 필드가 있음
public class MemberDao {

  private Long id;

  @EncryptedField          // 실제 암호화 대상 (non-final String 만 허용, 위반 시 첫 사용에서 IllegalStateException)
  private String phone;
}
```

동작 범위:

- **쓰기**: 파라미터 객체를 순회(단일 객체, Map 의 값, Collection/배열 요소)해 `@SecretData` 객체의 `@EncryptedField` 를 암호화한 뒤
  바인딩하고, 바인딩이 끝나면 **원래 평문으로 복원**한다. 그래서 같은 DTO 를 다시 저장해도 이중 암호화되지 않는다.
  같은 객체가 여러 키로 들어와도(MyBatis 의 `collection`/`list`/파라미터명) 1회만 암호화한다.
  암호화 실패는 예외로 전파되어 평문이 저장되지 않는다(fail-closed).
- **읽기**: 결과 List 의 첫 non-null 요소가 `@SecretData` 면 행마다 `@EncryptedField` 를 복호화한다.
  스칼라 NULL 결과(`[null]`)·Map·다중 결과셋은 건드리지 않는다. 복호화 실패는 예외로 전파된다.
- 대상 해석은 `SecretFieldResolver` 가 클래스별로 캐시하며, `@SecretData`·`@EncryptedField` 를 슈퍼클래스까지 수집한다.

주의:

- 바인딩 구간(수 µs) 동안만 DTO 필드가 암호문 상태다. 같은 DTO 인스턴스를 다른 스레드와 동시에 공유하지 않는다.
- JPA 엔티티에는 적용되지 않는다(MyBatis 인터셉터 전용).

## 5. 도입·전환 시 주의 (마커 없음의 결과)

- **평문·암호문 혼재 불가**: 암호화 대상 컬럼에 평문 행이 남아 있으면 조회가 `DecryptionException` 으로 실패한다.
  기존 평문 데이터는 **배포 전에 한 번에 암호화**해야 한다(점검 시간 확보 또는 오프라인 마이그레이션).
- **방식 전환(CBC ↔ GCM)도 전체 재암호화**가 필요하다. 두 포맷을 구분할 수 없으므로 인터셉터 쌍을 섞어 등록하지 않는다.
- **키·salt 변경 금지**: 바꾸면 기존 암호문을 읽을 수 없다. 교체가 필요하면 전체 재암호화 절차로 다룬다.
- **컬럼 길이 확장**: 암호문은 Base64 로 길어진다.
  - GCM: `ceil((utf8길이 + 28) × 4 / 3)` 자 — `AESGcmUtil.cipherTextLength(utf8Length)` 로 계산 (예: 한글 60자 → 278자, 400자 → 1,638자)
  - CBC: `ceil((32 + 16 × (floor(utf8길이 / 16) + 1)) × 4 / 3)` 자 (salt + iv + PKCS5 패딩 블록)

## 6. 메트릭

`ObjectProvider<MeterRegistry>` 를 넘긴 경우에만 기록한다.

| 메트릭                          | 태그              | 의미                                     |
|------------------------------|-----------------|----------------------------------------|
| `hvy.crypto.encrypt.failure` | `class`,`field` | 쓰기 인터셉터 암호화 실패(저장 실패로 이어짐)              |
| `hvy.crypto.decrypt.failure` | `class`,`field` | 복호화 실패(키 불일치·손상·평문 혼입). 0 이 정상          |

## 7. 검색(blind index) 주의

CBC·GCM 모두 비결정적(같은 평문 → 매번 다른 암호문)이라 암호화 컬럼으로는 등호/LIKE/GROUP BY 가 불가능하다.
검색·집계가 필요하면 결정적 해시(`util.HashUtil`)로 별도 `*_hash` 컬럼을 두고 그 컬럼으로 조회한다.
