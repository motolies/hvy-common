package kr.hvy.common.infrastructure.database.mybatis.interceptor;

import kr.hvy.common.application.crypto.annotation.EncryptedField;
import kr.hvy.common.application.crypto.annotation.SecretData;

/** 인터셉터 테스트용 더미 DTO. id 는 비대상, name·tel 은 암호화 대상이다. */
@SecretData
class TestPerson {

  String id;
  @EncryptedField
  String name;
  @EncryptedField
  String tel;

  TestPerson(String id, String name, String tel) {
    this.id = id;
    this.name = name;
    this.tel = tel;
  }
}
