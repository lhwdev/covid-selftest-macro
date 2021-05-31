package com.lhwdev.selfTestMacro

import java.net.URL


val sCommonUrl = URL("https://hcs.eduro.go.kr/v2")

val sDefaultFakeHeader = mapOf(
	"User-Agent" to "Mozilla/5.0 (Linux; Android 7.0; SM-G892A Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/85.0.4183.81 Mobile Safari/537.36",
	"X-Requested-With" to "XMLHttpRequest",
)

val sRegions = mapOf(
	"01" to "서울", "02" to "부산", "03" to "대구", "04" to "인천", "05" to "광주", "06" to "대전",
	"07" to "울산", "08" to "세종", "10" to "경기", "11" to "강원", "12" to "충북",
	"13" to "충남", "14" to "전북", "15" to "전남", "16" to "경북", "17" to "경남", "18" to "제주"
)

val sSchoolLevels = mapOf(1 to "유치원", 2 to "초등학교", 3 to "중학교", 4 to "고등학교", 5 to "특수학교")

enum class InstitutionType(val displayName: String) {
	school("학교"),
	university("대학교"),
	academy("학원"),
	office("회사")
}

object ContentTypes {
	const val json = "application/json;charset=utf-8"
}
