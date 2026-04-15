package kr.hvy.common.infrastructure.redis.impl.masterdata.query;

import java.util.List;
import kr.hvy.common.infrastructure.redis.impl.masterdata.dto.MasterCodeResponse;
import kr.hvy.common.infrastructure.redis.impl.masterdata.dto.MasterCodeTreeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * blog-back REST API 를 호출하여 마스터코드를 조회하는 {@link MasterCodeLoader} 구현.
 * <p>
 * 소비 앱은 이 빈을 그대로 등록하고 {@link MasterCodeClientProperties} 만 설정하면 된다.
 * 엔드포인트 규약(blog-back {@code MasterCodeController}):
 * <ul>
 *   <li>GET {base}/api/v1/codes/tree</li>
 *   <li>GET {base}/api/v1/codes/tree/{rootCode}</li>
 *   <li>GET {base}/api/v1/codes/tree/{rootCode}/flat</li>
 * </ul>
 * <p>
 * 실패 시 예외를 그대로 전파한다. 회로차단/재시도는 소비 앱에서 필요 시 상위에 추가.
 */
@Slf4j
public class RestClientMasterCodeLoader implements MasterCodeLoader {

  private static final ParameterizedTypeReference<List<MasterCodeTreeResponse>> TREE_LIST =
      new ParameterizedTypeReference<>() {
      };

  private static final ParameterizedTypeReference<List<MasterCodeResponse>> RESPONSE_LIST =
      new ParameterizedTypeReference<>() {
      };

  private final RestClient restClient;

  public RestClientMasterCodeLoader(MasterCodeClientProperties properties) {
    if (properties == null || !StringUtils.hasText(properties.baseUrl())) {
      throw new IllegalStateException(
          "hvy.masterdata.client.base-url 이 비어 있습니다. RestClientMasterCodeLoader 는 이 속성을 필요로 합니다.");
    }
    RestClient.Builder builder = RestClient.builder().baseUrl(properties.baseUrl());
    if (StringUtils.hasText(properties.apiKey())) {
      builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey());
    }
    this.restClient = builder.build();
    log.info("RestClientMasterCodeLoader 초기화: baseUrl={}", properties.baseUrl());
  }

  @Override
  public List<MasterCodeTreeResponse> loadFullTree() {
    return restClient.get()
        .uri("/api/v1/codes/tree")
        .retrieve()
        .body(TREE_LIST);
  }

  @Override
  public List<MasterCodeTreeResponse> loadSubTree(String rootCode) {
    return restClient.get()
        .uri("/api/v1/codes/tree/{rootCode}", rootCode)
        .retrieve()
        .body(TREE_LIST);
  }

  @Override
  public List<MasterCodeResponse> loadChildren(String rootCode) {
    return restClient.get()
        .uri("/api/v1/codes/tree/{rootCode}/flat", rootCode)
        .retrieve()
        .body(RESPONSE_LIST);
  }
}
