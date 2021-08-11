# 자가진단 API 관련 PoC
코틀린 기반 구현체는 api 폴더에서 찾아볼 수 있습니다. 단 파일의 이름을 이해하기 쉽도록 바꾸어서
실제 api의 주소와 다를 수 있습니다.

교육청의 가상 보안키보드 패치 이후 추가된 transkey 관련 PoC는 이곳에 없습니다.
쓸 시간도 없고, 보안상의 이유로 공개하지 않습니다.

## 토큰
여기서는 토큰을 세가지 종류, UsersIdToken, UsersToken, UserToken으로 구분하도록 하겠습니다. (제가 붙인 이름임)
이름 그대로 'users-'로 시작하는 토큰은 사용자 그룹에 대한 토큰입니다.
각 토큰은 jwt 형식으로 `Bearer xxx.yyy.zzz`의 형식을 띕니다. (xxx, yyy, zzz는 Base64 인코딩됨)  

## 기본 url
공통 url: `https://hcs.eduro.go.kr`
시도 교육청 url: `https://???hcs.eduro.go.kr`  
추가: 관리자 url: `https://???hcm.eduro.go.kr`

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
    "kraOrgNm": "<기관 이름>",
    "orgCode": "<기관 코드>",
    "addres": "<주소>",
    "atptOfcdcConctUrl": "<시도 교육청 url>",
    // ...
  }
  ```

#### url 쿼리:
- 학교
  * `loginType`: 고정값 `school`
  * `orgName`: 학교 이름
  * `lctnScCode`: 지역 코드(하단 표 참고)
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
  {
    "cdcValueNm": "<시/도 이름>",
    "cdcValueAbrvNm": "<시/도 이름 줄임>",
    "upperCdcValue": "<시/군/구 코드>",
    // ...
  }
  ```

#### url 쿼리:
* `queryUrlParamsToString`: 고정값 `SIG_CODE`
* `upperClsfCodeValue`: 고정값 `LCTN_SC_CODE`
* `stateKey`: 고정값 `sigCodes`
* `upperCdcValue`: 지역 코드(하단 표 참고)

### 대표 사용자 정보로 사용자 그룹 찾기
* 주소: HTTP POST, `시도 교육청 url/v2/findUser`
* 헤더:
  - `Content-Type: application/json;charset=utf-8`
* 입력: json
  ```json5
  {
    "orgCode": "<기관 코드>",
    "name": "<사용자 이름/암호화>",
    "birthday": "<생일: 6자리/암호화>",
    "loginType": "school/univ/office"
    // "stdntPNo": <페이지 번호> // 보통 필요없음
  }
  ```

* 결과: json
  ```json5
  {
    "userName": "<사용자 이름>",
    "token": "<UsersIdToken>",
    "stdntYn": "Y/N",
    // ...
  }
  ```

### 비밀번호 확인 및 로그인
* 주소: HTTP POST, `시도 교육청 url/v2/validatePassword`
* 헤더:
  - `Content-Type: application/json;charset=utf-8`
  - Authorization: **UsersIdToken**
  - 쿠키: `WAF`가 있어야 작동함 (hcs 사이트에 있는 것들 중 아무거나 호출해도 `Set-Cookie`를
    반환하기 때문에 문제없음)
* 입력: json
  ```json5
  {
    "password": "<transkey>", // transkey의 결과.
    "makeSession": true,
    "deviceUuid": "<기기 uuid>" // 공식 앱 사용시에 들어감, ""로 비워둬도 됨
  }
  ```
* 출력:
  - 로그인 성공 시: `"<UsersToken>"` (쌍따옴표 " " 포함)
  - 실패 시: json
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

### 그룹의 사용자 목록 보기
여기 아래로는 `WAF`, `_JSESSIONID` 같은 쿠키가 있어야 잘 작동하는 것 같습니다.

* 주소: HTTP POST, `시도 교육청 url/v2/selectUserGroup`
* 헤더:
  - `Content-Type: application/json;charset=utf-8`
  - Authorization: **UsersToken**
* 입력: json `{}` (말 그대로 빈 json object)
* 출력: json
  ```json5
  [
    {
      "orgCode": "D000000000", // 기관 코드,
      "orgName": "<학교 이름>",
      "userPNo": "<사용자 id>",
      "stdntYn": "Y/N", // 학생 여부
      // ...
      "token": "<UserToken>"
    },
    // 그룹에 속한 다른 사용자들 ...
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
		"orgCode": "<기관 코드>",
		"userPNo": "<사용자 id>"
	}
	```
* 출력: json
  ```json5
  {
	  "admnYn": "N",
	  "atptOfcdcConctUrl": "dgehcs.eduro.go.kr",
	  "deviceUuid": "3b...",
	  "insttClsfCode": "5",
	  "isHealthy": true,
	  "lctnScCode": "03",
	  "lockYn": "N",
	  "mngrClassYn": "N",
	  "mngrDeptYn": "N",
	  "orgCode": "D????????",
	  "orgName": "??고등학교",
	  "pInfAgrmYn": "Y",
	  "registerDtm": "2020-10-21 07:05:43.187088",
	  "registerYmd": "20201021",
	  "schulCrseScCode": "4",
	  "stdntYn": "Y",
	  "token": "Bearer ey.....",
	  "upperUserName": "홍길동",
	  "userName": "홍길동",
	  "userNameEncpt": "홍길동",
	  "userPNo": "...",
	  "wrongPassCnt": 0
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
    "rspns00": "Y",
    "rspns01": "1",
    "rspns02": "1",
    "rspns03": null,
    "rspns04": null,
    "rspns05": null,
    "rspns06": null,
    "rspns07": null,
    "rspns08": null,
    "rspns09": "0",
    "rspns10": null,
    "rspns11": null,
    "rspns12": null,
    "rspns13": null,
    "rspns14": null,
    "rspns15": null,
    "upperToken": "<UserToken>",
    "upperUserNameEncpt": "홍길동" // 제출한 사람 이름. 공식 사이트에서는 다른 사람이 대신 제출한 경우 그 이름이 들어가나, 실제로는 아무 이름이나 넣어도 됨
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
      "mobnuEncpt":"<학생 휴대전화 번호>"
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
