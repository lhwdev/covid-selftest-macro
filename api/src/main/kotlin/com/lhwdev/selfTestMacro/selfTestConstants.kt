package com.lhwdev.selfTestMacro

import com.lhwdev.selfTestMacro.api.LoginType
import java.net.URL


val sCommonUrl = URL("https://hcs.eduro.go.kr/v2")

val sRegions = mapOf(
	"01" to "서울", "02" to "부산", "03" to "대구", "04" to "인천", "05" to "광주", "06" to "대전",
	"07" to "울산", "08" to "세종", "10" to "경기", "11" to "강원", "12" to "충북",
	"13" to "충남", "14" to "전북", "15" to "전남", "16" to "경북", "17" to "경남", "18" to "제주"
)

val sSchoolLevels = mapOf(1 to "유치원", 2 to "초등학교", 3 to "중학교", 4 to "고등학교", 5 to "특수학교 등")

enum class InstituteType(val displayName: String, val loginType: LoginType) {
	school("학교", LoginType.school),
	university("대학교", LoginType.univ),
	academy("학원", LoginType.office),
	office("회사", LoginType.office)
}

object ContentTypes {
	const val json = "application/json;charset=utf-8"
}
