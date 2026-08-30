package kr.hvy.common.infrastructure.redis.impl.masterdata.query;

import java.util.List;
import kr.hvy.common.infrastructure.redis.impl.masterdata.cache.MasterCodeCacheService;
import kr.hvy.common.infrastructure.redis.impl.masterdata.dto.MasterCodeResponse;
import kr.hvy.common.infrastructure.redis.impl.masterdata.dto.MasterCodeTreeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * 마스터코드 조회 Facade.
 * <p>
 * <b>blog-back 과 소비 앱 공용 엔트리포인트.</b> 차이는 주입되는 {@link MasterCodeLoader} 구현체 뿐.
 * <ul>
 *   <li>blog-back → {@code JpaMasterCodeLoader} (DB 직결)</li>
 *   <li>소비 앱 → {@link RestClientMasterCodeLoader} (blog-back REST 호출)</li>
 * </ul>
 * <p>
 * 조회 순서: L1(Caffeine) → L2(Redis) → loader(DB/REST) → L1+L2 시드 → 반환.
 * 쓰기 후 무효화는 {@link MasterCodeCacheService#evictByRootCode(String)} 를 직접 호출하면
 * TwoTierCache 가 Pub/Sub 으로 다른 인스턴스의 L1 을 자동 drop 한다.
 */
@Slf4j
@RequiredArgsConstructor
public class MasterCodeQuery {

  private final MasterCodeCacheService cache;
  private final MasterCodeLoader loader;

  /**
   * 전체 활성 트리 조회.
   */
  public List<MasterCodeTreeResponse> getFullTree() {
    List<MasterCodeTreeResponse> cached = cache.getFullTree();
    if (!CollectionUtils.isEmpty(cached)) {
      return cached;
    }
    List<MasterCodeTreeResponse> loaded = loader.loadFullTree();
    if (loaded != null) {
      cache.putFullTree(loaded);
    }
    return loaded;
  }

  /**
   * 루트 코드별 서브트리 조회.
   * <p>
   * ⚠️ 빈 결과는 캐시 히트로 보지도, 캐시에 쓰지도 않는다({@link #getFullTree()} 와 같은 규약).
   * 예전에는 {@code cached != null} 로 판정해 빈 리스트도 히트였는데, 그 때문에 데이터가 있는데도
   * 일시적으로 0건이 나온 경우(예: 시드 시 path 백필 누락)의 {@code []} 가 L2 TTL(1일) 동안
   * 눌러앉아 "DB 를 고쳤는데도 화면이 그대로"인 상황을 만들었다. 빈 서브트리를 매번 다시 읽는
   * 비용보다 잘못된 빈 값이 하루 고착되는 위험이 훨씬 크다.
   */
  public List<MasterCodeTreeResponse> getSubTree(String rootCode) {
    List<MasterCodeTreeResponse> cached = cache.getSubTree(rootCode);
    if (!CollectionUtils.isEmpty(cached)) {
      return cached;
    }
    List<MasterCodeTreeResponse> loaded = loader.loadSubTree(rootCode);
    if (!CollectionUtils.isEmpty(loaded)) {
      cache.putSubTree(rootCode, loaded);
    }
    return loaded;
  }

  /**
   * 루트의 직계 자식 플랫 목록 조회.
   * <p>
   * 빈 결과 처리 규약은 {@link #getSubTree(String)} 와 같다.
   */
  public List<MasterCodeResponse> getChildren(String rootCode) {
    List<MasterCodeResponse> cached = cache.getChildren(rootCode);
    if (!CollectionUtils.isEmpty(cached)) {
      return cached;
    }
    List<MasterCodeResponse> loaded = loader.loadChildren(rootCode);
    if (!CollectionUtils.isEmpty(loaded)) {
      cache.putChildren(rootCode, loaded);
    }
    return loaded;
  }
}
