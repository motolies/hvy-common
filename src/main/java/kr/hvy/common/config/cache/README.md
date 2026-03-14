# L1(Caffeine) + L2(Redis) 2단계 캐시 시스템

멀티 Pod 환경에서 일관성 있는 캐시를 제공하기 위한 2단계 캐시 구현체입니다.
L1(Caffeine)으로 빠른 로컬 조회를, L2(Redis)로 Pod 간 공유 캐시를, Redis Pub/Sub으로 크로스 Pod L1 무효화를 처리합니다.

> Last updated: 2026-03-15

---

## 아키텍처 개요

```
┌─────────────────────────────────┐     ┌─────────────────────────────────┐
│            Pod 1                │     │            Pod 2                │
│  ┌───────────┐                  │     │                  ┌───────────┐  │
│  │ L1 Caffeine│ ◄── evictLocal ─┼─────┼── Pub/Sub ──►   │ L1 Caffeine│  │
│  └─────┬─────┘                  │     │                  └─────┬─────┘  │
│        │                        │     │                        │        │
│        ▼                        │     │                        ▼        │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     L2 Redis (공유)                              │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│        │                        │     │                        │        │
│        ▼                        │     │                        ▼        │
│     DB (valueLoader)            │     │             DB (valueLoader)    │
└─────────────────────────────────┘     └─────────────────────────────────┘
```

---

## 조회 흐름

```
요청 ──► L1(Caffeine) hit? ──YES──► 반환
              │
              NO
              ▼
         L2(Redis) hit? ──YES──► L1에 저장 + 반환
              │
              NO
              ▼
         valueLoader(DB) 호출 ──► L1 + L2 저장 + 반환
```

1. **L1 hit**: Caffeine 로컬 캐시에서 즉시 반환 (나노초 단위)
2. **L1 miss, L2 hit**: Redis에서 JSON 조회 후 역직렬화, L1에 승격(promote) 후 반환
3. **L1+L2 miss**: `valueLoader` (일반적으로 DB 쿼리)를 호출하고 L1 + L2 모두에 저장

## 무효화 흐름 (CUD 발생 시)

```
Pod 1: evict(key) 호출
   ├──► L1(Caffeine) evict
   ├──► L2(Redis) evict
   └──► Redis Pub/Sub publish(EVICT, cacheName, key)
            │
            ▼
   Pod 2: 메시지 수신
   ├── sourceInstanceId == 자기 자신? ──YES──► 무시
   └── NO ──► evictLocal(key) ──► L1만 삭제
```

1. `evict()` 호출 시 로컬 L1과 공유 L2를 모두 삭제합니다.
2. Redis Pub/Sub으로 `EVICT` 또는 `CLEAR` 메시지를 발행합니다.
3. 다른 Pod의 `CacheInvalidationListener`가 메시지를 수신하면 **L1만 삭제**합니다.
   - L2는 공유 자원이므로 이미 삭제된 상태입니다.
4. `sourceInstanceId`가 자기 자신인 메시지는 무시합니다 (이중 삭제 방지).

---

## 설계 원칙

| 원칙 | 설명 |
|------|------|
| **Graceful Degradation** | 모든 L2(Redis) 작업은 try-catch로 감싸져 있어, Redis 장애 시 L1(Caffeine)만으로 동작합니다. |
| **Pub/Sub 발행 실패 허용** | 발행 실패 시 warn 로그만 남깁니다. L1 TTL로 자연 만료되므로 데이터 정합성은 TTL 범위 내에서 보장됩니다. |
| **자기 메시지 필터링** | `UUID.randomUUID()`로 생성된 `sourceInstanceId`를 통해 자기 Pod가 발행한 메시지를 무시합니다. |
| **L2는 JSON String 저장** | `ObjectMapperConfigurer.getObjectMapper()`를 사용하며, default typing 없이 순수 JSON으로 저장합니다. |
| **Spring Cache 호환** | `TwoTierCache`가 `org.springframework.cache.Cache` 인터페이스를 구현하므로, `@Cacheable` / `@CacheEvict` 등 기존 어노테이션을 그대로 사용할 수 있습니다. |

