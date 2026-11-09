package kr.hvy.common.infrastructure.database.mybatis.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import kr.hvy.common.application.crypto.EncryptionException;
import kr.hvy.common.application.crypto.Encryptor;
import kr.hvy.common.application.crypto.util.AESGcmUtil;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * MyBatis 파이프라인을 흉내 내어(Invocation + ParameterHandler mock) intercept() 를 직접 호출한다.
 * private 메서드를 리플렉션으로 호출하지 않는다.
 */
@DisplayName("EncryptionInterceptor - encrypt → bind → restore")
class EncryptionInterceptorTest {

  private static AESGcmUtil aesGcmUtil;
  private static Method setParameters;

  @BeforeAll
  static void setUp() throws Exception {
    aesGcmUtil = new AESGcmUtil("unit-test-gcm-key-0123456789-abcdefghijklmnop", "unit-test-kdf-salt");
    setParameters = ParameterHandler.class.getMethod("setParameters", PreparedStatement.class);
  }

  /** 파라미터 객체를 돌려주는 ParameterHandler mock 으로 Invocation 을 만든다. */
  private static Invocation invocationFor(ParameterHandler handler) {
    return new Invocation(handler, setParameters, new Object[]{mock(PreparedStatement.class)});
  }

  private static ParameterHandler handlerReturning(Object parameterObject) {
    ParameterHandler handler = mock(ParameterHandler.class);
    when(handler.getParameterObject()).thenReturn(parameterObject);
    return handler;
  }

  @Test
  @DisplayName("바인딩 시점에는 암호문이고, 바인딩 후에는 원래 평문으로 복원된다")
  void encryptsDuringBindAndRestoresAfter() throws Throwable {
    TestPerson dto = new TestPerson("ID-1", "홍길동", "010-1234-5678");
    ParameterHandler handler = handlerReturning(dto);
    AtomicReference<String> boundName = new AtomicReference<>();
    AtomicReference<String> boundTel = new AtomicReference<>();
    doAnswer(inv -> {
      boundName.set(dto.name);
      boundTel.set(dto.tel);
      return null;
    }).when(handler).setParameters(any());

    new EncryptionInterceptor(aesGcmUtil).intercept(invocationFor(handler));

    assertThat(boundName.get()).isNotEqualTo("홍길동");
    assertThat(aesGcmUtil.decrypt(boundName.get())).isEqualTo("홍길동");
    assertThat(aesGcmUtil.decrypt(boundTel.get())).isEqualTo("010-1234-5678");
    assertThat(dto.name).isEqualTo("홍길동"); // 복원
    assertThat(dto.tel).isEqualTo("010-1234-5678");
    assertThat(dto.id).isEqualTo("ID-1"); // 비대상 필드 불변
  }

  @Test
  @DisplayName("같은 DTO 를 두 번 바인딩해도 매번 평문에서 출발한다(이중 암호화 없음)")
  void rebindingSameDtoDoesNotDoubleEncrypt() throws Throwable {
    TestPerson dto = new TestPerson("ID-1", "홍길동", null);
    EncryptionInterceptor interceptor = new EncryptionInterceptor(aesGcmUtil);
    for (int i = 0; i < 2; i++) {
      ParameterHandler handler = handlerReturning(dto);
      AtomicReference<String> bound = new AtomicReference<>();
      doAnswer(inv -> {
        bound.set(dto.name);
        return null;
      }).when(handler).setParameters(any());

      interceptor.intercept(invocationFor(handler));

      assertThat(aesGcmUtil.decrypt(bound.get())).isEqualTo("홍길동");
    }
  }

  @Test
  @DisplayName("같은 List 가 collection/list/파라미터명 세 키로 들어와도 필드당 1회만 암호화한다")
  void listParamUnderMultipleKeysEncryptsOnce() throws Throwable {
    TestPerson first = new TestPerson("ID-1", "김철수", "010-1111-1111");
    TestPerson second = new TestPerson("ID-2", "이영희", "010-2222-2222");
    List<TestPerson> list = List.of(first, second);
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("collection", list);
    paramMap.put("list", list);
    paramMap.put("personList", list);
    Encryptor counting = mock(Encryptor.class);
    when(counting.encrypt(anyString())).thenAnswer(inv -> "ENC(" + inv.getArgument(0) + ")");
    ParameterHandler handler = handlerReturning(paramMap);

    new EncryptionInterceptor(counting).intercept(invocationFor(handler));

    verify(counting, times(4)).encrypt(anyString()); // 2 객체 × 채워진 필드 2개
    verify(handler).setParameters(any());
    assertThat(first.tel).isEqualTo("010-1111-1111"); // 복원
    assertThat(second.name).isEqualTo("이영희");
  }

  @Test
  @DisplayName("암호화 도중 실패하면 바인딩하지 않고 예외를 전파하며, 이미 바꾼 필드는 복원하고 카운터를 올린다(fail-closed)")
  void failClosedOnEncryptError() throws SQLException {
    TestPerson dto = new TestPerson("ID-1", "홍길동", "010-1234-5678");
    Encryptor failing = mock(Encryptor.class);
    when(failing.encrypt(anyString()))
        .thenReturn("ENC(first)")
        .thenThrow(new EncryptionException("boom", new RuntimeException()));
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    ParameterHandler handler = handlerReturning(dto);
    EncryptionInterceptor interceptor = new EncryptionInterceptor(failing,
        new StaticListableBeanFactory(Map.of("meterRegistry", registry)).getBeanProvider(MeterRegistry.class));

    assertThatThrownBy(() -> interceptor.intercept(invocationFor(handler)))
        .isInstanceOf(EncryptionException.class);

    verify(handler, never()).setParameters(any()); // 평문 바인딩 없음
    assertThat(dto.name).isEqualTo("홍길동");
    assertThat(dto.tel).isEqualTo("010-1234-5678");
    assertThat(registry.get(EncryptionInterceptor.FAILURE_METRIC)
        .tag("class", "TestPerson").tag("field", "tel").counter().count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("바인딩(proceed) 자체가 실패해도 파라미터 객체는 평문으로 복원된다")
  void restoresEvenIfProceedThrows() throws SQLException {
    TestPerson dto = new TestPerson("ID-1", null, "010-1234-5678");
    ParameterHandler handler = handlerReturning(dto);
    doThrow(new SQLException("bind failed")).when(handler).setParameters(any());
    EncryptionInterceptor interceptor = new EncryptionInterceptor(aesGcmUtil);

    assertThatThrownBy(() -> interceptor.intercept(invocationFor(handler)))
        .isInstanceOf(InvocationTargetException.class)
        .hasCauseInstanceOf(SQLException.class);

    assertThat(dto.tel).isEqualTo("010-1234-5678");
  }

  @Test
  @DisplayName("@SecretData 가 아닌 파라미터(문자열·일반 Map·null)는 건드리지 않는다")
  void nonSecretParamUntouched() throws Throwable {
    Encryptor encryptor = mock(Encryptor.class);
    EncryptionInterceptor interceptor = new EncryptionInterceptor(encryptor);

    interceptor.intercept(invocationFor(handlerReturning("plain")));
    interceptor.intercept(invocationFor(handlerReturning(Map.of("id", "ID-1", "ids", List.of(1, 2)))));
    interceptor.intercept(invocationFor(handlerReturning(null)));

    verify(encryptor, never()).encrypt(anyString());
  }
}
