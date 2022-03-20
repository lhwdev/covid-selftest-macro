package com.lhwdev.utils

import kotlinx.coroutines.CancellationException
import java.io.InterruptedIOException


fun Throwable.willRethrowError(): Boolean = isCancellationError()

fun Throwable.isCancellationError(): Boolean = when(this) {
	is CancellationException -> true
	is InterruptedException -> true
	is InterruptedIOException -> true
	else -> false
}

fun Throwable.rethrowIfNeeded() {
	if(willRethrowError()) throw this
}
