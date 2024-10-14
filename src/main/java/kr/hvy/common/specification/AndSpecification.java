package kr.hvy.common.specification;

import java.util.List;

public class AndSpecification<T> implements Specification<T> {

  private final Specification<T> spec1;
  private final Specification<T> spec2;

  public AndSpecification(Specification<T> spec1, Specification<T> spec2) {
    this.spec1 = spec1;
    this.spec2 = spec2;
  }

  @Override
  public boolean isSatisfiedBy(T t) {
    return spec1.isSatisfiedBy(t) && spec2.isSatisfiedBy(t);
  }

  @Override
  public void collectErrors(T t, List<String> errors) {
    spec1.collectErrors(t, errors);
    spec2.collectErrors(t, errors);
  }

  @Override
  public String getErrorMessage() {
    return "AndSpecification is not satisfied.";
  }
}