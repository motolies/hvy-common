package kr.hvy.common.specification;

public interface Specification<T> {

  boolean isSatisfiedBy(T t);

  Specification<T> and(Specification<T> specification);
}