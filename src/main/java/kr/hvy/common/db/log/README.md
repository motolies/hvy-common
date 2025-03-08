# sql monitoring

```groovy
implementation 'net.ttddyy:datasource-proxy:1.10.1'
```

```yaml
# 로그 레벨 
log:
  level:
    kr.hvy.common.db.log: DEBUG

# datasource-proxy 설정
datasource-wrapper:
  wrap: true # wrap 여부, wrap=true로 설정하면 datasource-proxy가 datasource를 감싸서 사용
  format: true # SQL 포맷팅 여부
  names: # 여러 데이터소스를 사용할 경우 이름을 지정
    - "dataSource"
    - "readDataSource"
    - "writeDataSource"
```

