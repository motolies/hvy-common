package kr.hvy.common.specification;

import java.util.List;

public class OrSpecification<T> implements Specification<T> {

  private final Specification<T> spec1;
  private final Specification<T> spec2;

  public OrSpecification(Specification<T> spec1, Specification<T> spec2) {
    this.spec1 = spec1;
    this.spec2 = spec2;
  }

  @Override
  public boolean isSatisfiedBy(T t) {
    return spec1.isSatisfiedBy(t) || spec2.isSatisfiedBy(t);
  }

  @Override
  public void collectErrors(T t, List<String> errors) {
    if (!spec1.isSatisfiedBy(t)) {
      spec1.collectErrors(t, errors);
    }
    if (!spec2.isSatisfiedBy(t)) {
      spec2.collectErrors(t, errors);
    }
  }

  @Override
  public String getErrorMessage() {
    return "OrSpecification is not satisfied.";
  }
}