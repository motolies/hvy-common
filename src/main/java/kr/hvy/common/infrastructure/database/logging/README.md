# SQL Monitoring

datasource-proxy를 사용하여 SQL 쿼리 로깅 및 모니터링 기능을 제공합니다.

## 의존성

```groovy
// 공통 모듈에 포함되어서 앱에서는 추가할 필요 없음
implementation 'net.ttddyy:datasource-proxy:1.10.1'
```

## 빈 등록

```java
  @Bean
  @ConfigurationProperties(prefix = "hvy.sql.datasource-wrapper")
  public DataSourceProxySettingProperty dataSourceProxySettingProperty() {
    return new DataSourceProxySettingProperty();
  }

  @Bean
  public DataSourceWrapperPostProcessor dataSourceWrapperPostProcessor(DataSourceProxySettingProperty dataSourceProxySettingProperty) {
    return new DataSourceWrapperPostProcessor(dataSourceProxySettingProperty);
  }
```

## 기본 설정

```yaml
# 로그 레벨
logging:
  level:
    kr.hvy:
      common.infrastructure.database.logging: DEBUG

# datasource-proxy 설정
hvy:
  sql:
    datasource-wrapper:
      enable-wrapper: true # wrap 여부
      format: true # SQL 포맷팅 여부
      slow-query-threshold: 500 # 느린 쿼리 임계값 (ms)
      data-source-names: # 여러 데이터소스를 사용할 경우 이름을 지정
        - "dataSource"
        - "readDataSource"
        - "writeDataSource"
```

## SELECT 결과 테이블 로깅

SELECT 쿼리의 결과를 GFM(GitHub Flavored Markdown) 표 형식으로 로깅할 수 있습니다.
공백 패딩이 없어 Loki JSON 한 줄 안에서도 깨지지 않고, 마크다운 뷰어나 LLM 에 그대로 붙여 넣으면 표로 읽힙니다.

### 설정

```yaml
hvy:
  sql:
    datasource-wrapper:
      enable-wrapper: true
      enable-result-set-logging: true  # ResultSet 테이블 로깅 활성화
      max-result-set-rows: 100  # 최대 표시 행 수 (기본값: Integer.MAX_VALUE)
      max-result-set-columns: 20  # 최대 표시 컬럼 수 (기본값: Integer.MAX_VALUE)
      max-column-value-length: 150  # 셀 값 최대 문자 수 (기본값: Integer.MAX_VALUE)
```

### 설정 속성 설명

| 속성 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `enable-result-set-logging` | boolean | `false` | ResultSet 테이블 로깅 활성화 여부 (성능 영향 고려) |
| `max-result-set-rows` | int | `Integer.MAX_VALUE` | 로그에 표시할 최대 행 수. 초과분은 값을 읽지 않고 개수만 세어 `total` 로 표시 (메모리 보호) |
| `max-result-set-columns` | int | `Integer.MAX_VALUE` | 로그에 표시할 최대 컬럼 수 |
| `max-column-value-length` | int | `Integer.MAX_VALUE` | 셀 값 최대 **문자 수**(UTF-16, 한글도 1자). 초과 시 앞부분 + `...` |

> `max-column-value-length` 는 이전 ASCII 표에서는 표시 너비(한글 2칸) 기준이었으나 이제 문자 수 기준입니다.
> 같은 값이면 한글 셀이 최대 2배까지 길게 남습니다.

### 출력 형식

로그 메시지는 `ResultSet` + 요약 줄, 빈 줄, 표 순서입니다(로거: `ResultSetTableLogger`, 레벨: INFO).

- 요약: `rows=N`. 표시 한도를 넘으면 `rows=50 total=2345` 처럼 전체 행 수를 함께 표시합니다.
- 1행 결과는 세로표(`| column | value |`), 여러 행은 가로표입니다. 0행 결과는 로그를 남기지 않습니다.
- 조인 등으로 같은 라벨이 반복되면 두 번째부터 `.2`, `.3` 을 붙입니다(대소문자 무시).

