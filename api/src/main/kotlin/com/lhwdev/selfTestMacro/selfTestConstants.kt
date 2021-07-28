package com.lhwdev.selfTestMacro

import java.net.URL


val sCommonUrl = URL("https://hcs.eduro.go.kr/v2")

val sRegions = mapOf(
	"서울" to "01", "부산" to "02", "대구" to "03", "인천" to "04", "광주" to "05", "대전" to "06",
	"울산" to "07", "세종" to "08", "경기" to "10", "강원" to "11", "충북" to "12",
	"충남" to "13", "전북" to "14", "전남" to "15", "경북" to "16", "경남" to "17", "제주" to "18"
)

val sSchoolLevels = mapOf("유치원" to 1, "초등학교" to 2, "중학교" to 3, "고등학교" to 4, "특수학교" to 5)

object ContentTypes {
	const val json = "application/json;charset=utf-8"
}
