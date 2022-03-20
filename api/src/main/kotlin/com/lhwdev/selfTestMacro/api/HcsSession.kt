package com.lhwdev.selfTestMacro.api

import com.lhwdev.fetch.http.Session
import java.net.URL


public interface HcsSession : Session {
	/**
	 * The base url fraction for most hcs operations.
	 * This property is commonly used to get `atptOfcdcConctUrl`. (url for Si/Do)
	 * Note that this url does not include `https://`. Instead, use [requestUrl] or [requestUrlV2].
	 *
	 * Normally form of `???hcs.eduro.go.kr` where `???` comes the code of Ministry of Education, i.e., 'sen', 'dge'.
	 *
	 * @see InstituteInfo.requestUrlBody
	 */
	public val requestUrlBody: String
	
	/**
	 * v2 url for request such as [findUser], [validatePassword], [getUserGroup], [getUserInfo].
	 *
	 * Normally form of `https://???hcs.eduro.go.kr/v2` where `???` comes the code of Ministry of Education, i.e.,
	 * 'sen', 'dge'.
	 */
	public val requestUrlV2: URL get() = URL("https://$requestUrlBody/v2")
	
	/**
	 * The base url for request such as [registerSurvey], [getClassList].
	 *
	 * Normally form of `https://???hcs.eduro.go.kr` where `???` comes the code of Ministry of Education, i.e.,
	 * 'sen', 'dge'.
	 */
	public val requestUrl: URL get() = URL("https://$requestUrlBody")
	
	public var clientVersion: String
}

// public class BasicHcsSession(
// 	override val requestUrlBody: String,
// 	override val cookieManager: CookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL),
// 	override var keepAlive: Boolean? = null
// ) : HcsSession {
// 	override var clientVersion: String = ""
// }
