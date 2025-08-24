package kr.hvy.common.config.tsid;

import io.hypersistence.tsid.TSID;
import io.hypersistence.tsid.TSID.Factory;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TsidConfig {

  @Bean
  @Primary
  public Supplier<TSID.Factory> tsidFactoryProvider() {
    // TSID.Factory 인스턴스를 생성하여 반환합니다.
    // 256비트 노드 ID를 사용하는 Factory 인스턴스를 생성하여 애플리케이션 전체에서
    // 고유한 시간 기반 ID를 생성할 수 있도록 합니다.
    return Factory::newInstance256;
  }
}
