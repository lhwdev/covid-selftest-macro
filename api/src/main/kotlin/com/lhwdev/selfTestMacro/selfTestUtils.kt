@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro


inline fun <reified T> FetchResult.toJsonLoose() = toJson<T> {
	ignoreUnknownKeys = true
}
