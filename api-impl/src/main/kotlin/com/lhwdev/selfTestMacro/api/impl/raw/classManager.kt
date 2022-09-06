@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.Bodies
import com.lhwdev.fetch.get
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.json
import com.lhwdev.fetch.sDefaultFakeHeader
import com.lhwdev.io.decodeBase64
import com.lhwdev.selfTestMacro.api.User
import com.lhwdev.selfTestMacro.toJsonLoose
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * ```
 * POST /joinClassList HTTP/1.1
 * <common headers>
 * Authorization: UserToken(..)
 *
 * Response:
 * {
 *   "classList": [
 *     {
 *       "orgCode":"Xnnnnnnnnnn",
 *       "dghtCrseScCode":"n",
 *       "ordScCode":"nn",
 *       "dddepCode":"nnnn",
 *       "grade":"<grade>",
 *       "classCode":"<class number code>",
 *       "schulCrseScCode":"n",
 *       "ay":"<year>",
 *       "kraOrgNm":"<school name>",
 *       "dghtCrseScNm":"<classifier>",
 *       "classNm":"<class number>"
 *     },
 *     ...
 *   ]
 * }
 * ```
 */
@Serializable
public data class ClassList(val classList: List<ClassInfo>)

@Serializable
public data class ClassInfo(
	@SerialName("orgCode") val instituteCode: String,
	@Serializable(IntAsStringSerializer::class) val grade: Int,
	@SerialName("classNm") @Serializable(IntAsStringSerializer::class) val classNumber: Int,
	@SerialName("classCode") val classCode: String
)

public suspend fun HcsSession.getClassList(token: User.Token): ClassList = fetch(
	requestUrl["/joinClassList"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Authorization" to token.token)
).toJsonLoose(ClassList.serializer())


/**
 * ```
 * POST /join HTTP/1.1
 * <common headers>
 * Authorization: UserToken(..)
 *
 * Request payload: ClassInfo(..)
 *
 * Response:
 * {
 *   "joinList":[
 *     {
 *       "orgCode":"Xnnnnnnnnnn",
 *       "inveYmd":"YYYYMMDD <last register date>",
 *       "grade":"<grade>",
 *       "classCode":"<class number code>",
 *       "serveyTime":"n <???>",
 *       "name":"<name>",
 *       "userPNo":"<user id>", // see getUserInfo request payload
 *       "surveyYn":"Y/N",
 *       "rspns00":"Y/N",
 *       "deviceUuidYn":"Y/N", // if installed the offical app
 *       "registerDtm":"YYYY-MM-DD HH:MM:SS.ssssss <last register time>",
 *       "stdntCnEncpt":"n",
 *       "upperUserName":"<also name>"
 *     },
 *     ...
 *   ]
 * }
 * ```
 */
@Serializable
public data class ClassSurveyStatus(val joinList: List<ClassSurveyStudentStatus>)

@Serializable
public data class ClassSurveyStudentStatus(
	val name: String,
	@Serializable(IntAsStringSerializer::class) val grade: Int,
	@SerialName("classCode") val classCode: String,
	@SerialName("userPNo") val userCode: String,
	@SerialName("surveyYn") @Serializable(YesNoSerializer::class) val registeredSurvey: Boolean,
	@SerialName("registerDtm") val lastRegisterAt: String? = null,
	@SerialName("deviceUuidYn") @Serializable(YesNoSerializer::class) val installedOfficalApp: Boolean
)

public suspend fun HcsSession.getClassSurveyStatus(token: User.Token, classInfo: ClassInfo): ClassSurveyStatus = fetch(
	requestUrl["/join"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf(
		"Authorization" to token.token
	),
	body = Bodies.json(ClassInfo.serializer(), classInfo)
).toJsonLoose(ClassSurveyStatus.serializer())


/**
 * ```json
 * {
 *   "joinInfo": {
 *     "grade":"<number>",
 *     "classCode":"<class code>",
 *     "name":"<name>",
 *     "surveyYn":"Y/N",
 *     "isHealthy":boolean,
 *     "atptOfcdcConctUrl":"???hcs.eduro.go.kr",
 *     "pInfAgrmYn":"Y/N",
 *     "mobnuEncpt":"<base64 encrypted phone number>"
 *   }
 * }
 * ```
 */
@Serializable
public data class ClassSurveyStudentStatusDetail(
	@Serializable(IntAsStringSerializer::class) val grade: Int,
	val classCode: String,
	val name: String,
	@SerialName("surveyYn") @Serializable(YesNoSerializer::class) val registeredSurvey: Boolean,
	val isHealthy: Boolean,
	@SerialName("mobnuEncpt") val phoneNumberBase64Encoded: String
) {
	val phoneNumber: String get() = decodeBase64(phoneNumberBase64Encoded).toString(Charsets.UTF_8)
}

public suspend fun HcsSession.getStudentSurveyStatusDetail(
	token: User.Token, student: ClassSurveyStudentStatus
): ClassSurveyStudentStatusDetail = fetch(
	requestUrl["/joinDetail"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf(
		"Authorization" to token.token
	),
	body = Bodies.json(ClassSurveyStudentStatus.serializer(), student)
).toJsonLoose(ClassSurveyStudentStatusDetail.serializer())
