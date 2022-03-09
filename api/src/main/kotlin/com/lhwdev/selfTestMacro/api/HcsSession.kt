package com.lhwdev.selfTestMacro.api

import com.lhwdev.fetch.http.Session
import java.net.CookieManager
import java.net.CookiePolicy


public interface HcsSession : Session {
	public var clientVersion: String
}


public class BasicHcsSession(
	override val cookieManager: CookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL),
	override var keepAlive: Boolean? = null
) : HcsSession {
	override var clientVersion: String = ""
}
