package kr.hvy.common.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import kr.hvy.common.exception.SpecificationException;

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
      String errorMsg = Optional.ofNullable(errors)
          .map(errList -> errList.stream().collect(Collectors.joining(", ")))
          .orElse(getErrorMessage());

      throw new SpecificationException(errorMsg);
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