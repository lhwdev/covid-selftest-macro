package com.lhwdev.io

import com.lhwdev.utils.isCancellationError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runInterruptible
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


suspend fun <R> runInterruptibleGracefully(context: CoroutineContext = EmptyCoroutineContext, block: () -> R): R = try {
	runInterruptible(context, block)
} catch(th: Throwable) {
	if(th !is CancellationException && th.isCancellationError()) {
		throw CancellationException(message = "gracefully wrapping cancellation error", th)
	} else {
		throw th
	}
}
