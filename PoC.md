# 자가진단 API 관련 PoC
코틀린 기반 구현체는 api 폴더에서 찾아볼 수 있습니다. 단 파일의 이름을 이해하기 쉽도록 바꾸어서
실제 api의 주소와 다를 수 있습니다.

아래 나오는 모든 api의 경우 response header는 `X-Client-Version`을 포함합니다. 나중에
이 정보를 저장해두었다가 자가진단 제출을 할 때(`/registerServey`) 쓰면 됩니다.

교육청의 가상 보안키보드 패치 이후 추가된 transkey 관련 PoC는 이곳에 없습니다.
쓸 시간도 없고, 보안상의 이유로 공개하지 않습니다.

## 토큰
여기서는 토큰을 UsersToken, UserToken으로 구분하도록 하겠습니다. (제가 붙인 이름임)
이름 그대로 'users-'로 시작하는 토큰은 사용자 그룹에 대한 토큰입니다. UserToken의 유효기간은 약 2주 정도라고 하네요.
~~각 토큰은 jwt 형식으로 `Bearer xxx.yyy.zzz`의 형식을 띕니다. (xxx, yyy, zzz는 Base64 인코딩됨)~~
예전 버전에는 그랬는데 이제 아니네요.

## 기본 url
- 공통 url: `https://hcs.eduro.go.kr`
- 시도 교육청 url: `https://???hcs.eduro.go.kr`
- 관리자 url: `https://???hcm.eduro.go.kr`

## 암호화
'암호화'라 쓰인 곳: `RSA/ECB/PKCS1Padding`으로 암호화
공개키:
- modulus: `30718937712611605689191751047964347711554702318809238360089112453166217803395521606458190795722565177328746277011809492198037993902927400109154434682159584719442248913593972742086295960255192532052628569970645316811605886842040898815578676961759671712587342568414746446165948582657737331468733813122567503321355924190641302039446055143553127897636698729043365414410208454947672037202818029336707554263659582522814775377559532575089915217472518288660143323212695978110773753720635850393399040827859210693969622113812618481428838504301698541638186158736040620420633114291426890790215359085924554848097772407366395041461`
- publicExponent: `65537`

## 기본 API 목록
### 기관 정보 가져오기
* 주소: HTTP GET, `공통 url/v2/searchSchool` + url 쿼리
* 결과: json
  ```json5
  {
    "key": "<SearchKey>",
    "schulList": [
      {
        "kraOrgNm": "<기관 이름>",
        "orgCode": "<암호화된 가변 기관 코드>",
        "juOrgCode": "<기본 기관 코드>",
        "addres": "<주소>",
        "atptOfcdcConctUrl": "<시도 교육청 url>",
        // ...
      },
      // ...
    ]
  }
  ```

  참고로 `<SearchKey>`의 유효기간은 약 2분입니다.

#### url 쿼리:
( ): 안써도 됨

- 학교
  * `loginType`: 고정값 `school`
  * `orgName`: 학교 이름 (2글자 이상; 이제 api단에서 길이가 검증됨)
  * (`lctnScCode`: 지역 코드(하단 표 참고))
  * `schulCrseScCode`: 햑교 단계 코드(하단 표 참고)
- 대학
  * `loginType`: 고정값 `univ`
  * `orgName`: 학교 이름
- 교육행정기관
  * `loginType`: 고정값 `office`
  * `orgName`: 기관 이름
- 학원
  * `loginType`: 고정값 `office`
  * `isAcademySearch`: 고정값 `true`
  * `lctnScCode`: 지역 코드(하단 표 참고)
  * `sigCode`: 시/군/구 코드(시/군/구 코드 api 참고)
  * `orgName`: 학원 이름

### 시/군/구 코드 가져오기
* 주소: HTTP GET, `공통 url/v2/getMinors` + url 쿼리
* 결과: json
  ```json5
  [
    {
      "cdcValueNm": "<시/도 이름>",
      "cdcValueAbrvNm": "<시/도 이름 줄임>",
      "upperCdcValue": "<시/군/구 코드>",
      // ...
    },
    // ...
  ]
  ```

