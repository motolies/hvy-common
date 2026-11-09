package kr.hvy.common.infrastructure.database.mybatis.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import kr.hvy.common.application.crypto.DecryptionException;
import kr.hvy.common.application.crypto.util.AESGcmUtil;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * ResultSetHandler mock 이 반환하는 결과 리스트를 Invocation 으로 감싸 intercept() 를 직접 호출한다.
 */
@DisplayName("DecryptionInterceptor - 결과 행 복호화")
class DecryptionInterceptorTest {

  private static AESGcmUtil aesGcmUtil;
  private static Method handleResultSets;

  @BeforeAll
  static void setUp() throws Exception {
    aesGcmUtil = new AESGcmUtil("unit-test-gcm-key-0123456789-abcdefghijklmnop", "unit-test-kdf-salt");
    handleResultSets = ResultSetHandler.class.getMethod("handleResultSets", Statement.class);
  }

  /** handleResultSets 가 주어진 결과를 돌려주는 Invocation 을 만든다. */
  private static Invocation invocationReturning(List<Object> result) throws Exception {
    ResultSetHandler handler = mock(ResultSetHandler.class);
    when(handler.<Object>handleResultSets(any())).thenReturn(result);
    return new Invocation(handler, handleResultSets, new Object[]{mock(Statement.class)});
  }

  private static TestPerson encryptedRow(String name, String tel) {
    return new TestPerson("ID-1", aesGcmUtil.encrypt(name), aesGcmUtil.encrypt(tel));
  }

  @Test
  @DisplayName("[회귀] 스칼라 NULL 결과([null])에서 NPE 없이 그대로 반환한다")
  void nullOnlyListNoNpe() throws Throwable {
    List<Object> result = Arrays.asList((Object) null);

    Object returned = new DecryptionInterceptor(aesGcmUtil).intercept(invocationReturning(result));

    assertThat(returned).isSameAs(result);
  }

  @Test
  @DisplayName("null 요소와 null 필드가 섞여 있어도 나머지 값은 복호화된다")
  void nullElementsAndFieldsSkipped() throws Throwable {
    TestPerson row = encryptedRow("홍길동", "010-1234-5678");
    TestPerson nullField = new TestPerson("ID-2", aesGcmUtil.encrypt("김철수"), null);
    List<Object> result = Arrays.asList(null, row, null, nullField);

    new DecryptionInterceptor(aesGcmUtil).intercept(invocationReturning(result));

    assertThat(row.name).isEqualTo("홍길동");
    assertThat(row.tel).isEqualTo("010-1234-5678");
    assertThat(nullField.name).isEqualTo("김철수");
    assertThat(nullField.tel).isNull();
  }

  @Test
  @DisplayName("@SecretData 행 리스트의 암호화 필드를 모두 복호화하고 비대상 필드는 유지한다")
  void decryptsSecretRows() throws Throwable {
    TestPerson first = encryptedRow("홍길동", "010-1111-1111");
    TestPerson second = encryptedRow("김철수", "010-2222-2222");

    new DecryptionInterceptor(aesGcmUtil).intercept(invocationReturning(List.of(first, second)));

    assertThat(first.name).isEqualTo("홍길동");
    assertThat(first.tel).isEqualTo("010-1111-1111");
    assertThat(second.name).isEqualTo("김철수");
    assertThat(second.id).isEqualTo("ID-1");
  }

  @Test
  @DisplayName("마커가 없으므로 평문 행은 통과시키지 않고 예외를 전파하며 실패 카운터를 올린다")
  void plaintextRowThrowsAndCounts() throws Exception {
    TestPerson row = new TestPerson("ID-1", "홍길동", null);
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    DecryptionInterceptor interceptor = new DecryptionInterceptor(aesGcmUtil,
        new StaticListableBeanFactory(Map.of("meterRegistry", registry)).getBeanProvider(MeterRegistry.class));
    Invocation invocation = invocationReturning(List.of(row));

    assertThatThrownBy(() -> interceptor.intercept(invocation)).isInstanceOf(DecryptionException.class);

    assertThat(registry.get(DecryptionInterceptor.FAILURE_METRIC)
        .tag("class", "TestPerson").tag("field", "name").counter().count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("다른 키로 만든 암호문은 암호문 문자열을 반환하지 않고 예외를 전파한다")
  void wrongKeyThrows() throws Exception {
    AESGcmUtil other = new AESGcmUtil("another-unit-test-key-9876543210-zyxwvutsrq", "unit-test-kdf-salt");
    TestPerson row = new TestPerson("ID-1", other.encrypt("홍길동"), null);
    Invocation invocation = invocationReturning(List.of(row));

    assertThatThrownBy(() -> new DecryptionInterceptor(aesGcmUtil).intercept(invocation))
        .isInstanceOf(DecryptionException.class);
  }

  @Test
  @DisplayName("비대상 결과(Map 리스트·문자열 리스트·다중 결과셋·리스트 아님)는 건드리지 않는다")
  void nonSecretResultsUntouched() throws Throwable {
    DecryptionInterceptor interceptor = new DecryptionInterceptor(aesGcmUtil);
    String cipherText = aesGcmUtil.encrypt("not-touched");
    List<Object> maps = List.of(Map.of("name", cipherText));
    List<Object> strings = List.of(cipherText);
    List<Object> nested = List.of(List.of(cipherText));

    assertThat(interceptor.intercept(invocationReturning(maps))).isSameAs(maps);
    assertThat(interceptor.intercept(invocationReturning(strings))).isSameAs(strings);
    assertThat(interceptor.intercept(invocationReturning(nested))).isSameAs(nested);
    assertThat(((Map<?, ?>) maps.get(0)).get("name")).isEqualTo(cipherText);

    Invocation scalar = mock(Invocation.class);
    when(scalar.proceed()).thenReturn("scalar");
    assertThat(interceptor.intercept(scalar)).isEqualTo("scalar");
  }
}
