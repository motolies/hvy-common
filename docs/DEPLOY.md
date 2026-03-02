# 배포 설정하기

- [Maven Central Repository에 gradle사용해서 배포하기](https://dami97.tistory.com/36)

## 1. gpg 키 생성

- [gpg 설치 및 키 생성](https://mangkyu.tistory.com/237)

```bash
# gpg 설치
brew install gpg

# 키 생성 명령어
gpg --gen-key

# 키 확인 명령어
gpg --list-secret-keys --keyid-format LONG

# 공개키 등록
gpg --keyserver keyserver.ubuntu.com --send-keys ABCDEF1234567890

# 만약 키서버를 못 찾으면
# https://stackoverflow.com/questions/67251078/gpg-keyserver-send-failed-no-keyserver-available-when-sending-to-hkp-pool
gpgconf --kill dirmngr
dirmngr --debug-all --daemon --standard-resolver

# 공개키 등록 후 singing.gpg 파일 생성
gpg --export-secret-keys ABCDEF1234567890 > signing.gpg
```

## 2. Create Properties file
프로젝트 루트에 아래 파일을 생성한다.
<br/>
**gradle.properties**
```properties
## maven central account 토큰 정보
# 공식 사이트의 토큰 username(example)
mavenCentralUsername=aBcDeFgh
# 공식 사이트의 토큰 password(example)
mavenCentralPassword=a1234567890b/z1234567890qwertyui+asdfghjkzxcv

## gpg 관련
# key 뒤 8자리 값
signing.keyId=ABCD1234
# key 발급 시 입력한 gpg password
signing.password=gpg+key+password
# gpg export 한 파일 절대경로 ex) C:/dev/signing.gpg
signing.secretKeyRingFile=/Users/nwkim/.gnupg/signing.gpg

## 버전관리
publishVersion=1.0.0
```

## 3. Build And Publish

### 3.1 Maven Central 배포
`publishToMavenCentral(true)` 설정으로 자동 릴리스가 활성화되어 있으므로,
배포 후 Maven Central 사이트에서 별도로 Publish 버튼을 누를 필요 없이 자동으로 릴리스된다.

```shell
./gradlew publishAllPublicationsToMavenCentralRepository

# 아래처럼 버전을 추가해야 한다
./gradlew publishAllPublicationsToMavenCentralRepository -PpublishVersion=1.0.0
```

### 3.2 버전 관리
`gradle.properties` 파일을 사용한다.
```properties
# 아래에 실제 배포 버전을 적는다
# 단 로컬에서는 주석처리 하고 실행한다.
publishVersion=1.0.0
```

### 3.3 로컬 배포 (개발 환경)
로컬 저장소(`~/.m2/repository`)에 배포하여 다른 모듈에서 개발 중인 버전을 테스트할 수 있다.
`publishVersion`을 지정하지 않으면 버전이 `local`로 설정된다.

```shell
./gradlew publishToMavenLocal
```

배포 후 사용하는 모듈(예: hvy-blog)의 `build.gradle`에서 다음과 같이 사용한다:
```groovy
repositories {
    mavenLocal()  // 로컬 저장소 우선 탐색
    mavenCentral()
}

dependencies {
    // 로컬 개발 시 (env != prod)
    implementation('io.github.motolies:hvy-common:local') { ... }

    // 프로덕션 배포 시 (env == prod)
    implementation('io.github.motolies:hvy-common:0.1.19') { ... }
}
```

## 4. 플러그인 안내 (v0.34.0+)
`com.vanniktech.maven.publish` 0.34.0부터는 `maven-publish`, `signing` 플러그인을 별도로 선언할 필요 없다.
`mavenPublishing` 블록이 서명(signing)과 배포(publishing)를 모두 내부적으로 처리한다.

```groovy
// build.gradle
plugins {
    id "com.vanniktech.maven.publish" version "0.34.0"  // maven-publish, signing 내장
}

mavenPublishing {
    signAllPublications()        // GPG 서명 (signing 플러그인 불필요)
    publishToMavenCentral(true)  // Central Portal + 자동 릴리스
    // ...
}
```

## 5. gpg 키 백업 및 복원

- gpg 키를 백업한다.
```bash
# 공개키 내보내기
gpg --list-keys
gpg --export -a "사용자 이메일" > public_key.asc

# 비밀키 내보내기
gpg --list-secret-keys
sudo gpg --export-secret-keys -a "사용자 이메일" > private_key.asc

# 비밀키 암호화
# 암호화를 하면 private_key.asc.gpg 파일이 생성된다.
sudo gpg --symmetric --cipher-algo AES256 private_key.asc

```

- gpg 키를 복원한다.
```bash
# gpg 설치
brew install gpg

# 공개키 복원
gpg --import public_key.asc

# 비밀키 복호화
gpg --decrypt private_key.asc.gpg > private_key.asc

# 비밀키 복원
gpg --import private_key.asc

# singing.gpg 파일 생성
gpg --export-secret-keys ABCDEF1234567890 > signing.gpg
```

- [가져온 키 확인 및 오류 해결](https://rainbow-flavor.tistory.com/11)
```bash
# 키를 import 했다면 아래 명령어를 실행해봅니다.
echo "test" | gpg --clearsign

# 만약 다음과 같은 오류가 발생한다면
gpg: signing failed: Inappropriate ioctl for device
gpg: [stdin]: clear-sign failed: Inappropriate ioctl for device

# .zshrc 파일 혹은 .bashrc 파일에 다음 줄을 추가 후 다시 명령을 실행해줍니다.
echo 'export GPG_TTY=$(tty)' >> ~/.zshrc

# 다시 명령을 실행해봅니다.
# 정상적으로 실행이 되면 PassPhrase를 입력하라고 나올 것입니다.
```
