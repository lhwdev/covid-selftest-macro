# 코로나19 자가진단 매크로 앱 (안드로이드 전용)
[![Download](https://img.shields.io/github/downloads/lhwdev/covid-selftest-macro/latest/total?label=%EB%8B%A4%EC%9A%B4%EB%A1%9C%EB%93%9C%20%EB%B0%9B%EA%B8%B0&style=for-the-badge)](https://github.com/lhwdev/covid-selftest-macro/releases/latest/download/app-release.apk)
[![Discord](https://img.shields.io/discord/868429217740783637?label=%EA%B3%B5%EC%8B%9D%20%EB%94%94%EC%BD%94%EB%B0%A9&style=for-the-badge&color=5865F2)](https://discord.gg/a2hNMF39AC)
[![Email](https://img.shields.io/badge/%EC%9D%B4%EB%A9%94%EC%9D%BC-%EA%B0%9C%EC%9D%B8-orange?style=for-the-badge)](mailto:lhwdev6@outlook.com)
[![covid-hcs](https://img.shields.io/badge/organization-covid--hcs-2962ff?style=for-the-badge)](https://github.com/covid-hcs)

> 예약해두면 매일 특정 시간에 자동으로 자가진단을 합니다.  
  **이 앱을 사용하여 생기는 모든 문제의 책임은 이 앱의 사용자인 여러분에게 있습니다.**
  건강상태가 좋지 않다면 매크로 예약을 취소해두고 공식 사이트나 앱에서 자가진단을 하시길 바랍니다.

### [**📎 앱 다운로드 받기**](https://github.com/lhwdev/covid-selftest-macro/releases/latest/download/app-release.apk)  
- [개인정보 처리 방침](PRIVACY_POLICY.md)
- 이 앱에 100% 의존하기보다는, 평상시에 일찍 일어나는 습관을 기르고(?), 가끔 늦게 일어나거나 까먹었을 때 대신 해주는 수단으로
  이용하는 게 안전합니다. 이 앱은 아무래도 매크로라는 특성 때문에 직접 자가진단하는 것보다 안정성이 떨어질 수 밖에 없습니다.
- 이 앱을 공유할 때는 위 링크보다 이 사이트의 주소를 공유해주세요. (위의 링크는 바뀔 수도)  
- 자가진단 사이트의 구조가 바뀌어서 앱이 작동하지 않거나 업데이트가 있을 때 알림을 받으려면 [디스코드 서버](https://discord.gg/a2hNMF39AC)에 들어오세요.  

오류가 생기면 보통 몇 시간 안에 패치합니다.  
제작자 자신이 쓸려고 만든 앱이고 제작자의 지인들도 쓰고 있답니다.  
공식 사이트의 api 구조가 바뀌면 작동하지 않게 될 수 있습니다.
그런 경우 업데이트가 나오면 디스코드 서버에 공지할 겁니다.

참고: **새 학년이 시작될 때는 공식 앱이나 사이트에서 약관에 동의해야 합니다.**  
[iOS 사용자 분의 경우 이걸 써주세요.](https://github.com/ChemistryX/self-diagnosis-ios-shortcuts)  

## 참고 사항 & 알려진 버그
- **데이터를 통해서 진단할 때에는 안될 수도 있습니다**. 이 앱은 원래 와이파이로 작동되는 것을 의도하고 만들었기에,
  작동하게 하려면 설정을 조금 바꿔줘야 합니다. 앱 아이콘 길게 클릭(홈화면에서) > 설정 > 데이터 네트워크 >
  `백그라운드 데이터 사용 허용`, `데이터 절약 모드 미절약 앱`을 둘 다 켜주세요.
- 한 계정에 사용자 여러명을 등록하면 로그인이 되지 않습니다. 이 버그는 가능한 한
  빨리 고치겠습니다.
- 일부 휴대폰 기종에서는 잘 작동하지 않는 것 같습니다. (지금 유효한 버그인지 모르겠네요.)


## 기능:
- **최근 가상 보안키보드 추가 대응**
- 버튼 한 개 클릭으로 자가진단
- 매일 일정 시각에 자가진단 예약


## 개발 중(아직 구현되지 않은 것):
- [x] 범위 내 랜덤 시간 기능
- [x] 주말에 자가진단 하지 않기 기능
- [x] 여러 명 그룹 기능

자세한 개발 목표/현황은 [디스코드 방의 이 채널](https://discord.gg/M3rjvnDNAJ)을 봐주세요.

## 개발자 분들을 위한 설명

**이 코드들을 사용함으로써 오는 책임은 이 코드를 이용한 개발자에 있습니다.**

**코틀린 자가진단 API**는 api 폴더를 참고하세요.  
최대한 많은 api를 추가하고 있고, 관리자(교사)용 api, 비밀번호 바꾸기 등 api도 있습니다.  
아직 maven 같은데 따로 올리진 않았고, 직접 가져다 쓰세요. 언젠가 올리고 싶습니다. 그리고 각 커밋 사이에 호환성은 보장하지 않습니다.  
자가진단 앱의 버전은 api의 버전과는 관계가 없습니다.

**자가진단 API 관련 PoC**는 [이 파일을 참고하세요.](PoC.md)
자가진단 내부 api는 소리소문없이 바뀌기 때문에 옛날 정보가 있을 수도 있습니다.

이 앱은 안드로이드 기반으로, Kotlin과 Jetpack Compose를 이용하여 만들어졌습니다.  
마스터 브랜치는 항상 만들다 만 것들이 올라올 예정이라서 그대로 가져다 쓰신다면 오류가 뜰 거에요. 개발을 도와주실 게 아니라면 태그로 가서 보고 싶은 버전을 선택하는 게 낫습니다.

- `master`: 신기능 개발, 최신버전
- `bug-fix`: 버그가 생겼을 때 수리, 이미 출시된 버전 중 가장 최신버전 기준

디자인에 관련해서는 컴포넌트를 만들기도 하지만 그냥 하나하나 스타일을 집어넣는 경우도 많아서 코드가 조금 더럽습니다. 대규모 프젝도 아니라서 그냥 그렇게 했답니다..!

~~이 코드의 일부분은 노출될 경우 곤란합니다. 따라서 transkey 폴더는 이 저장소에 올라와있지
않습니다. 코틀린 구현체가 필요하신 분은 따로 연락주세요.~~ [이제 별 문제 없을 거 같아서 그냥 공개했습니다.](https://github.com/lhwdev/covid-selftest-macro-transkey)
클론할 때는 저 리포를 클론하지 마시고 이 리포를 `--recurse-submodules`와 함께 클론하면 됩니다.  
예시 명령줄:
```shell
git clone --recurse-submodules https://github.com/lhwdev/covid-selftest-macro
```

참고로 이미 [파이썬으로 만들어진 자가진단 구현체](https://github.com/331leo/hcskr_python)가 있습니다.
다른 언어의 경우 위 구현체를 직접 포팅해주세요.

## 연락 & 버그 제보
[디스코드 서버](https://discord.gg/a2hNMF39AC): 업데이트 공지나 버그 제보  
[깃허브 커뮤니티](https://github.com/lhwdev/covid-selftest-macro/discussions)  
개인 이메일: lhwdev6@outlook.com (최대한 버그가 있어서 작동하던 앱이 작동하지 않게 된 경우에만 보내주세요)  

## 오픈소스 라이센스 & 기타
- [mTranskey](https://github.com/Nua07/mTransKey)
- Special thanks to [blluv](https://github.com/blluv), 이승수
- App icon by Gradient

