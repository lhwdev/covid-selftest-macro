@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro


inline fun <reified T> FetchResult.toJsonLoose() = toJson<T> {
	ignoreUnknownKeys = true
}


///**
// * {
// *     "registerDtm":"2020-09-07 13:37:16.817049",
// *     "admnYn":"N",
// *     "orgname":"학교",
// *     "registerYmd":"20200907",
// *     "mngrClassYn":"N", // 아마 관리자(선생님)이면 Y일 것으로 추정
// *     "name":"이름", "man":"N", "stdntYn":"Y",
// *     "infAgrmYn":"Y",
// *     "token":"...",
// *     "mngrDeptYn":"N",
// *     "isHealthy":true,
// *     "serverAddr":"dgehcs.eduro.go.kr"
// * }
// */
//@Serializable
//class TestUser(
//	// user info
//	val userType: UserType,
//	val name: String,
//	val organization: String,
//	val token: String,
//
//	// etc
//	@Serializable(with = URLSerializer::class)
//	val serverAddress: URL
//) {
//	enum class UserType { student, admin }
//
//	fun userInfoToString() = "$name ($organization)"
//}


//fun TestUser(obj: JsonObject) = TestUser(
//	userType = when {
//		obj["stdntYn"]!!.jsonPrimitive.content == "Y" -> TestUser.UserType.student
//		obj["mngrClassYn"]!!.jsonPrimitive.content == "Y" -> TestUser.UserType.admin
//		else -> TestUser.UserType.student // I don't know, but assuming
//	},
//	name = obj.[name"],
//	organization = obj.getString("orgname"),
//	token = obj.getString("token"),
//
//	// etc
//	serverAddress = URL(obj.getString("serverAddr"))
//)


//fun parseUsersStorageData(content: String): List<TestUser> {
//	val array = JSONArray(content)
//	return List(array.length()) { i -> TestUser(array.getJSONObject(i)) }
//}


//data class SubmitResult(val submitTime: String)
//
//@Suppress("SpellCheckingInspection")
//suspend fun Context.submitSuspend(user: TestUser) = withContext(Dispatchers.IO) {
//	val server = user.serverAddress
//	val token = user.token
//
//	val request = URL(server, "/registerServey")
//		.openConnection() as HttpURLConnection
//	request.requestMethod = "POST"
//
//	val toWrite = """
//		{"rspns01":"1","rspns02":"1","rspns03":null,"rspns04":null,"rspns05":null,"rspns06":null,"rspns07":"0","rspns08":"0","rspns09":"0","rspns10":null,"rspns11":null,"rspns12":null,"rspns13":null,"rspns14":null,"rspns15":null,"rspns00":"Y","deviceUuid":""}
//	""".trimIndent()
//
//	mapOf(
//		"Content-Type" to "application/json;charset=UTF-8",
//		"User-Agent" to "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8",
//		"Authentication" to token
//	).forEach { (k, v) -> request.addRequestProperty(k, v) }
//
//	request.outputStream.writer().use {
//		it.write(toWrite)
//		it.flush()
//	}
//
//	// {"registerDtm":"Sep 9, 2020 11:12:25 AM","inveYmd":"20200909"}
//	val response = request.inputStream.reader().readText()
//	Log.i("HOI", "${request.responseMessage} / $response")
//
//	val responseObj = JSONObject(response)
//	val registerTime = responseObj.getString("registerDtm")
//
//	request.disconnect()
//
//	showToastSuspendAsync("자가진단 완료")
//
//	// log
//	File(getExternalFilesDir(null)!!, "log.txt")
//		.appendText(
//			"self-tested at $registerTime ${request.responseMessage}\n"
//		)
//
//	SubmitResult(registerTime)
//}
