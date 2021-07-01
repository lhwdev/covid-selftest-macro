package com.lhwdev.selfTestMacro

import kotlinx.coroutines.*


interface DisposeScope {
	fun registerDisposal(onDispose: () -> Unit)
}

inline fun <R> disposeScope(block: DisposeScope.() -> R): R {
	val disposals = mutableListOf<() -> Unit>()
	return try {
		object : DisposeScope {
			override fun registerDisposal(onDispose: () -> Unit) {
				disposals += onDispose
			}
		}.block()
	} finally {
		disposals.forEach { it() }
	}
}

suspend inline fun <R> ioTask(crossinline task: suspend DisposeScope.() -> R): R =
	withContext(Dispatchers.IO) {
		disposeScope { task() }
	}
