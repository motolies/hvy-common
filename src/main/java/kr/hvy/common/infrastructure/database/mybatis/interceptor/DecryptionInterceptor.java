package kr.hvy.common.infrastructure.database.mybatis.interceptor;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import kr.hvy.common.application.crypto.Decryptor;
import kr.hvy.common.application.crypto.annotation.EncryptedField;
import kr.hvy.common.application.crypto.annotation.SecretData;
import kr.hvy.common.config.executor.TraceExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.springframework.util.CollectionUtils;

@Slf4j
@Intercepts({
    @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})
})
@RequiredArgsConstructor
public class DecryptionInterceptor implements Interceptor {

  private final Decryptor decryptor;

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    Object resultObject = invocation.proceed();
    if (Objects.isNull(resultObject)) {
      return null;
    }

    if (resultObject instanceof List) {
      List resultList = (List) resultObject;

      if (!CollectionUtils.isEmpty(resultList) && checkSecretData(resultList.get(0))) {
        decryptListParallel(resultList);
      }
    } else {
      if (checkSecretData(resultObject)) {
        decryptData(resultObject);
      }
    }
    return resultObject;
  }

  /**
   * 원소별 복호화를 병렬로 수행한다.
   * <p>
   * 병렬을 유지하는 근거: AESUtil 은 값마다 랜덤 salt 를 쓰므로 EncryptedField 하나당
   * PBKDF2(8192 iterations) 키 유도가 매번 새로 일어난다(실측 1ms+/건). 컨텍스트 스냅샷 캡처·복원 비용은
   * 그보다 세 자릿수 저렴해서 오버헤드로 문제되지 않는다.
   * <p>
   * executor 미지정 {@code CompletableFuture.runAsync} 대신 {@link TraceExecutors} 를 쓰는 이유:
   * ① ForkJoinPool.commonPool 은 traceId 를 물려받지 못해 여기서 찍히는 오류 로그가 요청과 끊긴다
   * ② commonPool 은 JVM 전역 자원이라 수 ms 단위 CPU 작업을 몰아넣으면 다른 사용자를 굶긴다
   * ③ vCPU 1개 컨테이너에서는 commonPool parallelism 이 0이 되어 원소마다 플랫폼 스레드가 새로 생긴다
   */
  private void decryptListParallel(List<Object> resultList) {
    // try-with-resources 의 close() 가 shutdown + awaitTermination 이라 전 원소 완료까지 대기한다
    try (ExecutorService pool = TraceExecutors.virtualThreadPerTask("crypto-decrypt-")) {
      for (Object result : resultList) {
        pool.submit(() -> {
          try {
            decryptData(result);
          } catch (Exception e) {
            log.error("decrypt error", e);
          }
        });
      }
    }
  }

  private boolean checkSecretData(Object object) {
    Class<?> objectClass = object.getClass();
    return objectClass.isAnnotationPresent(SecretData.class);
  }

  private void decryptData(Object object) throws Exception {
    Class<?> entityClass = object.getClass();
    Field[] fields = entityClass.getDeclaredFields();
    for (Field field : fields) {
      if (field.isAnnotationPresent(EncryptedField.class)) {
        field.setAccessible(true);
        if (field.getType().equals(String.class)) {
          String value = (String) field.get(object);
          String decryptedValue = decryptor.decrypt(value);
          field.set(object, decryptedValue);
        }
      }
    }
  }

  @Override
  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  @Override
  public void setProperties(Properties properties) {

  }
}
