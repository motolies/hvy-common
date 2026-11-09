package kr.hvy.common.infrastructure.database.mybatis.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;
import kr.hvy.common.application.crypto.annotation.EncryptedField;
import kr.hvy.common.application.crypto.annotation.SecretData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SecretFieldResolver - @SecretData/@EncryptedField 해석")
class SecretFieldResolverTest {

  @SecretData
  static class Base {

    @EncryptedField
    String baseSecret;
    String plain;
  }

  static class Child extends Base {

    @EncryptedField
    String childSecret;
  }

  @SecretData
  static class NoEncryptedField {

    String plain;
  }

  @SecretData
  static class BadFinal {

    @EncryptedField
    final String fixed = "x";
  }

  @SecretData
  static class BadType {

    @EncryptedField
    Integer number;
  }

  @Test
  @DisplayName("@SecretData 와 @EncryptedField 는 슈퍼클래스까지 수집한다")
  void collectsSuperclassFields() {
    Set<String> names = SecretFieldResolver.encryptedFields(Child.class).stream()
        .map(Field::getName).collect(Collectors.toSet());

    assertThat(SecretFieldResolver.isSecretData(Child.class)).isTrue();
    assertThat(names).containsExactlyInAnyOrder("baseSecret", "childSecret");
  }

  @Test
  @DisplayName("@SecretData 가 없거나 @EncryptedField 가 하나도 없으면 대상이 아니다")
  void nonSecretTypes() {
    assertThat(SecretFieldResolver.isSecretData(String.class)).isFalse();
    assertThat(SecretFieldResolver.encryptedFields(Integer.class)).isEmpty();
    assertThat(SecretFieldResolver.isSecretData(NoEncryptedField.class)).isFalse();
  }

  @Test
  @DisplayName("final 또는 String 이 아닌 @EncryptedField 는 첫 해석 시 거부한다")
  void rejectsInvalidFields() {
    assertThatThrownBy(() -> SecretFieldResolver.encryptedFields(BadFinal.class))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("fixed");
    assertThatThrownBy(() -> SecretFieldResolver.encryptedFields(BadType.class))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("number");
  }
}
