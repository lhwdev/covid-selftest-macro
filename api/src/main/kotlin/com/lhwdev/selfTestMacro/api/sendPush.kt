package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*


// setSender: function (e) {
// var t = "";
// return (
//   Object.prototype.hasOwnProperty.call(e, "classNm") &&
//   ((t = e.kraOrgNm),
//     (t += "0" !== e.grade ? " ".concat(e.grade, "학년") : ""),
//     (t += " ".concat(e.classNm, "반"))),
//   Object.prototype.hasOwnProperty.call(e, "kraDeptNm") &&
//   (t = "".concat(e.kraOrgNm, " ").concat(e.kraDeptNm)),
//   (e.senderName = t),
//   e
// );
/**
 * Note: make sure to get users token in 'right' way and inform user before calling this
 */
@DangerousHcsApi
public suspend fun Session.sendPushNotification(institute: InstituteInfo, token: UsersToken) {
	fetch(
		institute.requestUrl["push"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf("Authorization" to token.token),
		body = HttpBodies.jsonObject {
			TODO()
		}
	)
}
