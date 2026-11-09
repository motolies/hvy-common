package kr.hvy.common.infrastructure.database.mybatis.interceptor;

import io.micrometer.core.instrument.MeterRegistry;
import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import kr.hvy.common.application.crypto.CryptoException;
import kr.hvy.common.application.crypto.DecryptionException;
import kr.hvy.common.application.crypto.Decryptor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 읽기(복호화) 인터셉터. {@code ResultSetHandler.handleResultSets} 결과 리스트의 {@code @SecretData} 행에서
 * {@code @EncryptedField} 를 복호화한다.
 *
 * <p>MyBatis 는 결과를 항상 List 로 반환하며, 스칼라(resultType=string/int) 조회의 NULL 값은 {@code [null]} 처럼
 * null 요소로 들어온다. 첫 <b>non-null</b> 요소로 대상 여부를 판정하고 null 요소는 건너뛴다.
 *
 * <p>암호문에 마커가 없으므로 비어 있지 않은 필드 값은 모두 암호문으로 간주한다. 복호화 실패({@link DecryptionException})는
 * 카운터를 올린 뒤 그대로 전파한다. 평문이 섞인 테이블(전환 미완료)이나 키 오설정은 조회 실패로 드러난다.
 */
@Intercepts({
    @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})
})
public class DecryptionInterceptor implements Interceptor {

  static final String FAILURE_METRIC = "hvy.crypto.decrypt.failure";

  private final Decryptor decryptor;
  /** 메트릭 레지스트리(지연 해석). 없으면 카운트하지 않는다. */
  private final ObjectProvider<MeterRegistry> meterRegistryProvider;

  public DecryptionInterceptor(Decryptor decryptor) {
    this(decryptor, null);
  }

  /**
   * @param meterRegistryProvider 실패 카운터용. {@code ObjectProvider} 로 지연 해석해 SqlSessionFactory 초기화 순서 문제를 피한다
   */
  public DecryptionInterceptor(Decryptor decryptor, ObjectProvider<MeterRegistry> meterRegistryProvider) {
    this.decryptor = decryptor;
    this.meterRegistryProvider = meterRegistryProvider;
  }

  /** 결과 리스트가 암호화 대상 타입이면 행마다 복호화한다. */
  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    Object result = invocation.proceed();
    if (!(result instanceof List<?> rows)) {
      return result;
    }
    Object first = firstNonNull(rows);
    if (first == null || !SecretFieldResolver.isSecretData(first.getClass())) {
      return result; // 비대상 결과(스칼라·Map·다중 결과셋 등)는 즉시 반환
    }
    for (Object row : rows) {
      if (row != null) {
        decryptFields(row);
      }
    }
    return result;
  }

  /** 리스트의 첫 non-null 요소. 전부 null 이면 null. */
  private static Object firstNonNull(List<?> rows) {
    for (Object row : rows) {
      if (row != null) {
        return row;
      }
    }
    return null;
  }

  /** 행 하나의 암호화 필드를 복호화한다. 행 타입별로 캐시된 필드 목록을 쓴다(discriminator 등 이종 타입 대비). */
  private void decryptFields(Object row) {
    for (Field field : SecretFieldResolver.encryptedFields(row.getClass())) {
      try {
        String value = (String) field.get(row);
        if (value == null) {
          continue;
        }
        field.set(row, decryptor.decrypt(value));
      } catch (CryptoException e) {
        countFailure(row.getClass(), field);
        throw e;
      } catch (IllegalAccessException e) {
        countFailure(row.getClass(), field);
        throw new DecryptionException(
            "복호화 필드 접근 실패: " + row.getClass().getName() + "." + field.getName(), e);
      }
    }
  }

  /** 복호화 실패 카운터를 증가시킨다. 레지스트리가 없으면 무시한다. */
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
}
