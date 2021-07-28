package com.lhwdev.selfTestMacro

import android.util.Base64


actual fun encodeBase64(array: ByteArray): String =
	Base64.encodeToString(array, Base64.NO_WRAP)

actual fun decodeBase64(string: String): ByteArray =
	Base64.decode(string, Base64.NO_WRAP)
