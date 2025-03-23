# sql monitoring

```groovy
// 공통 모듈에 포함되어서 앱에서는 추가할 필요 없음
implementation 'net.ttddyy:datasource-proxy:1.10.1'
```

```yaml
# 로그 레벨 
log:
  level:
    kr.hvy.common.db.log: DEBUG

# datasource-proxy 설정
hvy:
  sql:
    datasource-wrapper:
      enable-wrapper: true # wrap 여부
      format: true # SQL 포맷팅 여부
      slow-query-threshold: 500 # 느린 쿼리 임계값
      data-source-names: # 여러 데이터소스를 사용할 경우 이름을 지정
        - "dataSource"
        - "readDataSource"
        - "writeDataSource"
```

