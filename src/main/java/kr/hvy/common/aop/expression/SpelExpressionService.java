package kr.hvy.common.aop.expression;

import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

/*
 * SpEL(Spring Expression Language) 표현식을 처리하는 서비스
 * 메서드 파라미터와 SpEL 표현식을 통해 동적으로 값을 추출할 수 있습니다.
 */
@Service
public class SpelExpressionService {

  private final SpelExpressionParser parser = new SpelExpressionParser();
  private final BeanFactoryResolver beanFactoryResolver;

  public SpelExpressionService(ApplicationContext applicationContext) {
    this.beanFactoryResolver = new BeanFactoryResolver(applicationContext);
  }

  /**
   * 메서드 파라미터와 SpEL 표현식을 사용하여 문자열 값을 추출합니다.
   *
   * @param parameterNames 메서드 파라미터 이름 배열
   * @param args           메서드 파라미터 값 배열
   * @param expression     SpEL 표현식
   * @return 표현식 평가 결과 문자열
   */
  public String evaluateExpression(String[] parameterNames, Object[] args, String expression) {
    StandardEvaluationContext context = createEvaluationContext(parameterNames, args);
    return parser.parseExpression(expression).getValue(context, String.class);
  }

  /**
   * 메서드 파라미터와 SpEL 표현식을 사용하여 특정 타입의 값을 추출합니다.
   *
   * @param parameterNames 메서드 파라미터 이름 배열
   * @param args           메서드 파라미터 값 배열
   * @param expression     SpEL 표현식
   * @param returnType     반환할 타입 클래스
   * @return 표현식 평가 결과
   */
  public <T> T evaluateExpression(String[] parameterNames, Object[] args, String expression, Class<T> returnType) {
    StandardEvaluationContext context = createEvaluationContext(parameterNames, args);
    return parser.parseExpression(expression).getValue(context, returnType);
  }

  /**
   * 단순 SpEL 표현식을 평가합니다 (파라미터 없이)
   *
   * @param expression SpEL 표현식
   * @param returnType 반환할 타입 클래스
   * @return 표현식 평가 결과
   */
  public <T> T evaluateExpression(String expression, Class<T> returnType) {
    StandardEvaluationContext context = createBaseEvaluationContext();
    return parser.parseExpression(expression).getValue(context, returnType);
  }

  /**
   * 주어진 문자열이 SpEL 표현식인지 확인합니다.
   * SpEL 표현식의 특징적인 문자들(#, @, ', +, ., (, ))을 확인하여 판별합니다.
   *
   * @param expression 확인할 문자열
   * @return SpEL 표현식이면 true, 일반 문자열이면 false
   */
  public boolean isSpelExpression(String expression) {
    if (expression == null || expression.trim().isEmpty()) {
      return false;
    }
    
    // SpEL 표현식의 특징적인 문자들을 확인
    return expression.contains("#") ||    // 파라미터 참조: #param
           expression.contains("@") ||    // 빈 참조: @beanName
           expression.contains("'") ||    // 문자열 리터럴: 'string'
           expression.contains("+") ||    // 연산자: +
           expression.contains(".") ||    // 메서드/프로퍼티 접근: obj.method()
           expression.contains("(") ||    // 메서드 호출: method()
           expression.contains(")");
  }

  /*
   * 메서드 파라미터를 포함한 평가 컨텍스트를 생성합니다.
   * BeanFactoryResolver는 재사용하되, 매번 새로운 컨텍스트를 생성하여 변수 격리를 보장합니다.
   */
  private StandardEvaluationContext createEvaluationContext(String[] parameterNames, Object[] args) {
    StandardEvaluationContext context = createFreshContext();

    // 메서드 파라미터들을 컨텍스트 변수로 등록
    for (int i = 0; i < parameterNames.length; i++) {
      context.setVariable(parameterNames[i], args[i]);
    }

    return context;
  }

  /*
   * 기본 평가 컨텍스트를 생성합니다.
   * BeanFactoryResolver는 재사용하되, 매번 새로운 컨텍스트를 생성합니다.
   */
  private StandardEvaluationContext createBaseEvaluationContext() {
    return createFreshContext();
  }

  /*
   * BeanFactoryResolver가 설정된 새로운 컨텍스트를 생성합니다.
   * BeanFactoryResolver만 재사용하여 성능을 최적화합니다.
   */
  private StandardEvaluationContext createFreshContext() {
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setBeanResolver(beanFactoryResolver);
    return context;
  }
}