@file:OptIn(InternalCoroutinesApi::class)

package com.lhwdev.fetch.http

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


suspend fun <T> runInterruptibleFork(
	context: CoroutineContext = EmptyCoroutineContext,
	block: suspend CoroutineScope.() -> T // became suspend
): T = withContext(context) {
	runInterruptibleInExpectedContext(coroutineContext) { block() }
}

suspend fun <T> runInterruptible(
	context: CoroutineContext = EmptyCoroutineContext,
	block: () -> T
): T = withContext(context) {
	runInterruptibleInExpectedContext(coroutineContext) { block() }
}


private inline fun <T> CoroutineScope.runInterruptibleInExpectedContext(
	coroutineContext: CoroutineContext,
	block: CoroutineScope.() -> T
): T {
	try {
		val threadState = ThreadState(coroutineContext.job)
		threadState.setup()
		try {
			return block()
		} finally {
			threadState.clearInterrupt()
		}
	} catch(e: InterruptedException) {
		throw CancellationException("Blocking call was interrupted due to parent cancellation").initCause(e)
	}
}

private const val WORKING = 0
private const val FINISHED = 1
private const val INTERRUPTING = 2
private const val INTERRUPTED = 3

private class ThreadState(private val job: Job) : CompletionHandler {
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
	private val targetThread = Thread.currentThread()
	
	// Registered cancellation handler
	private var cancelHandle: DisposableHandle? = null
	
	fun setup() {
		cancelHandle = job.invokeOnCompletion(onCancelling = true, invokeImmediately = true, handler = this)
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
	
	fun clearInterrupt() {
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
					/*
					 * Spin, cancellation mechanism is interrupting our thread right now,
					 * and we have to wait it and then clear interrupt status
					 */
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
	
	// Cancellation handler
	override fun invoke(cause: Throwable?) {
		while(true) {
			when(val state = _state.get()) {
				// Working -> try to transit state and interrupt the thread
				WORKING -> {
					if(_state.compareAndSet(state, INTERRUPTING)) {
						targetThread.interrupt()
						_state.set(INTERRUPTED)
						return
					}
				}
				// Finished -- runInterruptible is already complete, INTERRUPTING - ignore
				FINISHED, INTERRUPTING, INTERRUPTED -> return
				else -> invalidState(state)
			}
		}
	}
	
	private fun invalidState(state: Int): Nothing = error("Illegal state $state")
}
