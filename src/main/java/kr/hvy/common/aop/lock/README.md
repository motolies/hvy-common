# redissonClient Lock 분산락

아래처럼 키의 이름을 생성할 수 있음 
```java
@DistributedLock(key = "'prefix:middle_name:' + #updateRequest.getId()")
public void update(UpdateRequest updateRequest) {
  //...
}
```
