@file:OptIn(InternalCoroutinesApi::class)

package com.lhwdev.fetch.http

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


interface RunInterruptibleScope : CoroutineScope {
	fun setupInterruptible()
	fun cleanInterruptible()
}

@OptIn(ExperimentalContracts::class)
fun <R> RunInterruptibleScope.interruptible(block: () -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	setupInterruptible()
	return try {
		block()
	} finally {
		cleanInterruptible()
	}
}


suspend fun <T> interruptibleScope(
	context: CoroutineContext = EmptyCoroutineContext,
	block: suspend RunInterruptibleScope.() -> T // became suspend
): T = withContext(context) {
	runInterruptibleInExpectedContext(coroutineContext) { block() }
}


private inline fun <T> runInterruptibleInExpectedContext(
	coroutineContext: CoroutineContext,
	block: RunInterruptibleScope.() -> T
): T {
	try {
		val scope = RunInterruptibleScopeImpl(coroutineContext)
		scope.setup()
		try {
			return scope.block()
		} finally {
			scope.clearLeftInterrupt()
		}
	} catch(e: InterruptedException) {
		throw CancellationException("Blocking call was interrupted due to parent cancellation").initCause(e)
	}
}

private const val WORKING = 0
private const val FINISHED = 1
private const val INTERRUPTING = 2
private const val INTERRUPTED = 3

private class RunInterruptibleScopeImpl(override val coroutineContext: CoroutineContext) : RunInterruptibleScope {
	/*
	   === States ===
	   WORKING: running normally
	   FINISH: complete normally
	   INTERRUPTING: canceled, going to interrupt this thread
	   INTERRUPTED: this thread is interrupted
	   === Possible Transitions ===
	   +----------------+         register job       +-------------------------+
	   |    WORKING     |   cancellation listener    |         WORKING         |
	   | (thread, null) | -------------------------> | (thread, cancel handle) |
	   +----------------+                            +-------------------------+
			   |                                                |   |
			   | cancel                                  cancel |   | complete
			   |                                                |   |
			   V                                                |   |
	   +---------------+                                        |   |
	   | INTERRUPTING  | <--------------------------------------+   |
	   +---------------+                                            |
			   |                                                    |
			   | interrupt                                          |
			   |                                                    |
			   V                                                    V
	   +---------------+                              +-------------------------+
	   |  INTERRUPTED  |                              |         FINISHED        |
	   +---------------+                              +-------------------------+
	*/
	private val _state = AtomicInteger(WORKING)
	val targetThreads = ConcurrentSkipListSet<Thread>()
	
	// Registered cancellation handler
	private var cancelHandle: DisposableHandle? = null
	
	// Cancellation handler
	private val completionHandler: CompletionHandler = handler@{ _ ->
		while(true) {
			when(val state = _state.get()) {
				// Working -> try to transit state and interrupt the thread
				WORKING -> {
					if(_state.compareAndSet(state, INTERRUPTING)) {
						targetThreads.forEach { it.interrupt() }
						_state.set(INTERRUPTED)
						return@handler
					}
				}
				// Finished -- runInterruptible is already complete, INTERRUPTING - ignore
				FINISHED, INTERRUPTING, INTERRUPTED -> return@handler
				else -> invalidState(state)
			}
		}
	}
	
	fun setup() {
		cancelHandle = coroutineContext.job.invokeOnCompletion(
			onCancelling = true,
			invokeImmediately = true,
			handler = completionHandler
		)
		
		// Either we successfully stored it or it was immediately cancelled
		while(true) {
			when(val state = _state.get()) {
				// Happy-path, move forward
				WORKING -> if(_state.compareAndSet(state, WORKING)) return
				// Immediately cancelled, just continue
				INTERRUPTING, INTERRUPTED -> return
				else -> invalidState(state)
			}
		}
	}
	
	override fun setupInterruptible() {
		targetThreads += Thread.currentThread()
	}
	
	override fun cleanInterruptible() {
		targetThreads -= Thread.currentThread()
		
		while(true) {
			when(val state = _state.get()) {
				WORKING -> {
					// no-op: just move on
					return
				}
				INTERRUPTING -> {
					// Spin, cancellation mechanism is interrupting our thread right now,
					// and we have to wait it and then clear interrupt status
				}
				INTERRUPTED -> {
					// Clear it and bail out
					Thread.interrupted()
					return
				}
				else -> invalidState(state)
			}
		}
	}
	
	fun clearLeftInterrupt() {
		/*
		 * Do not allow to untriggered interrupt to leak
		 */
		while(true) {
			when(val state = _state.get()) {
				WORKING -> if(_state.compareAndSet(state, FINISHED)) {
					cancelHandle?.dispose()
					return
				}
				INTERRUPTING -> {
					// Spin, cancellation mechanism is interrupting other threads right now.
				}
				else -> invalidState(state)
			}
		}
	}
	
	
	private fun invalidState(state: Int): Nothing = error("Illegal state $state")
}
