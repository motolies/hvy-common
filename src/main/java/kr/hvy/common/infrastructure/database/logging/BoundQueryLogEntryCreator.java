package kr.hvy.common.infrastructure.database.logging;


import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
   * 2단계 치환 방식으로 문자열 리터럴 안의 ?와 SQL 파라미터 자리표시자를 안전하게 구분한다.
   */
  private String getInlinedQuery(String query, QueryInfo queryInfo) {
    if (queryInfo.getParametersList().isEmpty()) {
      return query;
    }

    // 1단계: SQL 파라미터를 안전한 플레이스홀더로 치환
    String queryWithPlaceholders = replaceParametersWithPlaceholders(query);

    // 파라미터 매핑 구성 (기존 로직 유지)
    List<ParameterSetOperation> firstParamSet = queryInfo.getParametersList().get(0);
    Map<Integer, Object> parameterMap = new TreeMap<>();

    for (ParameterSetOperation param : firstParamSet) {
      Object[] args = param.getArgs();
      String methodName = param.getMethod().getName();

      if (args == null || args.length < 2) {
        continue;
      }

      Integer index = (Integer) args[0];  // 첫 번째 인자는 파라미터 인덱스
      Object value = "setNull".equals(methodName) ? null : args[1];  // setNull은 null, 나머지는 실제 값
      parameterMap.put(index, value);
    }

    // 2단계: 플레이스홀더를 실제 값으로 치환
    return replacePlaceholdersWithValues(queryWithPlaceholders, parameterMap);
  }

  /**
   * 원본 쿼리의 SQL 파라미터 자리표시자(?)를 안전한 플레이스홀더로 치환한다.
   * 문자열 리터럴 안의 ?는 치환하지 않는다.
   */
  private String replaceParametersWithPlaceholders(String query) {
    int paramCount = 1;
    StringBuilder result = new StringBuilder();
    boolean inStringLiteral = false;

    for (int i = 0; i < query.length(); i++) {
      char c = query.charAt(i);

      if (c == '\'') {
        // SQL 이스케이프 처리: '' 는 작은따옴표 하나를 의미
        if (inStringLiteral && i + 1 < query.length() && query.charAt(i + 1) == '\'') {
          result.append("''");
          i++; // 다음 따옴표 건너뛰기
        } else {
          inStringLiteral = !inStringLiteral;
          result.append(c);
        }
      } else if (c == '?' && !inStringLiteral) {
        // 문자열 리터럴 밖의 ?만 플레이스홀더로 치환
        result.append("__PARAM_").append(paramCount++).append("__");
      } else {
        result.append(c);
      }
    }

    return result.toString();
  }

  /**
   * 플레이스홀더를 실제 파라미터 값으로 치환한다.
   */
  private String replacePlaceholdersWithValues(String query, Map<Integer, Object> parameterMap) {
    String result = query;
    for (Map.Entry<Integer, Object> entry : parameterMap.entrySet()) {
      String placeholder = "__PARAM_" + entry.getKey() + "__";
      String value = formatParameterValue(entry.getValue());
      result = result.replace(placeholder, value);
    }
    return result;
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

  /**
   * 파라미터 값을 타입에 따라 적절히 포맷팅
   */
  private String formatParameterValue(Object value) {
    if (value == null) return "null";

    if (value instanceof String) {
      return escapeStringValue((String) value);
    }

    // LocalDate: 'yyyy-MM-dd' 형식
    if (value instanceof java.time.LocalDate) {
      return "'" + ((java.time.LocalDate) value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "'";
    }

    // LocalDateTime: 'yyyy-MM-dd HH:mm:ss.SSSSSS' 형식
    if (value instanceof java.time.LocalDateTime) {
      return "'" + ((java.time.LocalDateTime) value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")) + "'";
    }

    // LocalTime: 'HH:mm:ss.SSSSSS' 형식
    if (value instanceof java.time.LocalTime) {
      return "'" + ((java.time.LocalTime) value).format(DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS")) + "'";
    }

    // java.sql.Date: 'yyyy-MM-dd' 형식
    if (value instanceof java.sql.Date) {
      return "'" + new SimpleDateFormat("yyyy-MM-dd").format((java.sql.Date) value) + "'";
    }

    // java.sql.Timestamp: 'yyyy-MM-dd HH:mm:ss.SSSSSS' 형식
    if (value instanceof java.sql.Timestamp) {
      // SimpleDateFormat에서는 S가 millisecond이므로, Timestamp의 toString() 사용
      String timestampStr = value.toString();
      // 필요시 microsecond 자릿수 조정
      if (timestampStr.length() > 19 && timestampStr.contains(".")) {
        String[] parts = timestampStr.split("\\.");
        if (parts.length == 2) {
          String nanos = parts[1];
          // 6자리 microsecond로 맞추기
          if (nanos.length() > 6) {
            nanos = nanos.substring(0, 6);
          } else {
            nanos = String.format("%-6s", nanos).replace(' ', '0');
          }
          timestampStr = parts[0] + "." + nanos;
        }
      }
      return "'" + timestampStr + "'";
    }

    // 기타 타입은 그대로
    return String.valueOf(value);
  }

  /**
   * 문자열 값의 특수문자를 SQL 로깅에 적합하게 이스케이프 처리
   */
  private String escapeStringValue(String value) {
    if (value == null) return "null";

    // SQL 표준에 따라 작은따옴표를 두 개로 이스케이프
    String escaped = value.replace("'", "''");

    // 백슬래시도 이스케이프 (로그 가독성 향상)
    escaped = escaped.replace("\\", "\\\\");

    return "'" + escaped + "'";
  }

}