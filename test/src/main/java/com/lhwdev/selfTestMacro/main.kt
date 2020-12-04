package com.lhwdev.selfTestMacro

import com.lhwdev.selfTestMacro.api.*
import java.util.Base64


suspend fun main() {
	sDebugFetch = true
	encodeBase64 = { Base64.getEncoder().encodeToString(it) }
	val schoolList = getSchoolData("03", "4", "영남고등학교", loginType =  LoginType.school)
	val school = schoolList.schoolList.single()
	val userToken = findUser(school, GetUserTokenRequestBody(school, "이현우", "040116", LoginType.school))
	val userInfo = getUserGroup(school, userToken).single()
	val detailedInfo = getDetailedUserInfo(school, userInfo)
	registerSurvey(school, detailedInfo.token, SurveyData(userToken = userToken.token, userName = userInfo.name))
}
