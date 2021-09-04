@file:Suppress("NewApi") // why? it's just java

package com.lhwdev.io

import java.util.Base64


actual fun encodeBase64(array: ByteArray): String =
	Base64.getEncoder().encodeToString(array)

actual fun decodeBase64(string: String): ByteArray =
	Base64.getDecoder().decode(string.toByteArray())