---

## 파일 구성

### hvy-common: 캐시 코어 (`kr.hvy.common.config.cache`)

| 파일 | 역할 |
|------|------|
| `TwoTierCacheProperties.java` | L1/L2 캐시 설정 record. `l1Only()`, `twoTier()` 팩토리 메서드 제공 |
| `TwoTierCache.java` | Spring `Cache` 인터페이스 구현체. L1 -> L2 -> valueLoader 조회 체인 |
| `TwoTierCacheConfigurer.java` | 모듈용 베이스 설정 클래스. `CacheManager` 빈 생성 헬퍼 |

### hvy-common: Redis Pub/Sub (`kr.hvy.common.infrastructure.redis.cache`)

| 파일 | 역할 |
|------|------|
| `CacheInvalidationMessage.java` | Pub/Sub 메시지 DTO (record). `EVICT` / `CLEAR` 타입 |
| `CacheInvalidationPublisher.java` | RTopic(`cache:invalidation`)으로 무효화 메시지 발행 |
| `CacheInvalidationListener.java` | Pub/Sub 구독. 수신 시 `TwoTierCache.evictLocal()` / `clearLocal()` 호출 |
| `CacheInvalidationConfig.java` | `@ConditionalOnBean(RedissonClient.class)`로 Redis 존재 시에만 Publisher/Listener 빈 등록 |

---

## 주요 클래스 상세

### TwoTierCacheProperties

캐시 하나의 설정을 담는 record입니다. 두 가지 팩토리 메서드를 제공합니다.

```java
// L1(Caffeine) 전용 캐시
TwoTierCacheProperties.l1Only(
    "searchEngine",      // name
    Duration.ofMinutes(10), // l1Ttl
    200,                 // l1MaxSize
    false                // allowNullValues
);

// L1(Caffeine) + L2(Redis) 2단계 캐시
TwoTierCacheProperties.twoTier(
    "masterCodeTree",    // name
    Duration.ofMinutes(10), // l1Ttl
    50,                  // l1MaxSize
    Duration.ofHours(1)  // l2Ttl
);
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `name` | `String` | 캐시 이름 (`@Cacheable(cacheNames = "...")` 에서 사용) |
| `l1Ttl` | `Duration` | L1 Caffeine `expireAfterWrite` 값 |
| `l1MaxSize` | `int` | L1 Caffeine `maximumSize` 값 |
| `allowNullValues` | `boolean` | null 값 캐싱 허용 여부 (L1 전용 캐시에서 사용) |
| `l2Enabled` | `boolean` | L2 Redis 사용 여부 |
| `l2Ttl` | `Duration` | L2 Redis 항목 TTL |
| `l2KeyPrefix` | `String` | L2 Redis `RMapCache` 이름 (자동 생성: `"cache:" + name`) |

### TwoTierCache

Spring `Cache` 인터페이스의 핵심 구현체입니다.

**주요 메서드:**

| 메서드 | 동작 |
|--------|------|
| `get(key)` | L1 -> L2 순서로 조회. L2 hit 시 L1에 승격 |
| `get(key, valueLoader)` | L1 -> L2 -> valueLoader 조회 체인. `@Cacheable`에서 주로 사용 |
| `put(key, value)` | L1 + L2 동시 저장 |
| `evict(key)` | L1 + L2 삭제 후 Pub/Sub 발행 |
| `clear()` | L1 + L2 전체 삭제 후 Pub/Sub 발행 |
| `evictLocal(key)` | L1만 삭제 (다른 Pod에서 수신한 무효화 메시지 처리용) |
| `clearLocal()` | L1 전체 삭제 (다른 Pod에서 수신한 무효화 메시지 처리용) |
| `getNativeCache()` | 내부 Caffeine 캐시 반환 (Actuator 통계 조회 호환) |

### TwoTierCacheConfigurer

모듈에서 상속하여 `CacheManager`를 생성하는 베이스 클래스입니다.

- `twoTierCacheManager(List<TwoTierCacheProperties>)`: `SimpleCacheManager`를 생성하고, 각 설정에 대해 `TwoTierCache`를 만들어 등록합니다.
- `getCacheRegistry()`: 이름으로 `TwoTierCache`를 찾을 수 있는 `Map`을 반환합니다. `CacheInvalidationListener`가 이를 통해 대상 캐시를 찾습니다.
- 의존성 주입 (`@Autowired(required = false)`):
  - `RedissonClient`: 없으면 L2 비활성화
  - `CacheInvalidationPublisher`: 없으면 Pub/Sub 비활성화
  - `CacheLoggingProperties`: 없으면 기본 `recordStats()` 사용

---

## 사용 방법 (모듈에서의 적용)

### 1. 캐시 타입 정의

```java
@Getter
@AllArgsConstructor
public enum CacheType {

