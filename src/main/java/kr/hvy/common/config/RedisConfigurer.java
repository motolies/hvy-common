package kr.hvy.common.config;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;

@RequiredArgsConstructor
public class RedisConfigurer {

  private final RedisProperties redisProperties;

  @Bean(destroyMethod = "shutdown")
  public RedissonClient redissonClient() {
    Config config = new Config();

    // todo : 변경했을 때 어떻게 되는지 확인 필요
    final Codec codec = new CustomJsonJacksonCodec(ObjectMapperConfigurer.getRedisObjectMapper());
    config.setCodec(codec);

    String redisAddress = String.format("redis://%s:%s", redisProperties.getHost(), redisProperties.getPort());

    if ("cluster".equals(redisProperties.getClientName())) {
      config.useClusterServers().addNodeAddress(redisAddress);
    } else {
      config.useSingleServer().setAddress(redisAddress);
    }

    return Redisson.create(config);
  }



}
