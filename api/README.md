# 자가진단 매크로 api

- api의 구조는 이 /api 폴더에 있습니다.
- api 구현체는 [/api-impl](../api-impl) 폴더에 있습니다.

## 대략적인 구조

어떤 개체에 대한 모델, 데이터, 구현체가 존재합니다. 예를 들어 사용자(User)라는 개체에 대해
[`UserModel`, `UserData`, `User`](src/main/kotlin/com/lhwdev/selfTestMacro/api/User.kt)가 존재합니다.

- `UserModel`는 사용자로써 기본으로 가져야 할 불변적인 속성을 정의합니다.
- `UserData`는 UserModel의 가장 소박한 구현체입니다. 딱히 상태나 구현이 따로 없고, 불변적인 데이터만 담은 클래스입니다. 이 변수는
  `@Serializable`입니다. 대부분의 경우 데이터베이스 호환성이 유지돼야 합니다.
- `User`는 UserModel의 화려한 구현체입니다. 사용자에 대해 가능한 동작을 정의해놓았습니다. (예를 들어 '자가진단 제출' 같은)
  실제 구현체인 `UserImpl`은 /api-impl 폴더에 있습니다. 이 부분은 자가진단 사이트의 내부 api 구조가 바뀌어도 비교적 영향을 많이 받지 않도록 설계되었습니다.
- `UserData`는 개체의 불변적인 속성만을 담았기 때문에, 임시적인 속성을 담지 못합니다. `User.Status`와 같은 일시적인 속성
  (예: 언제 자가진단을 제출했는지) 을 담은 값은 `User.status`에서 얻을 수 있습니다.

나머지 `Institute`, `UserGroup` 등도 마천가지입니다.
