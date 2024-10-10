package kr.hvy.common.specification;

import kr.hvy.common.exception.SpecificationException;

public interface Specification<T> {
  boolean isSatisfiedBy(T t);

  // 두 명세를 결합 (AND)
  default Specification<T> and(Specification<T> other) {
    return t -> this.isSatisfiedBy(t) && other.isSatisfiedBy(t);
  }

  // 두 명세를 결합 (OR)
  default Specification<T> or(Specification<T> other) {
    return t -> this.isSatisfiedBy(t) || other.isSatisfiedBy(t);
  }

  // 명세 반전 (NOT)
  default Specification<T> not() {
    return t -> !this.isSatisfiedBy(t);
  }

  // 명세가 만족되지 않으면 예외를 던지는 메서드
  default void validate(T t) throws SpecificationException {
    if (!isSatisfiedBy(t)) {
      throw new SpecificationException("Specification is not satisfied.");
    }
  }
}