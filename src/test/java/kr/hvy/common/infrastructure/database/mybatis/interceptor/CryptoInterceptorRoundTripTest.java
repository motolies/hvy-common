package kr.hvy.common.infrastructure.database.mybatis.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;
import kr.hvy.common.application.crypto.Decryptor;
import kr.hvy.common.application.crypto.Encryptor;
import kr.hvy.common.application.crypto.util.AESCbcUtil;
import kr.hvy.common.application.crypto.util.AESGcmUtil;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 쓰기 인터셉터가 바인딩한 값을 그대로 "DB 에서 읽은 행"으로 만들어 읽기 인터셉터에 통과시킨다.
 * CBC·GCM 어느 쪽을 끼워도 인터셉터 쌍이 같은 계약으로 동작하는지 확인한다.
 */
@DisplayName("암복호화 인터셉터 왕복 - CBC/GCM 공통")
class CryptoInterceptorRoundTripTest {

  static Stream<Arguments> cryptos() {
    AESCbcUtil cbc = new AESCbcUtil("legacy-cbc-test-key");
    AESGcmUtil gcm = new AESGcmUtil("unit-test-gcm-key-0123456789-abcdefghijklmnop", "unit-test-kdf-salt");
    return Stream.of(
        Arguments.of("CBC", cbc, cbc),
        Arguments.of("GCM", gcm, gcm));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cryptos")
  @DisplayName("저장 시 바인딩된 암호문을 조회 결과로 돌려주면 평문으로 복원된다")
  void writeThenReadRoundTrip(String mode, Encryptor encryptor, Decryptor decryptor) throws Throwable {
    TestPerson saved = new TestPerson("ID-1", "홍길동", "010-1234-5678");
    TestPerson stored = new TestPerson(null, null, null);

    // 쓰기: 바인딩 시점의 필드 값을 "DB 에 저장된 값"으로 복사한다
    ParameterHandler parameterHandler = mock(ParameterHandler.class);
    when(parameterHandler.getParameterObject()).thenReturn(saved);
    doAnswer(inv -> {
      stored.id = saved.id;
      stored.name = saved.name;
      stored.tel = saved.tel;
      return null;
    }).when(parameterHandler).setParameters(any());
    new EncryptionInterceptor(encryptor).intercept(new Invocation(parameterHandler,
        ParameterHandler.class.getMethod("setParameters", PreparedStatement.class),
        new Object[]{mock(PreparedStatement.class)}));

    assertThat(stored.name).isNotEqualTo("홍길동");
    assertThat(stored.tel).isNotEqualTo("010-1234-5678");

    // 읽기: 저장된 행을 결과로 돌려준다
    ResultSetHandler resultSetHandler = mock(ResultSetHandler.class);
    when(resultSetHandler.<Object>handleResultSets(any())).thenReturn(List.of(stored));
    new DecryptionInterceptor(decryptor).intercept(new Invocation(resultSetHandler,
        ResultSetHandler.class.getMethod("handleResultSets", Statement.class),
        new Object[]{mock(Statement.class)}));

    assertThat(stored.name).isEqualTo("홍길동");
    assertThat(stored.tel).isEqualTo("010-1234-5678");
    assertThat(stored.id).isEqualTo("ID-1");
  }
}
