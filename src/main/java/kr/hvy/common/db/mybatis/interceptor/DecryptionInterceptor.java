package kr.hvy.common.db.mybatis.interceptor;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import kr.hvy.common.crypto.Decryptor;
import kr.hvy.common.crypto.annotation.EncryptedField;
import kr.hvy.common.crypto.annotation.SecretData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.springframework.util.CollectionUtils;

@Slf4j
@Intercepts({
    @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})
})
@RequiredArgsConstructor
public class DecryptionInterceptor implements Interceptor {

  private final Decryptor decryptor;

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    Object resultObject = invocation.proceed();
    if (Objects.isNull(resultObject)) {
      return null;
    }

    if (resultObject instanceof List) {
      List resultList = (List) resultObject;

      if (!CollectionUtils.isEmpty(resultList) && checkSecretData(resultList.get(0))) {
        decryptListParallel(resultList);
      }
    } else {
      if (checkSecretData(resultObject)) {
        decryptData(resultObject);
      }
    }
    return resultObject;
  }

  private void decryptListParallel(List<Object> resultList) {
    CompletableFuture.allOf(
        resultList.stream()
            .map(result -> CompletableFuture.runAsync(() -> {
              try {
                decryptData(result);
              } catch (Exception e) {
                log.error("decrypt error", e);
              }
            })).toArray(CompletableFuture[]::new)
    ).join();
  }

  private boolean checkSecretData(Object object) {
    Class<?> objectClass = object.getClass();
    return objectClass.isAnnotationPresent(SecretData.class);
  }

  private void decryptData(Object object) throws Exception {
    Class<?> entityClass = object.getClass();
    Field[] fields = entityClass.getDeclaredFields();
    for (Field field : fields) {
      if (field.isAnnotationPresent(EncryptedField.class)) {
        field.setAccessible(true);
        if (field.getType().equals(String.class)) {
          String value = (String) field.get(object);
          String decryptedValue = decryptor.decrypt(value);
          field.set(object, decryptedValue);
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
