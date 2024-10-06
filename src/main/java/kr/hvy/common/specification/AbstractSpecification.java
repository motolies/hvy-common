package kr.hvy.common.specification;


import kr.hvy.common.exception.GenericSpecificationException;

public abstract class AbstractSpecification<T> implements Specification<T> {

  public abstract boolean isSatisfiedBy(T t);

  public abstract void check(T t) throws GenericSpecificationException;

  public Specification<T> and(final Specification<T> specification) {
    return new AndSpecification<T>(this, specification);
  }
}
