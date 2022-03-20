package com.lhwdev.selfTestMacro.debug

import android.content.Context
import android.util.Log
import com.lhwdev.selfTestMacro.App
import com.lhwdev.utils.willRethrowError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


abstract class DebugManager {
	class PendingError(
		val context: DebugContext,
		val error: ErrorInfo
	)
	
	class PendingErrorInfo(val pending: PendingError, val job: Job)
	
	
	abstract val workScope: CoroutineScope
	
	open val debugLogDirectory: File = App.debugLogDirectory
	
	protected val pendingErrors = mutableMapOf<Any?, PendingErrorInfo>()
	
	
	open fun identifierOf(error: ErrorInfo): Any? = error.throwable ?: Any() // Any(): not identifiable
	
	
	open fun pendThrowingError(error: PendingError) {
		pendError(afterMillis = 50, error = error)
	}
	
	open fun pendError(afterMillis: Int, error: PendingError) {
		val id = identifierOf(error.error)
		
		val previous = pendingErrors[id]
		if(previous == null) {
			val job = workScope.launch {
				delay(afterMillis.toLong())
				
				val e = (pendingErrors[id] ?: return@launch).pending
				pendingErrors -= id
				
				error.context.onError(e.error)
			}
			
			pendingErrors[id] = PendingErrorInfo(error, job)
		} else {
			val new = previous.pending.error.merge(error.error)
			pendingErrors[id] = PendingErrorInfo(PendingError(error.context, new), previous.job)
		}
	}
	
	open fun onErrorFromContext(context: DebugContext, error: ErrorInfo, forceShow: Boolean) {
		if(error.throwable != null && error.throwable.willRethrowError()) {
			Log.w("DebugManager", "Rethrowing error", error.throwable)
			throw error.throwable
		}
		val id = identifierOf(error)
		
		val previous = pendingErrors[id]
		var newError = error
		
		if(previous != null) {
			newError = previous.pending.error.merge(error)
			previous.job.cancel()
		}
		
		workScope.launch {
			context.onErrorSuspend(newError, forceShow)
		}
	}
}


class DefaultDebugManager(
	val androidContext: Context,
	override val workScope: CoroutineScope
) : DebugManager()
