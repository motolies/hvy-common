package kr.hvy.common.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

  /**
   * 만족 여부와 관계없이 검증에 따른 오류 메시지 수집
   * <pre>
   * public void createUser(User user) {
   *   Specification<User> spec = new UserCreateSpecification()
   *                                   .and(new AnotherSpecification())
   *                                   .or(new YetAnotherSpecification());
   *
   *   spec.validateOptional(user).ifPresent(errors -> {
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
  default Optional<List<String>> validateOptional(T t) {
    List<String> errors = new ArrayList<>();
    collectErrors(t, errors);
    return errors.isEmpty() ? Optional.empty() : Optional.of(errors);
  }

  default void collectErrors(T t, List<String> errors) {
    if (!isSatisfiedBy(t)) {
      errors.add(getErrorMessage());
    }
  }

  default String getErrorMessage() {
    return "Specification is not satisfied.";
  }
}