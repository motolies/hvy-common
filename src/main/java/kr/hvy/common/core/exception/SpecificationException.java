package kr.hvy.common.core.exception;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpecificationException extends RuntimeException {

  private final List<String> errorMessages;

  public SpecificationException(String message) {
    super(message);
    this.errorMessages = new ArrayList<>();
  }

  public SpecificationException(List<String> messages) {
    super(Optional.ofNullable(messages)
        .map(errList -> errList.stream().collect(Collectors.joining(", ")))
        .orElse("No error messages provided"));
    this.errorMessages = Optional.ofNullable(messages).orElse(new ArrayList<>());
  }

  public List<String> getErrorMessages() {
    return errorMessages;
  }
}
