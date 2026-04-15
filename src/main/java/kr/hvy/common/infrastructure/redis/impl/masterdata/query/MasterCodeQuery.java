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
   */
  public List<MasterCodeTreeResponse> getSubTree(String rootCode) {
    List<MasterCodeTreeResponse> cached = cache.getSubTree(rootCode);
    if (cached != null) {
      return cached;
    }
    List<MasterCodeTreeResponse> loaded = loader.loadSubTree(rootCode);
    if (loaded != null) {
      cache.putSubTree(rootCode, loaded);
    }
    return loaded;
  }

  /**
   * 루트의 직계 자식 플랫 목록 조회.
   */
  public List<MasterCodeResponse> getChildren(String rootCode) {
    List<MasterCodeResponse> cached = cache.getChildren(rootCode);
    if (cached != null) {
      return cached;
    }
    List<MasterCodeResponse> loaded = loader.loadChildren(rootCode);
    if (loaded != null) {
      cache.putChildren(rootCode, loaded);
    }
    return loaded;
  }
}
