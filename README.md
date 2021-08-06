# 코로나19 자가검진 매크로 앱 (안드로이드 전용)

예약해두면 매일 특정 시간에 자동으로 자가진단을 합니다.  
**이 앱을 사용하여 생기는 모든 문제의 책임은 이 앱의 사용자인 여러분에게 있습니다.**
건강상태가 좋지 않다면 매크로 예약을 취소해두고 공식 사이트나 앱에서 자가진단을 하시길 바랍니다.

**지금 사용자 정보를 등록한 지 며칠 지나면 자가진단이 안되는 현상이 있을 수도 있습니다. 며칠동안 테스트해보고 사용하시길 바랍니다.**
### [**📎 앱 다운로드 받기**](https://github.com/lhwdev/covid-selftest-macro/releases/latest/download/app-release.apk)  
- [개인정보 처리 방침](PRIVACY_POLICY.md)
- 이 앱을 공유할 때는 위 링크보다 이 사이트의 주소를 공유해주세요. (위의 링크는 바뀔 수도)  
- 자가진단 사이트의 구조가 바뀌거나 해서 앱이 작동하지 않을 때 알림을 받으려면 [디스코드 서버](https://discord.gg/a2hNMF39AC) 에 들어오세요.  

오류가 생기면 보통 몇 시간 안에 패치합니다.  
제작자 자신이 쓸려고 만든 앱이고 제작자의 지인들도 쓰고 있답니다.  
공식 사이트의 api 구조가 바뀌면 작동하지 않게 될 수 있습니다.
그런 경우 업데이트가 나오면 디스코드 서버에 공지할 겁니다.

참고: **새 학년이 시작될 때는 공식 앱이나 사이트에서 약관에 동의해야 합니다.**  
참고2: 구형 휴대폰(옛날 버전 안드로이드)에서는 잘 작동하지 않는 것 같습니다. 언젠간... 고치겠습니다.  


## 기능:
- **최근 가상 보안키보드 추가 대응**
- 버튼 한 개 클릭으로 자가진단
- 매일 일정 시각에 자가진단 예약


## 개발 중(아직 구현되지 않은 것):
- [ ] 범위 내 렌덤 시간 기능
- [ ] 주말, 공휴일에 자가진단 하지 않기 기능
- [x] 여러 명 그룹 기능


## 자가진단 API 관련 PoC
[이 파일을 참고하세요.](PoC.md)

## 개발자 분들을 위한 설명
이 앱은 안드로이드 기반으로, Kotlin과 Jetpack Compose를 이용하여 만들어졌습니다. 자가진단 api에 대한 백엔드는 `api`
폴더를 보면 될 겁니다.  
마스터 브랜치는 항상 만들다 만 것들이 올라올 예정이라서 그대로 가져다 쓰신다면 오류가 뜰 거에요. 개발을 도와주실 게
아니라면 태그로 가서 보고 싶은 버전을 선택하는 게 낫습니다.

- `master`: 신기능 개발, 최신버전
- `bug-fix`: 버그가 생겼을 때 수리, 이미 출시된 버전 중 가장 최신버전 기준

디자인에 관련해서는 컴포넌트를 만들기도 하지만 그냥 하나하나 스타일을 집어넣는 경우도 많아서 코드가 조금 더럽습니다.
대규모 프젝도 아니라서 그냥 그렇게 했답니다..!

이 코드의 일부분은 노출될 경우 곤란합니다. 따라서 transkey 폴더는 이 저장소에 올라와있지
않습니다. 코드가 필요하신 분은 따로 연락주세요.


## 연락 & 버그 제보
[깃허브 커뮤니티](https://github.com/lhwdev/covid-selftest-macro/discussions)  
개인 이메일: lhwdev6@outlook.com (최대한 버그가 있어서 작동하던 앱이 작동하지 않게 된 경우에만 보내주세요)  
[디스코드 서버](https://discord.gg/a2hNMF39AC): 업데이트 공지나 버그 제보

## 오픈소스 라이센스
- [mTranskey](https://github.com/Nua07/mTransKey)
- Special thanks to [blluv](https://github.com/blluv)

