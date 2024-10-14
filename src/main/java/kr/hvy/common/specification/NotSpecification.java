package kr.hvy.common.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import kr.hvy.common.exception.SpecificationException;
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
    if (!(!spec.isSatisfiedBy(t))) {
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
    errors.addAll(spec.collectErrors(t));
    return errors;
  }

  @Override
  public String getErrorMessage() {
    return "NotSpecification is not satisfied.";
  }
}