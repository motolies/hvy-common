package kr.hvy.common.infrastructure.redis.impl.masterdata.cache;

/**
 * 마스터코드 캐시 이름 상수.
 * <p>
 * blog-back 과 소비 모듈이 동일한 CacheManager 키를 쓰기 위해 공유하는 단일 소스.
 * TwoTierCache 는 이 이름을 L1(Caffeine) 이름이자 L2(Redis) 키 prefix(`cache:&lt;name&gt;`) 로 사용한다.
 */
public final class MasterCodeCacheNames {

  /**
   * 전체 트리 + rootCode 별 서브트리 캐시.
   */
  public static final String TREE = "masterCodeTree";

  /**
   * 단일 노드 조회 캐시 (현재 예약, 사용은 향후 확장).
   */
  public static final String NODE = "masterCodeNode";

  /**
   * 루트 코드별 직계 자식 리스트 캐시.
   */
  public static final String CHILDREN = "masterCodeChildren";

  private MasterCodeCacheNames() {
  }
}
