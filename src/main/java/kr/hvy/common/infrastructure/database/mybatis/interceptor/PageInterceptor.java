package kr.hvy.common.infrastructure.database.mybatis.interceptor;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import kr.hvy.common.application.domain.dto.paging.OrderBy;
import kr.hvy.common.application.domain.dto.paging.PageRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;

@Slf4j
@Intercepts({
    @Signature(
        type = StatementHandler.class,
        method = "prepare",
        args = {Connection.class, Integer.class}
    )
})
public class PageInterceptor implements Interceptor {


  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    StatementHandler statementHandler = (StatementHandler) invocation.getTarget();

    BoundSql boundSql = statementHandler.getBoundSql();
    ParameterHandler parameterHandler = statementHandler.getParameterHandler();
    Object parameterObject = parameterHandler.getParameterObject();

    PageRequest pageRequest = getPageRequest(parameterObject);

    if (pageRequest != null) {
      String originalSql = boundSql.getSql();
      StringBuilder modifiedSql = new StringBuilder(originalSql);

      setOrderBy(pageRequest, modifiedSql);
      setLimit(pageRequest, modifiedSql);

      // BoundSql의 sql 필드 리플렉션 수정
      Field field = BoundSql.class.getDeclaredField("sql");
      field.setAccessible(true);
      field.set(boundSql, modifiedSql.toString());

      // --- count 쿼리 실행 시작 ---
      // StatementHandler에서 MappedStatement를 가져오기 위해 리플렉션 사용
      MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
      MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

      String countSql = originalSql.replaceFirst("(?is)SELECT\\s+.+?\\sFROM\\s", "SELECT COUNT(*) FROM ");
      BoundSql countBoundSql = copyFromBoundSql(mappedStatement, boundSql, countSql);

      // invocation의 인자에서 Connection 가져오기 (prepare 메서드의 첫번째 인자)
      Connection connection = (Connection) invocation.getArgs()[0];

      // count 쿼리에 대한 파라미터 핸들러 생성
      DefaultParameterHandler countParameterHandler = new DefaultParameterHandler(mappedStatement, parameterObject, countBoundSql);

      PreparedStatement countStmt = connection.prepareStatement(countSql);
      countParameterHandler.setParameters(countStmt);

      ResultSet rs = countStmt.executeQuery();
      int total = 0;
      if (rs.next()) {
        total = rs.getInt(1);
      }
      rs.close();
      countStmt.close();

      pageRequest.setTotalCount(total);
    }

    return invocation.proceed();
  }

  private static PageRequest getPageRequest(Object parameterObject) {
    PageRequest pageRequest = null;
    if (parameterObject instanceof PageRequest) {
      // 단일 파라미터인 경우
      pageRequest = (PageRequest) parameterObject;
    } else if (parameterObject instanceof Map) {
      // 다중 파라미터(@Param 여러 개)인 경우
      Map<?, ?> paramMap = (Map<?, ?>) parameterObject;
      for (Object value : paramMap.values()) {
        if (value instanceof PageRequest) {
          pageRequest = (PageRequest) value;
          break;
        }
      }
    }
    return pageRequest;
  }

  private static void setLimit(PageRequest pageRequest, StringBuilder modifiedSql) {
    int offset = pageRequest.getPage() * pageRequest.getPageSize();
    modifiedSql.append("\n").append("LIMIT ").append(offset).append(", ").append(pageRequest.getPageSize());
  }

  private static void setOrderBy(PageRequest pageRequest, StringBuilder modifiedSql) {
    List<OrderBy> orderByList = pageRequest.getOrderBy();
    if (orderByList != null && !orderByList.isEmpty()) {
      modifiedSql.append("\n").append("ORDER BY ");
      for (int i = 0; i < orderByList.size(); i++) {
        OrderBy order = orderByList.get(i);
        modifiedSql.append(order.getColumn());
        if (order.getDirection() != null) {
          modifiedSql.append(" ").append(order.getDirection().getCode());
        }
        if (i < orderByList.size() - 1) {
          modifiedSql.append(", ");
        }
      }
    }
  }

  /**
   * 기존 BoundSql의 설정을 유지하면서 새로운 SQL을 가지는 BoundSql를 생성합니다.
   */
  private BoundSql copyFromBoundSql(MappedStatement ms, BoundSql boundSql, String sql) {
    BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), sql,
        boundSql.getParameterMappings(), boundSql.getParameterObject());
    for (ParameterMapping mapping : boundSql.getParameterMappings()) {
      String prop = mapping.getProperty();
      if (boundSql.hasAdditionalParameter(prop)) {
        newBoundSql.setAdditionalParameter(prop, boundSql.getAdditionalParameter(prop));
      }
    }
    return newBoundSql;
  }

  @Override
  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  @Override
  public void setProperties(Properties properties) {
    // 추가 설정이 필요하면 처리
  }
}