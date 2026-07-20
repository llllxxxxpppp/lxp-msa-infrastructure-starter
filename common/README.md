# common

모든 마이크로서비스에서 공유할 DTO 객체들을 모아놓은 공통 모듈 프로젝트입니다.

## import 방법

각 마이크로서비스 프로젝트에서 Gradle의 includeBuild 방식으로 common 모듈을 포함해주세요.

### settings.gradle

`settings.gradle` 파일에는 다음 구문을 추가합니다.

```groovy
includeBuild '../common'
```

### build.gradle

`build.gradle` 파일의 `dependencies`에 다음과 같이 `implementation`을 추가합니다.

```groovy
dependencies {
    implementation 'com.lcs:common'
}
```

## 테스트 방법

아무런 클래스도 포함되어 있지 않은 현재 초기 common 모듈은 테스트를 위한 `com.lcs.common.CommonTestData` 클래스만 존재합니다.

위의 import 방법에 따라 두 구문을 적절히 포함한 후 마이크로서비스에서 `CommonTestData` 객체를 사용할 수 있다면 정상적으로 common 모듈이 포함된 것입니다.
