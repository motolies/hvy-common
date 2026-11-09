package kr.hvy.common.infrastructure.database.mybatis.interceptor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kr.hvy.common.application.crypto.annotation.EncryptedField;
import kr.hvy.common.application.crypto.annotation.SecretData;

/**
 * {@link SecretData} 클래스의 {@link EncryptedField} 목록을 해석·캐시한다.
 * 암호화/복호화 인터셉터가 공유하여 대상 판정 정책이 어긋나지 않게 한다.
 *
 * <p>규칙:
 * <ul>
 *   <li>{@code @SecretData}는 클래스 계층(슈퍼클래스 포함) 어디에 있어도 인정한다.</li>
 *   <li>{@code @EncryptedField}는 슈퍼클래스까지 수집한다.</li>
 *   <li>{@code @EncryptedField}가 String 이 아니거나 static/final 이면 첫 해석 시 {@link IllegalStateException}.
 *       (리플렉션 set 이 불가하거나 의도치 않은 동작이므로 조용히 누락시키지 않는다)</li>
 * </ul>
 */
final class SecretFieldResolver {

  private static final List<Field> NONE = List.of();
  private static final Map<Class<?>, List<Field>> CACHE = new ConcurrentHashMap<>();

  private SecretFieldResolver() {
  }

  /** 해당 타입이 암호화 대상 필드를 하나 이상 가진 {@code @SecretData} 클래스인지 여부 */
  static boolean isSecretData(Class<?> type) {
    return !encryptedFields(type).isEmpty();
  }

  /** 해당 타입의 {@code @EncryptedField} 목록(접근 가능 상태). 대상이 아니면 빈 리스트. */
  static List<Field> encryptedFields(Class<?> type) {
    return CACHE.computeIfAbsent(type, SecretFieldResolver::resolve);
  }

  /** 클래스 계층을 순회하며 어노테이션을 해석한다. 캐시 미스 시 1회만 호출된다. */
  private static List<Field> resolve(Class<?> type) {
    if (!hasSecretDataAnnotation(type)) {
      return NONE;
    }
    List<Field> fields = new ArrayList<>();
    for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field field : c.getDeclaredFields()) {
        if (!field.isAnnotationPresent(EncryptedField.class)) {
          continue;
        }
        validate(c, field);
        field.setAccessible(true);
        fields.add(field);
      }
    }
    return fields.isEmpty() ? NONE : List.copyOf(fields);
  }

  /** 클래스 또는 슈퍼클래스에 {@code @SecretData}가 있는지 확인한다. */
  private static boolean hasSecretDataAnnotation(Class<?> type) {
    for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
      if (c.isAnnotationPresent(SecretData.class)) {
        return true;
      }
    }
    return false;
  }

  /** 암호화 필드 제약(String, non-static, non-final)을 검증한다. */
  private static void validate(Class<?> owner, Field field) {
    String location = owner.getName() + "." + field.getName();
    if (field.getType() != String.class) {
      throw new IllegalStateException("@EncryptedField 는 String 필드에만 사용할 수 있습니다: " + location);
    }
    int mod = field.getModifiers();
    if (Modifier.isStatic(mod) || Modifier.isFinal(mod)) {
      throw new IllegalStateException("@EncryptedField 는 static/final 필드에 사용할 수 없습니다: " + location);
    }
  }
}
