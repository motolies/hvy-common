package kr.hvy.common.infrastructure.database.mybatis.interceptor;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import kr.hvy.common.application.crypto.Encryptor;
import kr.hvy.common.application.crypto.annotation.EncryptedField;
import kr.hvy.common.application.crypto.annotation.SecretData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.springframework.util.ObjectUtils;

@Slf4j
@Intercepts({
    @Signature(type = ParameterHandler.class, method = "setParameters", args = PreparedStatement.class),
})
@RequiredArgsConstructor
public class EncryptionInterceptor implements Interceptor {

  private final Encryptor encryptor;

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    encryptData(invocation);
    return invocation.proceed();
  }

  private void encryptData(Invocation invocation) throws Exception {
    ParameterHandler parameterHandler = (ParameterHandler) invocation.getTarget();
    Field parameterField = parameterHandler.getClass().getDeclaredField("parameterObject");
    parameterField.setAccessible(true);

    Object parameterObject = parameterField.get(parameterHandler);

    if (ObjectUtils.isEmpty(parameterObject)) {
      return;
    }

    if (parameterObject instanceof Map) {
      Map<String, Object> parameterMap = (Map<String, Object>) parameterObject;

      if (parameterMap.containsKey("list")) {
        List<Object> collections = (List<Object>) parameterMap.get("list");
        CompletableFuture.allOf(
            collections.stream()
                .map(inputParam -> CompletableFuture.runAsync(() -> {
                  try {
                    encryptObject(inputParam);
                  } catch (Exception e) {
                    log.error("decrypt error", e);
                  }
                })).toArray(CompletableFuture[]::new)
        ).join();
      }
    } else {
      encryptObject(parameterObject); // 단일 객체의 경우 기존 코드와 동일하게 암호화
    }
  }

  private void encryptObject(Object parameterObject) throws Exception {
    Class<?> entityClass = parameterObject.getClass();
    if (!entityClass.isAnnotationPresent(SecretData.class)) {
      return;
    }

    Field[] fields = entityClass.getDeclaredFields();
    for (Field field : fields) {
      if (field.isAnnotationPresent(EncryptedField.class)) {
        field.setAccessible(true);
        if (field.getType().equals(String.class)) {
          String value = (String) field.get(parameterObject);
          String encryptedValue = encryptor.encrypt(value);
          field.set(parameterObject, encryptedValue);
        }
      }
    }
  }


  @Override
  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  @Override
  public void setProperties(Properties properties) {
  }
}