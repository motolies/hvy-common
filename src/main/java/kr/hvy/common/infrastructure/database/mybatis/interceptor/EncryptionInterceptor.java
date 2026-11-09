package kr.hvy.common.infrastructure.database.mybatis.interceptor;

import io.micrometer.core.instrument.MeterRegistry;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import kr.hvy.common.application.crypto.EncryptionException;
import kr.hvy.common.application.crypto.Encryptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

/**
 * 쓰기(암호화) 인터셉터. {@code ParameterHandler.setParameters} 직전에 {@code @SecretData} 파라미터의
 * {@code @EncryptedField} 를 암호화하고, 바인딩이 끝나면 <b>원래 평문으로 복원</b>한다(encrypt → bind → restore).
 *
 * <p>복원하는 이유: 바인딩은 {@code PreparedStatement.setString} 으로 값을 복사하므로 복원해도 암호문이 실행된다.
 * 대신 호출자의 DTO 가 평문 상태로 돌아가므로 같은 객체를 다시 바인딩해도 이중 암호화되지 않고,
 * 저장 후 DTO 를 재사용해도 암호문이 노출되지 않는다. 암호문에 마커가 없으므로 이중 암호화 방지는 전적으로 이 복원에 의존한다.
 *
 * <p>fail-closed: 암호화 중 예외가 나면 그대로 전파해 평문이 바인딩되지 않게 한다(이미 바꾼 필드는 복원).
 *
 * <p>순차 처리: 필드 순회는 호출 스레드에서 한다. 복원 스냅샷을 단일 스레드에서 다뤄야 안전하고,
 * {@code AESGcmUtil} 은 필드당 µs 단위라 병렬화 비용이 더 크다.
 */
@Slf4j
@Intercepts({
    @Signature(type = ParameterHandler.class, method = "setParameters", args = PreparedStatement.class),
})
public class EncryptionInterceptor implements Interceptor {

  static final String FAILURE_METRIC = "hvy.crypto.encrypt.failure";

  private final Encryptor encryptor;
  /** 메트릭 레지스트리(지연 해석). 없으면 카운트하지 않는다. */
  private final ObjectProvider<MeterRegistry> meterRegistryProvider;

  public EncryptionInterceptor(Encryptor encryptor) {
    this(encryptor, null);
  }

  /**
   * @param meterRegistryProvider 실패 카운터용. {@code ObjectProvider} 로 지연 해석해 SqlSessionFactory 초기화 순서 문제를 피한다
   */
  public EncryptionInterceptor(Encryptor encryptor, ObjectProvider<MeterRegistry> meterRegistryProvider) {
    this.encryptor = encryptor;
    this.meterRegistryProvider = meterRegistryProvider;
  }

  /** 바인딩 전 암호화 → 바인딩 → 원복. 예외가 나도 finally 에서 반드시 원복한다. */
  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    Object parameterObject = ((ParameterHandler) invocation.getTarget()).getParameterObject();
    List<FieldSnapshot> snapshots = new ArrayList<>();
    try {
      encryptInPlace(parameterObject, snapshots);
      return invocation.proceed();
    } finally {
      restore(snapshots);
    }
  }

  /**
   * 파라미터 객체를 순회해 {@code @SecretData} 객체의 암호화 필드를 in-place 암호화한다.
   * MyBatis 는 List 파라미터를 {@code collection}/{@code list}/실제 파라미터명 등 여러 키로 같은 객체를 넣으므로
   * identity 기준으로 중복을 제거한다.
   */
  private void encryptInPlace(Object parameterObject, List<FieldSnapshot> snapshots) {
    Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    visit(parameterObject, visited, snapshots);
  }

  /** Map/Collection/배열은 요소를 재귀 방문하고, 그 외 객체는 암호화 대상이면 필드를 처리한다. */
  private void visit(Object target, Set<Object> visited, List<FieldSnapshot> snapshots) {
    if (target == null || !visited.add(target)) {
      return;
    }
    if (target instanceof Map<?, ?> map) {
      for (Object value : map.values()) {
        visit(value, visited, snapshots);
      }
      return;
    }
    if (target instanceof Collection<?> collection) {
      for (Object value : collection) {
        visit(value, visited, snapshots);
      }
      return;
    }
    if (target instanceof Object[] array) {
      for (Object value : array) {
        visit(value, visited, snapshots);
      }
      return;
    }
    for (Field field : SecretFieldResolver.encryptedFields(target.getClass())) {
      encryptField(target, field, snapshots);
    }
  }

  /** 필드 하나를 암호화한다. 원본 값을 먼저 스냅샷에 기록해 실패해도 복원되도록 한다. */
  private void encryptField(Object target, Field field, List<FieldSnapshot> snapshots) {
    try {
      String value = (String) field.get(target);
      if (!StringUtils.hasText(value)) {
        return;
      }
      snapshots.add(new FieldSnapshot(target, field, value));
      field.set(target, encryptor.encrypt(value));
    } catch (RuntimeException e) {
      countFailure(target.getClass(), field);
      throw e;
    } catch (IllegalAccessException e) {
      countFailure(target.getClass(), field);
      throw new EncryptionException(
          "암호화 필드 접근 실패: " + target.getClass().getName() + "." + field.getName(), e);
    }
  }

  /** 스냅샷을 역순으로 되돌려 파라미터 객체를 평문 상태로 복원한다. */
  private void restore(List<FieldSnapshot> snapshots) {
    for (int i = snapshots.size() - 1; i >= 0; i--) {
      FieldSnapshot snapshot = snapshots.get(i);
      try {
        snapshot.field().set(snapshot.target(), snapshot.original());
      } catch (IllegalAccessException e) {
        log.error("[crypto] 암호화 필드 복원 실패: {}.{}",
            snapshot.target().getClass().getName(), snapshot.field().getName(), e);
      }
    }
  }

  /** 암호화 실패 카운터를 증가시킨다. 레지스트리가 없으면 무시한다. */
  private void countFailure(Class<?> type, Field field) {
    MeterRegistry registry = meterRegistryProvider == null ? null : meterRegistryProvider.getIfAvailable();
    if (registry != null) {
      registry.counter(FAILURE_METRIC, "class", type.getSimpleName(), "field", field.getName()).increment();
    }
  }

  @Override
  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  @Override
  public void setProperties(Properties properties) {
  }

  /** 암호화 전 원본 값 기록(복원용) */
  private record FieldSnapshot(Object target, Field field, String original) {

  }
}
