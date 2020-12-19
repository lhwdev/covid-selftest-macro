@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro

import kotlinx.serialization.KSerializer


inline fun <reified T> FetchResult.toJsonLoose() = toJson<T> {
	ignoreUnknownKeys = true
}

fun <T> FetchResult.toJsonLoose(serializer: KSerializer<T>) = toJson(serializer) {
	ignoreUnknownKeys = true
}
