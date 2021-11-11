package com.lhwdev.selfTestMacro.debug

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


abstract class DebugManager {
	class PendingError(
		val context: DebugContext,
		val error: ErrorInfo
	)
	
	class PendingErrorInfo(val error: PendingError, val job: Job)
	
	
	abstract val androidContext: Context
	
	abstract val workScope: CoroutineScope
	
	protected val pendingErrors = mutableMapOf<Any?, PendingErrorInfo>()
	
	
	open fun identifierOf(error: PendingError): Any? = error.error.throwable ?: Any() // Any(): not identifiable
	
	
	open fun pendThrowingError(error: PendingError) {
		pendError(afterMillis = 50, error = error)
	}
	
	open fun pendError(afterMillis: Int, error: PendingError) {
		val id = identifierOf(error)
		val previous = pendingErrors[id]
		if(previous == null) {
			val job = workScope.launch {
				delay(afterMillis.toLong())
				
				val e = (pendingErrors[id] ?: return@launch).error
				pendingErrors -= id
				
				error.context.onError(e.error)
			}
			
			pendingErrors[id] = PendingErrorInfo(error, job)
		} else {
			val new = previous.error.error.merge(error.error)
			pendingErrors[id] = PendingErrorInfo(PendingError(error.context, new), previous.job)
		}
	}
}


class DefaultDebugManager(
	override val androidContext: Context,
	override val workScope: CoroutineScope
) : DebugManager()
