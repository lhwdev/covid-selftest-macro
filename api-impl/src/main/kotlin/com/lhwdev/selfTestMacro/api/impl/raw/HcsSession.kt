package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.api.CovidHcsApi
import java.net.URL


public interface HcsSession : Session {
	/**
	 * The base url fraction for most hcs operations.
	 * This property is commonly used to get `atptOfcdcConctUrl`. (url for Si/Do)
	 * Note that this url does not include `https://`. Instead, use [requestUrl].
	 *
	 * Normally form of `???hcs.eduro.go.kr` where `???` comes the code of Ministry of Education, i.e., 'sen', 'dge'.
	 *
	 * @see ApiInstituteInfo.requestUrlBody
	 */
	public val requestUrlBody: String
	
	/**
	 * The base url for request such as [findUser], [registerSurvey], [getClassList].
	 *
	 * Normally form of `https://???hcs.eduro.go.kr` where `???` comes the code of Ministry of Education, i.e.,
	 * 'sen', 'dge'.
	 */
	public val requestUrl: URL get() = URL("https://$requestUrlBody")
	
	public var clientVersion: String
	
	public val api: CovidHcsApi
}
