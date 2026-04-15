package kr.hvy.common.infrastructure.redis.impl.masterdata.cache;

import java.util.List;
import kr.hvy.common.infrastructure.redis.impl.masterdata.dto.MasterCodeResponse;
import kr.hvy.common.infrastructure.redis.impl.masterdata.dto.MasterCodeTreeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * 마스터코드 캐시 키 규약 래퍼.
 * <p>
 * Spring {@link CacheManager} 위에 얇게 얹혀 TREE/CHILDREN 캐시에 대한 도메인 키 규약을 캡슐화한다.
 * 내부 저장소는 {@code TwoTierCache}(L1 Caffeine + L2 Redis) 이며, {@code evict} 호출은
 * TwoTierCache 내부에서 Redis Pub/Sub(CacheInvalidationPublisher) 으로 자동 전파되어
 * 같은 Redis 에 붙은 모든 인스턴스의 L1 을 drop 한다.
 * <p>
 * 키 규약:
 * <ul>
 *   <li>TREE 캐시: {@code "full"} = 전체 트리, {@code "root:" + rootCode} = 서브트리</li>
 *   <li>CHILDREN 캐시: {@code rootCode} 자체가 키</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class MasterCodeCacheService {

  private static final String TREE_FULL_KEY = "full";
  private static final String TREE_ROOT_PREFIX = "root:";

  private final CacheManager cacheManager;

  // ========== 트리 캐시 ==========

  @SuppressWarnings("unchecked")
  public List<MasterCodeTreeResponse> getFullTree() {
    Cache.ValueWrapper wrapper = getTreeCache().get(TREE_FULL_KEY);
    return wrapper != null ? (List<MasterCodeTreeResponse>) wrapper.get() : null;
  }

  public void putFullTree(List<MasterCodeTreeResponse> tree) {
    getTreeCache().put(TREE_FULL_KEY, tree);
  }

  @SuppressWarnings("unchecked")
  public List<MasterCodeTreeResponse> getSubTree(String rootCode) {
    Cache.ValueWrapper wrapper = getTreeCache().get(TREE_ROOT_PREFIX + rootCode);
    return wrapper != null ? (List<MasterCodeTreeResponse>) wrapper.get() : null;
  }

  public void putSubTree(String rootCode, List<MasterCodeTreeResponse> tree) {
    getTreeCache().put(TREE_ROOT_PREFIX + rootCode, tree);
  }

  // ========== Children 캐시 ==========

  @SuppressWarnings("unchecked")
  public List<MasterCodeResponse> getChildren(String rootCode) {
    Cache.ValueWrapper wrapper = getChildrenCache().get(rootCode);
    return wrapper != null ? (List<MasterCodeResponse>) wrapper.get() : null;
  }

  public void putChildren(String rootCode, List<MasterCodeResponse> children) {
    getChildrenCache().put(rootCode, children);
  }

  // ========== 무효화 ==========

  /**
   * 특정 rootCode 의 트리·서브트리·children 캐시를 한 번에 무효화.
   * TwoTierCache 내부에서 Pub/Sub 이벤트가 발행되어 다른 인스턴스의 L1 도 drop 된다.
   */
  public void evictByRootCode(String rootCode) {
    log.debug("마스터코드 캐시 무효화: rootCode={}", rootCode);
    getTreeCache().evict(TREE_FULL_KEY);
    getTreeCache().evict(TREE_ROOT_PREFIX + rootCode);
    getChildrenCache().evict(rootCode);
  }

  /**
   * 모든 마스터코드 캐시 전체 비우기 (관리자용).
   */
  public void evictAll() {
    log.debug("마스터코드 전체 캐시 무효화");
    getTreeCache().clear();
    getChildrenCache().clear();
  }

  // ========== 내부 헬퍼 ==========

  private Cache getTreeCache() {
    Cache cache = cacheManager.getCache(MasterCodeCacheNames.TREE);
    if (cache == null) {
      throw new IllegalStateException(
          "CacheManager 에 '" + MasterCodeCacheNames.TREE + "' 캐시가 등록되지 않았습니다. "
              + "MasterCodeCacheProperties.all() 로 TwoTierCacheConfigurer 를 구성하세요.");
    }
    return cache;
  }

  private Cache getChildrenCache() {
    Cache cache = cacheManager.getCache(MasterCodeCacheNames.CHILDREN);
    if (cache == null) {
      throw new IllegalStateException(
          "CacheManager 에 '" + MasterCodeCacheNames.CHILDREN + "' 캐시가 등록되지 않았습니다. "
              + "MasterCodeCacheProperties.all() 로 TwoTierCacheConfigurer 를 구성하세요.");
    }
    return cache;
  }
}
