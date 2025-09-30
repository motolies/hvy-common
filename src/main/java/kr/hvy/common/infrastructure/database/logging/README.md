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

SELECT 쿼리의 결과를 log4jdbc 스타일의 테이블 형식으로 로깅할 수 있습니다.

### 설정

```yaml
hvy:
  sql:
    datasource-wrapper:
      enable-wrapper: true
      enable-result-set-logging: true  # ResultSet 테이블 로깅 활성화
      max-result-set-rows: 100  # 최대 표시 행 수 (기본값: Integer.MAX_VALUE)
      max-result-set-columns: 20  # 최대 표시 컬럼 수 (기본값: Integer.MAX_VALUE)
      max-column-value-length: 50  # 컬럼 값 최대 표시 길이 (기본값: Integer.MAX_VALUE)
```

### 설정 속성 설명

| 속성 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `enable-result-set-logging` | boolean | `false` | ResultSet 테이블 로깅 활성화 여부 (성능 영향 고려) |
| `max-result-set-rows` | int | `Integer.MAX_VALUE` | 로그에 표시할 최대 행 수 (메모리 보호) |
| `max-result-set-columns` | int | `Integer.MAX_VALUE` | 로그에 표시할 최대 컬럼 수 |
| `max-column-value-length` | int | `Integer.MAX_VALUE` | 각 컬럼 값의 최대 표시 길이 (긴 문자열 자르기) |

### 출력 예시

```
2025-09-30 17:48:56.831 [INFO] ResultSet (3 rows):
|---|----------|-------------------|
|id |name      |email              |
|---|----------|-------------------|
|1  |홍길동    |hong@example.com   |
|2  |김철수    |kim@example.com    |
|3  |이영희    |lee@example.com    |
|---|----------|-------------------|
```

### 성능 고려사항

- **기본값은 비활성화**: `enable-result-set-logging`의 기본값은 `false`로 성능 영향을 최소화합니다.
- **메모리 보호**: 대용량 ResultSet의 경우 `max-result-set-rows`를 설정하여 메모리 사용을 제한하세요.
- **개발/디버깅 용도**: 운영 환경보다는 개발 및 디버깅 환경에서 사용을 권장합니다.
- **로그 레벨**: ResultSet 로깅은 INFO 레벨로 출력되므로 적절한 로그 레벨 설정이 필요합니다.

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
      max-column-value-length: 50  # 컬럼 값 50자로 제한
```

