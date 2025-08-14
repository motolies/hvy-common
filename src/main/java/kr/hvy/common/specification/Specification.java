package kr.hvy.common.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
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
    return CollectionUtils.isEmpty(errors) ? Optional.empty() : Optional.of(errors);
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

  // ========== Static Helper Methods ==========

  /**
   * Static 방식으로 validation 수행
   */
  static <T> void validate(Specification<T> spec, T t) throws SpecificationException {
    spec.validateException(t);
  }

  /**
   * Static 방식으로 조건 확인
   */
  static <T> boolean check(Specification<T> spec, T t) {
    return spec.isSatisfiedBy(t);
  }

  /**
   * Static 방식으로 에러 메시지 수집
   */
  static <T> Optional<List<String>> getValidationErrors(Specification<T> spec, T t) {
    return spec.validateOptionalMessages(t);
  }

  // ========== Common Static Factory Methods ==========

  /**
   * Supplier를 이용한 Thread-Safe validation 수행
   * <p>
   * 사용법: Specification.validate(CategoryCreateSpecification::new, categoryCreate);
   */
  static <T, S extends Specification<T>> void validate(Supplier<S> specSupplier, T data) throws SpecificationException {
    specSupplier.get().validateException(data);
  }

  /**
   * Supplier를 이용한 Thread-Safe 조건 확인
   * <p>
   * 사용법: Specification.isValid(CategoryCreateSpecification::new, categoryCreate);
   */
  static <T, S extends Specification<T>> boolean isValid(Supplier<S> specSupplier, T data) {
    return specSupplier.get().isSatisfiedBy(data);
  }

  /**
   * Supplier를 이용한 Thread-Safe 에러 메시지 수집
   * <p>
   * 사용법: Specification.getErrors(CategoryCreateSpecification::new, categoryCreate);
   */
  static <T, S extends Specification<T>> Optional<List<String>> getErrors(Supplier<S> specSupplier, T data) {
    return specSupplier.get().validateOptionalMessages(data);
  }

  /**
   * Static 방식으로 AND 조합
   */
  static <T> Specification<T> and(Specification<T> spec1, Specification<T> spec2) {
    return new AndSpecification<>(spec1, spec2);
  }

  /**
   * Static 방식으로 OR 조합
   */
  static <T> Specification<T> or(Specification<T> spec1, Specification<T> spec2) {
    return new OrSpecification<>(spec1, spec2);
  }

  /**
   * Static 방식으로 NOT 조합
   */
  static <T> Specification<T> not(Specification<T> spec) {
    return spec.not();
  }
}