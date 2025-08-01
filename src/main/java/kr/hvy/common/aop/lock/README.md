# redissonClient Lock 분산락

아래처럼 키의 이름을 생성할 수 있음 
```java
// 단일 표현
@DistributedLock(key = "'prefix:middle_name:' + #updateRequest.getId()")
public void update(UpdateRequest updateRequest) {
  //...
}

// 빈 사용시
@DistributedLock(key = "'prefix:middle_name:' + @mobRepository.findById(#updateRequest.getId())")
public void update(UpdateRequest updateRequest) {
  //...
}
```
