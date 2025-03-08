package kr.hvy.common.db.log;


import java.util.List;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.StatementType;
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;
import org.hibernate.engine.jdbc.internal.FormatStyle;

/**
 * DefaultQueryLogEntryCreator를 상속받아 커스텀 로깅을 구현: 1) PreparedStatement 파라미터를 '?' 자리에 인라인 치환 2) Params 표시 제거 3) Time에 "ms" 단위 표시 4) Query:[ 앞에 줄바꿈
 */
public class BoundQueryLogEntryCreator extends DefaultQueryLogEntryCreator {

  private final boolean formatQuery;

  public BoundQueryLogEntryCreator(boolean formatQuery) {
    this.formatQuery = formatQuery;
  }

  /**
   * "Query:[ ... ]"를 "Query:\n[ ... ]"로 바꾸고, PreparedStatement라면 getInlinedQuery()로 치환된 쿼리를 출력.
   */
  @Override
  protected void writeQueriesEntry(StringBuilder sb, ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    // 줄바꿈 추가
    sb.append("Query:\n");
    for (QueryInfo queryInfo : queryInfoList) {
      String originalQuery = queryInfo.getQuery();
      if (execInfo.getStatementType() == StatementType.PREPARED) {
        // PreparedStatement 파라미터 인라인 치환
        String inlined = getInlinedQuery(originalQuery, queryInfo);
        sb.append(formatQuery ? formatSqlQuery(inlined) : inlined).append("\n");
      } else {
        // 일반 Statement, CallableStatement 등은 쿼리 그대로
        sb.append(originalQuery).append("\n");
      }
    }
  }

  /**
   * 파라미터 목록을 직접 "?" 자리에 치환해서 "바인딩된 SQL" 문자열을 만든다.
   */
  private String getInlinedQuery(String query, QueryInfo queryInfo) {
    // datasource-proxy는 QueryInfo에 여러 parametersList가 있을 수 있음(배치 등).
    // 여기서는 "첫 번째 파라미터 세트"만 사용한다고 가정.
    if (queryInfo.getParametersList().isEmpty()) {
      return query;
    }

    List<ParameterSetOperation> firstParamSet = queryInfo.getParametersList().get(0);
    for (ParameterSetOperation param : firstParamSet) {
      Object[] args = param.getArgs();
      if (args == null || args.length == 0) {
        continue;
      }
      // 간단히 "마지막 인자"를 실제 파라미터 값으로 보고 치환
      Object value = args[args.length - 1];

      String replacedValue;
      if (value instanceof String) {
        replacedValue = "'" + value + "'";
      } else {
        replacedValue = String.valueOf(value);
      }
      // 첫 번째 '?'만 순차적으로 교체
      query = query.replaceFirst("\\?", replacedValue);
    }
    return query;
  }

  /**
   * 기본 구현에서는 "Time:10, ..." 식으로 찍히는데, 뒤에 "ms"를 붙이기 위해 오버라이드.
   */
  @Override
  protected void writeTimeEntry(StringBuilder sb, ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    sb.append("Time:");
    sb.append(execInfo.getElapsedTime());
    sb.append("ms, ");
  }

  /**
   * Params 관련 로그를 완전히 제거하기 위해, 기본 구현을 비워둠.
   */
  @Override
  protected void writeParamsEntry(StringBuilder sb, ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    // 아무것도 안 함 -> Params 출력 생략
  }

  private String formatSqlQuery(String query) {
    return FormatStyle.BASIC.getFormatter().format(query);
  }

}