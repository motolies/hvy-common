package kr.hvy.common.specification;

import java.util.ArrayList;
import java.util.List;
import kr.hvy.common.exception.SpecificationException;
import org.springframework.util.CollectionUtils;

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
  public void validateException(T t) throws SpecificationException {
    if (!(spec1.isSatisfiedBy(t) && spec2.isSatisfiedBy(t))) {
      List<String> errors = collectErrors(t);
      if (CollectionUtils.isEmpty(errors)) {
        errors.add(getErrorMessage());
      }
      throw new SpecificationException(errors);
    }
  }

  @Override
  public List<String> collectErrors(T t) {
    List<String> errors = new ArrayList<>();
    errors.addAll(spec1.collectErrors(t));
    errors.addAll(spec2.collectErrors(t));
    return errors;
  }

  @Override
  public String getErrorMessage() {
    return "AndSpecification is not satisfied.";
  }
}