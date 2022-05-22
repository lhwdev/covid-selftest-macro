package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.Bodies
import com.lhwdev.fetch.get
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.jsonObject
import com.lhwdev.fetch.sDefaultFakeHeader


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
public suspend fun Session.sendPushNotification(institute: ApiInstituteInfo, token: UsersToken) {
	fetch(
		institute.requestUrl["/push"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf("Authorization" to token.token),
		body = Bodies.jsonObject {
			TODO()
		}
	)
}