  // L1 전용
  SEARCH_ENGINE(TwoTierCacheProperties.l1Only("searchEngine", Duration.ofMinutes(10), 200, false)),
  CATEGORY(TwoTierCacheProperties.l1Only("category", Duration.ofDays(1), 200, false)),

  // L1 + L2
  MASTER_CODE_TREE(TwoTierCacheProperties.twoTier("masterCodeTree", Duration.ofMinutes(10), 50, Duration.ofHours(1))),
  MASTER_CODE_NODE(TwoTierCacheProperties.twoTier("masterCodeNode", Duration.ofMinutes(10), 200, Duration.ofHours(1))),
  MASTER_CODE_CHILDREN(TwoTierCacheProperties.twoTier("masterCodeChildren", Duration.ofMinutes(10), 100, Duration.ofHours(1)));

  private final TwoTierCacheProperties properties;
}
```

### 2. CacheConfig 작성

```java
@Configuration
public class CacheConfig extends TwoTierCacheConfigurer {

  @Bean
  public CacheManager cacheManager() {
    return super.twoTierCacheManager(
        Arrays.stream(CacheType.values())
            .map(CacheType::getProperties)
            .toList()
    );
  }
}
```

### 3. 서비스에서 사용

기존 Spring Cache 어노테이션을 그대로 사용합니다.

```java
@Cacheable(cacheNames = "masterCodeTree", key = "'root'")
public List<MasterCodeTreeResponse> getMasterCodeTree() {
    // DB 조회 로직
}

@CacheEvict(cacheNames = "masterCodeTree", key = "'root'")
public void updateMasterCode(MasterCodeUpdateRequest request) {
    // 수정 로직 - 메서드 완료 후 캐시 자동 삭제
}
```

---

## 조건부 활성화

| 조건 | 동작 |
|------|------|
| `RedissonClient` 빈 존재 | L2 활성화, Pub/Sub Publisher/Listener 빈 등록 |
| `RedissonClient` 빈 없음 | L1(Caffeine) 전용으로 동작. L2 관련 코드 모두 비활성화 |
| `TwoTierCacheConfigurer` 빈 없음 | `CacheInvalidationListener` 빈 미등록 |
| `l2Enabled = false` (l1Only 팩토리) | 해당 캐시의 L2 비활성화, Pub/Sub 발행 안 함 |

---

## 장애 시나리오별 동작

### Redis 완전 장애

1. L2 읽기/쓰기 실패 시 warn 로그 후 **L1만으로 동작** 계속
2. Pub/Sub 발행 실패 시 warn 로그만 남김
3. 다른 Pod의 L1은 각자의 TTL이 만료되면 자연 갱신

### Redis 일시적 장애

1. 장애 중: L1 전용 모드로 동작
2. 복구 후: 새로운 요청부터 L2에 다시 저장 시작, Pub/Sub 자동 복구 (Redisson 내장 재연결)

### 단일 Pod 재시작

1. L1은 메모리 캐시이므로 재시작 시 비어 있음
2. L2(Redis)에 데이터가 남아 있으므로 첫 조회 시 L2에서 L1으로 승격
