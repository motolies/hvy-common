package kr.hvy.common.specification;

import java.util.List;

public class NotSpecification<T> implements Specification<T> {

  private final Specification<T> spec;

  public NotSpecification(Specification<T> spec) {
    this.spec = spec;
  }

  @Override
  public boolean isSatisfiedBy(T t) {
    return !spec.isSatisfiedBy(t);
  }

  @Override
  public void collectErrors(T t, List<String> errors) {
    if (spec.isSatisfiedBy(t)) {
      errors.add("NotSpecification is not satisfied.");
    }
  }

  @Override
  public String getErrorMessage() {
    return "NotSpecification is not satisfied.";
  }
}