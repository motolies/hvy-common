package kr.hvy.common.infrastructure.redis.impl.masterdata.query;

import java.util.List;
import kr.hvy.common.infrastructure.redis.impl.masterdata.dto.MasterCodeResponse;
import kr.hvy.common.infrastructure.redis.impl.masterdata.dto.MasterCodeTreeResponse;

/**
 * 마스터코드 원본 조회 SPI.
 * <p>
 * 캐시(L1/L2) 가 모두 미스일 때 호출된다. 구현체는 두 종류:
 * <ul>
 *   <li><b>blog-back (원본 소유자)</b>: {@code JpaMasterCodeLoader} — {@code MasterCodeRepository} 로 DB 직접 조회.</li>
 *   <li><b>소비 앱</b>: {@link RestClientMasterCodeLoader} — blog-back REST API 호출.</li>
 * </ul>
 */
public interface MasterCodeLoader {

  List<MasterCodeTreeResponse> loadFullTree();

  List<MasterCodeTreeResponse> loadSubTree(String rootCode);

  List<MasterCodeResponse> loadChildren(String rootCode);
}
