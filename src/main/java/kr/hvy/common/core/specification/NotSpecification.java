package kr.hvy.common.core.specification;

import java.util.ArrayList;
import java.util.List;
import kr.hvy.common.core.exception.SpecificationException;
import org.springframework.util.CollectionUtils;

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
  public void validateException(T t) throws SpecificationException {
    if (spec.isSatisfiedBy(t)) {
      List<String> errors = collectErrors(t);
      if (CollectionUtils.isEmpty(errors)) {
        errors.add(getErrorMessage());
      }
      throw new SpecificationException(errors);
    }
  }

  @Override
  public List<String> collectErrors(T t) {
    return new ArrayList<>(spec.collectErrors(t));
  }

  @Override
  public String getErrorMessage() {
    return "NotSpecification is not satisfied.";
  }
}