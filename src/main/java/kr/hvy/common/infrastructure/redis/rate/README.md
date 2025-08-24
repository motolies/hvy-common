# redissonClient RateLimit 분산리미터

아래처럼 키의 이름을 생성할 수 있음

```java
// 기존 방식 (함수명 기반)
@DistributedRateLimit
public void someMethod() { ...}

// 고정 키 - 더 명확해짐
@DistributedRateLimit(key = "api:upload")
public void uploadFile() { ...}

// SpEL 동적 키 - 더 직관적
@DistributedRateLimit(key = "'user:' + #userId")
public void processUser(String userId) { ...}

@DistributedRateLimit(key = "#request.userType + ':' + #request.action")
public void handleRequest(UserRequest request) { ...}
```
