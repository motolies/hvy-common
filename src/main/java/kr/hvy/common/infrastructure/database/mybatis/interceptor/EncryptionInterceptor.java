package kr.hvy.common.infrastructure.database.mybatis.interceptor;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import kr.hvy.common.application.crypto.Encryptor;
import kr.hvy.common.application.crypto.annotation.EncryptedField;
import kr.hvy.common.application.crypto.annotation.SecretData;
import kr.hvy.common.config.executor.TraceExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.springframework.util.ObjectUtils;

@Slf4j
@Intercepts({
    @Signature(type = ParameterHandler.class, method = "setParameters", args = PreparedStatement.class),
})
@RequiredArgsConstructor
public class EncryptionInterceptor implements Interceptor {

  private final Encryptor encryptor;

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    encryptData(invocation);
    return invocation.proceed();
  }

  private void encryptData(Invocation invocation) throws Exception {
    ParameterHandler parameterHandler = (ParameterHandler) invocation.getTarget();
    Field parameterField = parameterHandler.getClass().getDeclaredField("parameterObject");
    parameterField.setAccessible(true);

    Object parameterObject = parameterField.get(parameterHandler);

    if (ObjectUtils.isEmpty(parameterObject)) {
      return;
    }

    if (parameterObject instanceof Map) {
      Map<String, Object> parameterMap = (Map<String, Object>) parameterObject;

      if (parameterMap.containsKey("list")) {
        List<Object> collections = (List<Object>) parameterMap.get("list");
        encryptListParallel(collections);
      }
    } else {
      encryptObject(parameterObject); // 단일 객체의 경우 기존 코드와 동일하게 암호화
    }
  }

  /**
   * 배치 파라미터(list) 의 원소별 암호화를 병렬로 수행한다.
   * <p>
   * 병렬을 유지하는 근거: AESUtil 은 값마다 랜덤 salt 를 생성하므로 EncryptedField 하나당
   * PBKDF2(8192 iterations) 키 유도가 매번 새로 일어난다(실측 1ms+/건). 컨텍스트 스냅샷 캡처·복원 비용은
   * 그보다 세 자릿수 저렴해서 오버헤드로 문제되지 않는다.
   * <p>
   * executor 미지정 {@code CompletableFuture.runAsync} 대신 {@link TraceExecutors} 를 쓰는 이유:
   * ① ForkJoinPool.commonPool 은 traceId 를 물려받지 못해 여기서 찍히는 오류 로그가 요청과 끊긴다
   * ② commonPool 은 JVM 전역 자원이라 수 ms 단위 CPU 작업을 몰아넣으면 다른 사용자를 굶긴다
   * ③ vCPU 1개 컨테이너에서는 commonPool parallelism 이 0이 되어 원소마다 플랫폼 스레드가 새로 생긴다
   */
  private void encryptListParallel(List<Object> collections) {
    // try-with-resources 의 close() 가 shutdown + awaitTermination 이라 전 원소 완료까지 대기한다
    try (ExecutorService pool = TraceExecutors.virtualThreadPerTask("crypto-encrypt-")) {
      for (Object inputParam : collections) {
        pool.submit(() -> {
          try {
            encryptObject(inputParam);
          } catch (Exception e) {
            log.error("encrypt error", e);
          }
        });
      }
    }
  }

  private void encryptObject(Object parameterObject) throws Exception {
    Class<?> entityClass = parameterObject.getClass();
    if (!entityClass.isAnnotationPresent(SecretData.class)) {
      return;
    }

    Field[] fields = entityClass.getDeclaredFields();
    for (Field field : fields) {
      if (field.isAnnotationPresent(EncryptedField.class)) {
        field.setAccessible(true);
        if (field.getType().equals(String.class)) {
          String value = (String) field.get(parameterObject);
          String encryptedValue = encryptor.encrypt(value);
          field.set(parameterObject, encryptedValue);
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