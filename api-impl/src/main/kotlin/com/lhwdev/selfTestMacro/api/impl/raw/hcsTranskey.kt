package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.http.Session
import com.lhwdev.io.jsonObjectString
import com.lhwdev.selfTestMacro.api.InternalHcsApi
import com.lhwdev.selfTestMacro.transkey.old.Transkey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.random.Random


@InternalHcsApi
public val transkeyUrl: URL = URL("https://hcs.eduro.go.kr/transkeyServlet")


@InternalHcsApi
internal suspend fun Session.raonPassword(password: String): String = withContext(Dispatchers.Default) {
	val transkey = Transkey(this@raonPassword, transkeyUrl, Random)
	
	val keyPad = transkey.newKeypad(
		keyType = "number",
		name = "password",
		inputName = "password",
		fieldType = "password"
	)
	
	val encrypted = keyPad.encryptPassword(password)
	
	val hm = transkey.hmacDigest(encrypted.toByteArray())
	
	jsonObjectString {
		"raon" jsonArray {
			addObject {
				"id" set "password"
				"enc" set encrypted
				"hmac" set hm
				"keyboardType" set "number"
				"keyIndex" set keyPad.keyIndex
				"fieldType" set "password"
				"seedKey" set transkey.crypto.encryptedKey
				"initTime" set transkey.initTime
				"ExE2E" set "false"
			}
		}
	}
}