**여러 행**
```
ResultSet rows=3

| id | name | email | created_at |
|---|---|---|---|
| 1 | 홍길동 | hong@example.com | 2026-11-09T10:00:00.123456+09:00 |
| 2 | 김철수 | null | 2026-11-09T10:05:00+09:00 |
| 3 | "" | lee@example.com | 2026-11-09T10:10:00+09:00 |
```

**한 행**
```
ResultSet rows=1

| column | value |
|---|---|
| id | 1 |
| name | 홍길동 |
```

Loki 에서 결과표만 찾을 때는 `|= "ResultSet rows="` 로 거를 수 있습니다.

### 셀 디코딩 규칙

| 로그 표기 | 의미 |
|-----------|------|
| `null` | SQL NULL |
| `""`, `"null"`, `" z "`, `""abc""` | 문자열 값. 비었거나, 문자열 null(대소문자 무시)이거나, 따옴표로 시작하거나, 앞뒤 공백이 있을 때만 따옴표로 감쌈. 바깥 한 쌍만 벗기면 원래 값 |
| 역슬래시 + 문자 | 이스케이프된 문자 하나 (아래 목록) |
| `\n` `\r` | 개행·캐리지리턴(`\r\n` 은 `\n` 하나) |
| `2026-11-09T10:00:00.123456+09:00` | 순간 값(Timestamp·Instant·OffsetDateTime·ZonedDateTime·Date·Calendar). JVM 기본 시간대 오프셋을 붙인 ISO-8601 |
| `2026-11-09`, `2026-11-09T10:00` | 지역 값(LocalDate·LocalDateTime·sql.Date·sql.Time). 시간대 없는 ISO 그대로 |
| 앞부분 + `...` | `max-column-value-length` 초과 절단(이스케이프 전 원문 기준, 서로게이트 쌍은 가르지 않음) |
| `[error: ...]` | 해당 칸을 읽다 드라이버 오류 발생 |

이스케이프 대상:

- 항상: 역슬래시 `\\`, 파이프 `\|`, 별표 `\*`, 물결표 `\~`, 백틱(역슬래시 + 백틱)
- 마크다운 구문이 되는 자리에서만: `\_`(단어 밖 밑줄), `\<`(태그·autolink 시작), `\]`(링크 꼬리 `](`·`][`), `\&`(엔티티 참조 꼴)
- 예: `snake_case_value`, `a < b`, `[1,2]`, `a=1&b=2` 는 그대로 남습니다.

### 오류 처리

- 결과 수집·표 렌더링 중 오류는 ERROR 로그만 남기고 애플리케이션에는 전파하지 않습니다(처음 10건은 스택 포함, 이후 1,000건마다 건수만).
- 드라이버가 던진 `SQLException` 은 감싸지 않고 그대로 애플리케이션에 전달됩니다.
- `close()` 가 여러 번 호출되어도 표는 한 번만 남깁니다.

### 성능 고려사항

- **기본값은 비활성화**: `enable-result-set-logging`의 기본값은 `false`로 성능 영향을 최소화합니다.
- **메모리 보호**: 대용량 ResultSet의 경우 `max-result-set-rows`를 설정하여 메모리 사용을 제한하세요.
- **개발/디버깅 용도**: 운영 환경보다는 개발 및 디버깅 환경에서 사용을 권장합니다.
- **로그 레벨**: ResultSet 로깅은 INFO 레벨로 출력되며, 해당 로거가 INFO 미만으로 꺼져 있으면 값을 수집하지 않습니다.

### 사용 예시

**개발 환경 (전체 결과 보기)**
```yaml
hvy:
  sql:
    datasource-wrapper:
      enable-result-set-logging: true
      # 기본값 사용 (무제한)
```

**운영 환경 (제한적 사용)**
```yaml
hvy:
  sql:
    datasource-wrapper:
      enable-result-set-logging: false  # 비활성화 권장
```

**디버깅 환경 (대용량 데이터)**
```yaml
hvy:
  sql:
    datasource-wrapper:
      enable-result-set-logging: true
      max-result-set-rows: 100  # 처음 100행만 표시
      max-column-value-length: 50  # 셀 값 50자로 제한
```

