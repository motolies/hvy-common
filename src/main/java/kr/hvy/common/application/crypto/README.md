# 암호화 모듈 사용방법

#  설정
application.yml 설정
```yml
system:
  crypto:
    aes:
      key: crypto-secret-key
```

bean 등록
```java
@Configuration
public class CommonConfig extends CommonConfigurer {

  @Bean
  public AESUtil aesUtil() {
    return new AESUtil();
  }

}
```

interceptor 등록
```java
@Configuration
@RequiredArgsConstructor
public class MybatisConfig {

  private final AESUtil aesUtil;

  @Bean
  public EncryptionInterceptor encryptionInterceptor() {
      return new EncryptionInterceptor(aesUtil);
  }

  @Bean
  public DecryptionInterceptor decryptionInterceptor() {
      return new DecryptionInterceptor(aesUtil);
  }
  
  @Bean
  public PageInterceptor gridPagingInterceptor() {
    return new PageInterceptor();
  }
}
```


# 사용방법
JPA @Entity
```java
@Entity
public class EncTable {

  @Id
  private Long id;

  // 속성에 컨버너 추가 
  @Convert(converter = EncryptionConverter.class)
  @Column(name = "enc_data", length = 4096)
  private String enc_data;
}
```

Dao(mybatis interceptor)
```java
// 암호화 데이터가 있다는 annotation
@SecretData
public class EncTableDao {

  private Long id;
  
  // 실제 암호화 데이터 속성 annotation
  @EncryptedField
  private String encData;
}
```
