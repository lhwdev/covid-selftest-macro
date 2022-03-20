@file:JvmName("AndroidUtils")

package com.lhwdev.selfTestMacro

import android.content.Context
import android.widget.Toast
import com.lhwdev.utils.rethrowIfNeeded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


inline fun <R> tryAtMost(
	maxTrial: Int,
	errorFilter: (Throwable) -> Boolean = { true },
	onError: (th: Throwable) -> Unit = {},
	block: () -> R
): R {
	var trialCount = 0
	while(true) {
		try {
			return block()
		} catch(th: Throwable) {
			th.rethrowIfNeeded()
			if(!errorFilter(th)) throw th
			trialCount++
			if(trialCount >= maxTrial) throw th
			onError(th)
		}
	}
}

fun <K, V> Map<K, V>.added(key: K, value: V): Map<K, V> {
	val newMap = toMutableMap()
	newMap[key] = value
	return newMap
}

fun <K, V> Map<K, V>.replaced(from: K, to: V): Map<K, V> = toMutableMap().also {
	it[from] = to
}

fun <K, V> Map<K, V>.replacedValue(from: V, to: V): Map<K, V> =
	mapValues { (_, value) -> if(value == from) to else value }

fun <T> List<T>.replaced(from: T, to: T): List<T> = map { if(it == from) to else it }


suspend fun Context.showToastSuspendAsync(message: String, isLong: Boolean = false) =
	withContext(Dispatchers.Main) {
		showToast(message, isLong)
	}

fun Context.showToast(message: String, isLong: Boolean = false) {
	Toast.makeText(this, message, if(isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}