#### url 쿼리:
* `queryUrlParamsToString`: 고정값 `SIG_CODE`
* `upperClsfCodeValue`: 고정값 `LCTN_SC_CODE`
* `stateKey`: 고정값 `sigCodes`
* `upperCdcValue`: 지역 코드(하단 표 참고)

### 대표 사용자 정보로 사용자 그룹 찾고 로그인하기
* 주소: HTTP POST, `시도 교육청 url/v3/findUser`
* 헤더:
  - `Content-Type: application/json;charset=utf-8`
  - 쿠키: `WAF`가 있어야 작동함 (hcs 사이트에 있는 것들 중 아무거나 호출해도 `Set-Cookie`를
    반환하기 때문에 문제없음)
  
* 입력: json
  ```json5
  {
    "orgCode": "<암호화된 가변 기관 코드>",
    "orgName": "<기관 이름>",
    "name": "<사용자 이름/암호화>",
    "birthday": "<생일: 6자리/암호화>",
    "loginType": "school/univ/office",
    "searchKey": "<SearchKey>", // searchSchool 등에서 받은 정보
    "deviceUuid": "", // 기기 uuid를 넣어도 됨
    "lctnScCode": "<학교 단계 코드>",
    "makeSession": true,
    "password": "<transkey>",
    // "stdntPNo": <페이지 번호> // 보통 필요없음
  }
  ```
  ~~`<transkey>` 부분은 [여기를 참고하세요.](https://github.com/lhwdev/covid-selftest-macro-transkey/blob/master/PoC.md)~~
  지금 만들고는 있는데 한참 걸릴겁니다. 차라리 transkey 소스코드를 보세요.

* 
  - 로그인 성공 시: json
    ```json5
    {
      "token": "<UsersToken>", // Bearer (hex) 형태
      "pInfAgrmYn": "Y/N", // 약관 동의 여부,
      "hasPassword": true //비번 유무 여부
      // 기타 등등
    }
    ```
  - 실패 시: json
    * 사용자 정보(이름, 생년월일)이 틀릴 결루
      ```json5
      {"statusCode":1101}
      ```
    
    * `<SearchKey>`가 만료되었을 경우
      ```json5
      {
        "isError": true,
        "message": "학교 찾기 후 입력시간이 초과되었습니다"
      }
      ```
    
    * 비밀번호가 틀릴 경우
      ```json5
      {
        "isError": true,
        "statusCode": 252, // 숫자
        "errorCode": 1001, // 오류 코드: 하단 표 참고
        "data": {
          "failCnt": 1, // 비밀번호 틀린 횟수
          "canInitPassword": false // 비밀번호 초기화 가능 여부
        }
      }
      ```
      
  - 헤더
    - `Set-Cookie: _JSESSIONID=...`

  - 오류 코드
    - 1000: 비밀번호를 5회 틀림
    - 1001: 비밀번호가 맞지 않음
    - 1003: 비밀번호가 초기화됨

### 로그인 정보가 남아있는 상태에서 다시 로그인

* 주소: HTTP POST, `시도 교육청 url/v2/validatePassword`
* 헤더:
  - `Content-Type: application/json;charset=UTF-8`
  - Authorization: **UsersToken** (기존에 발급받은 UsersToken)
* 입력: json (findUser이랑 일부분 공유)
  ```json5
  {
    "deviceUuid": "", // 기기 uuid를 넣어도 됨
    "makeSession": true,
    "password": "<transkey>"
  }
  ```
* 결과: findUser이랑 유사함, 새로운 UsersToken을 발급받음
  그런데 이걸 굳이 쓸 필요가..? 토큰 유효기간이 짧은가..?

### 비밀번호 존재 유무 확인

* 주소: HTTP POST, `시도 교육청 url/v2/hasPassword`
* 헤더:
  - `Content-Type: application/json;charset=utf-8`
  - Authorization: **UsersToken**
* 입력: json
  ```json5
  {}
  ```

* 결과: json
  ```json5
  true
  ```
  
### 그룹의 사용자 목록 보기
여기 아래로는 `WAF`, `_JSESSIONID` 같은 쿠키가 있어야 잘 작동하는 것 같습니다.

* 주소: HTTP POST, `시도 교육청 url/v2/selectUserGroup`
* 헤더:
  - `Content-Type: application/json;charset=utf-8`
  - Authorization: **UsersToken**
* 입력: json
  ```json5
  {}
  ```
* 출력: json
  ```json5
  [
    { // 메인 사용자
      "orgCode": "D000000000", // 불변 기관 코드,
      "orgName": "<학교 이름>",
      "userPNo": "<사용자 id>",
      "userNameEncpt": "<사용자 이름>",
      "stdntYn": "Y/N", // 학생 여부
      "mngrYn": "Y/N", // 관리자 여부
      "otherYn": "N", // 메인 학생 여부
      "atptOfcdcConctUrl": "<자가진단 시/도 url>",
      // ...
      "token": "<UserToken>"
    },
    // 여기서부터는 한 계정에 여러명을 등록할 경우
    {
      "orgCode": "D000000000", // 불변 기관 코드,
      "userPNo": "<사용자 id>",
      "otherYn": "Y", // 메인 학생 여부
      "atptOfcdcConctUrl": "<자가진단 시/도 url>",
      // ...
      "token": "<UserToken>"
    }
    // ...
  ]
  ```
### 사용자 정보 자세히 보기
* 주소: `시도 교육청 url/getUserInfo`
* 헤더:
  - `Content-Type: application/json;charset=utf-8`
  - Authorization: **UserToken**
* 입력: json
  ```json5
  {
    "orgCode": "<불변 기관 코드>",
    "userPNo": "<사용자 id>"
  }
  ```
* 출력: json
  ```json5
  {
    "admnYn": "N",
    "atptOfcdcConctUrl": "dgehcs.eduro.go.kr",
    "deviceUuid": "3b...",
    "extSurveyCount": 0,
    "extSurveyRemainCount": 0,
    "insttClsfCode": "5",
    "isHealthy": true, // 일부 시도에서는 이 항목이 없다는 말을 들었습니다
    "isIsolated": false,
    "lctnScCode": "03",
    "lockYn": "N",
    "mngrClassYn": "N",
    "mngrDeptYn": "N",
    "orgCode": "D????????",
    "orgName": "??고등학교",
    "newNoticeCount": 0,
    "pInfAgrmYn": "Y",
    "stdntYn": "Y",
    "token": "Bearer 0156DA.....",
    "upperUserName": "홍길동",
    "userName": "홍길동",
    "userNameEncpt": "홍길동",
    "userPNo": "...",
    "wrongPassCnt": 0,
    
    // 자가진단 안하면 없음
    "registerDtm": "2020-10-21 07:05:43.187088",
    "registerYmd": "20201021",
    "rspns01": "1",
    "rspns02": "1",
    "rspns03": "0",
    "rspns05": "0",
    "rspns07": "0",
    "rspns08": "0",
    "rspns09": "0"
  }
  ```

### 자가진단 제출
* 주소: `시도 교육청 url/registerServey` (오타가 아님) (인데 오타가 맞음)
* 헤더:
  - `Content-Type: application/json;charset=utf-8`
  - Authorization: **UserToken**
* 입력: json
  ```json5
  {
    "deviceUuid": "",
    "rspns00": "Y", // 어느 하나라도 문제가 있어서 등교가 불가능하면(1, 3번 '예' 또는 2번 '양성) N, 아니면 Y
    "rspns01": "1", // 1. 학생 본인이 코로나19 감염에 의심되는 아래의 임상증상이 있나요?: "1"=아니오, "2"=예
    "rspns02": "1", // 3. 학생 본인 또는 동거인이 PCR 검사를 받고 그 결과를 기다리고 있나요?: "1"=아니오, "0"=예
    "rspns03": "1", // 2. 학생은 오늘 신속항원검사(자가진단)를 실시했나요?: "1"=실시하지 않음, null=실시함
    "rspns04": null,
    "rspns05": null,
    "rspns06": null,
    "rspns07": null, // 2. 학생은 오늘 신속항원검사(자가진단)를 실시했나요?: null=실시하지 않음, "0"=음성, "1"=양성
    "rspns08": null,
    "rspns09": null,
    "rspns10": null,
    "rspns11": null,
    "rspns12": null,
    "rspns13": null,
    "rspns14": null,
    "rspns15": null,
    "upperToken": "<UserToken>",
    "upperUserNameEncpt": "홍길동", // 제출한 사람 이름. 공식 사이트에서는 다른 사람이 대신 제출한 경우 그 이름이 들어가나, 실제로는 아무 이름이나 넣어도 됨
    "clientVersion": "1.9.2" // 메뉴 바에서 뜨는 UI ver. 버전에 대응되고 X-Client-Version 헤더 값을 넣으면 됨.
    // clientVersion이 일치하지 않으면 자가진단을 거부함.
  }
  ```
* 출력: json
  ```json5
  {"registerDtm":"2021-02-19 23:13:26", "inveYmd":"2021-02-19 23:13:26"} // 자가진단 제출 시기
  ```


## 관리자용 API 목록
선생님 등 기관의 관리자가 사용하는 api입니다.
현재 이 목록에는 학교 관련 api만 있습니다.

### 관리하는 반 목록 보기
* 주소: `시도 교육청 url/joinClassList`
* 헤더:
  - Authorization: **UserToken**
* 출력: json
```json5
  {
    "classList": [
      {
        "orgCode": "<기관 코드>",
        "grade": "1", // 학년
        "classNm": "1", // 반 번호
        "classCode": "01", // 반 코드
        "ay": "2020" // 뭔지 몰라도 암튼 연도
        // ...
      }
      // 다른 반들
    ]
  }
```

### 관리하는 반의 학생 목록 보기
* 주소: `시도 교육청 url/join`
* 헤더:
  - Authorization: **UserToken**
* 입력: json -- `/joinClassList`의 결과값인 반 목록 중 하나.. 이지만 `orgCode`, `grade`, `classNm`, `classCode`만 있어도 작동함
  ```json5
  {"orgCode":"D000000000","grade":"1","classNm":"1","classCode":"01"} // 공식 사이트에서 입력은 더 긺
  ```
* 출력: json
```json5
  {
    "joinList": [ // 반 목록
      {
        "orgCode":"Xnnnnnnnnnn",
        "inveYmd":"YYYYMMDD",
        "grade":"<학년>",
        "classCode":"<반 코드>",
        "serveyTime":"<숫자(??)>",
        "name":"<반 이름>",
        "userPNo":"<사용자 id>", // /??
        "surveyYn":"Y/N",
        "rspns00":"Y/N",
        "deviceUuidYn":"Y/N", // 공식앱 설치 여부
        "registerDtm":"YYYY-MM-DD HH:MM:SS.ssssss", // 자가진단 시기
        "stdntCnEncpt":"n",
        "upperUserName":"<also name>"
      },
      // ...
    ]
  }
```

### 한 학생의 자가진단 참가 현황 보기
* 주소: `시도 교육청 url/joinDetail`
* 헤더:
  - `Content-Type: application/json;charset=utf-8`
  - Authorization: **UserToken**
* 입력: `/join`에서 받은 학생 정보 중 하나를 그대로 입력
* 출력: json
  ```json5
  {
    "joinInfo": {
      "grade":"<학년>",
      "classCode":"<반 코드>",
      "name":"<이름>",
      "surveyYn":"Y/N", // 자가진단 제출 여부
      "isHealthy": false, // '정상' 여부
      "atptOfcdcConctUrl":"???hcs.eduro.go.kr", // 시도교육청 주소
      "pInfAgrmYn":"Y/N",
      "mobnuEncpt":"<학생 휴대전화 번호(base64 인코딩됨)>"
    }
  }
  ```


## 지역 코드
- 서울: `01`
- 부산: `02`
- 대구: `03`
- 인천: `04`
- 광주: `05`
- 대전: `06`
- 울산: `07`
- 세종: `08`
- 경기: `10`
- 강원: `11`
- 충북: `12`
- 충남: `13`
- 전북: `14`
- 전남: `15`
- 경북: `16`
- 경남: `17`
- 제주: `18`

## 학교 단계 코드
- 유치원: `1`
- 초등학교: `2`
- 중학교: `3`
- 고등학교: `4`
- 특수학교: `5`
