package kr.hvy.common.config;

import java.util.List;
import kr.hvy.common.mapper.ObjectMapperConfigurer;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;

@RequiredArgsConstructor
public class RedisConfigurer {

  private final RedisProperties redisProperties;

  @Bean
  public RedissonClient redissonClient() {
    Config config = new Config();
//    final Codec codec = new StringCodec();

    // todo : 변경했을 때 어떻게 되는지 확인 필요
    final Codec codec = new CustomJsonJacksonCodec(ObjectMapperConfigurer.getRedisObjectMapper());
    config.setCodec(codec);

    if (redisProperties.getCluster() != null) {
      // 클러스터 모드 설정
      config.useClusterServers()
          .setPassword(StringUtils.isBlank(redisProperties.getPassword()) ? null : redisProperties.getPassword())
          .setScanInterval(2000) // 클러스터 노드 스캔 주기
          .setConnectTimeout(10000)
          .setTimeout(3000)
          .setRetryAttempts(3)
          .setRetryInterval(1500)
          .setMasterConnectionPoolSize(20)
          .setMasterConnectionMinimumIdleSize(5)
          .setSlaveConnectionPoolSize(20)
          .setSlaveConnectionMinimumIdleSize(5);

      List<String> clusterNodes = redisProperties.getCluster().getNodes();
      for (String node : clusterNodes) {
        config.useClusterServers().addNodeAddress(node);
      }

    } else {
      config.useSingleServer()
          .setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
          .setPassword(StringUtils.isBlank(redisProperties.getPassword()) ? null : redisProperties.getPassword())
          .setConnectionPoolSize(20)
          .setConnectionMinimumIdleSize(5);
    }
    return Redisson.create(config);
  }

}
