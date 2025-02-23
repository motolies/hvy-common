package kr.hvy.common.code.base;

import jakarta.persistence.AttributeConverter;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class AbstractEnumCodeConverter<E extends Enum<E> & EnumCode<T>, T> implements AttributeConverter<E, T> {

  private final Class<E> enumClass;

  protected AbstractEnumCodeConverter(Class<E> enumClass) {
    this.enumClass = enumClass;
  }

  @Override
  public T convertToDatabaseColumn(E attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.getCode();
  }

  @Override
  public E convertToEntityAttribute(T dbData) {
    if (dbData == null) {
      return null;
    }
    return Stream.of(enumClass.getEnumConstants())
        .filter(e -> Objects.equals(e.getCode(), dbData))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("알 수 없는 코드: " + dbData));
  }
}