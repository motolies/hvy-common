package kr.hvy.common.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kr.hvy.common.exception.SpecificationException;
import org.apache.commons.collections4.CollectionUtils;

public interface Specification<T> {

  boolean isSatisfiedBy(T t);

  default Specification<T> and(Specification<T> other) {
    return new AndSpecification<>(this, other);
  }

  default Specification<T> or(Specification<T> other) {
    return new OrSpecification<>(this, other);
  }

  default Specification<T> not() {
    return new NotSpecification<>(this);
  }


  default void validateException(T t) throws SpecificationException {
    if (!isSatisfiedBy(t)) {
      throw new SpecificationException(getErrorMessage());
    }
  }

  /**
   * 만족 여부와 관계없이 검증에 따른 오류 메시지 수집
   * <pre>
   * public void createUser(User user) {
   *   Specification<User> spec = new UserCreateSpecification()
   *                                   .and(new AnotherSpecification())
   *                                   .or(new YetAnotherSpecification());
   *
   *   spec.validateOptionalMessages(user).ifPresent(errors -> {
   *       String errorMessage = String.join(", ", errors);
   *       throw new SpecificationException(errorMessage);
   *   });
   *
   *   // 사용자 생성 로직
   * }
   * </pre>
   *
   * @param t the t
   * @return the optional
   */
  default Optional<List<String>> validateOptionalMessages(T t) {
    List<String> errors = collectErrors(t);
    return CollectionUtils.isEmpty(errors)? Optional.empty() : Optional.of(errors);
  }

  default List<String> collectErrors(T t) {
    List<String> errors = new ArrayList<>();
    if (!isSatisfiedBy(t)) {
      errors.add(getErrorMessage());
    }
    return errors;
  }

  default String getErrorMessage() {
    return "Specification is not satisfied.";
  }
}