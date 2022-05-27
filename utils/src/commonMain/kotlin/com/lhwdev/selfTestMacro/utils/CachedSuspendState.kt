package com.lhwdev.selfTestMacro.utils

import androidx.compose.runtime.snapshots.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


interface CachedSuspendState<out Get : Cache, out Cache> {
	val cache: Cache
	
	suspend fun get(): Get
	
	suspend fun refresh(): Get
}

interface NullableSuspendState<T : Any> : CachedSuspendState<T, T?>

interface PresentSuspendState<T> : CachedSuspendState<T, T>


/**
 * Note that there is no guarantee about what the dispatcher is.
 * You may have to specify it by yourself.
 */
abstract class CachedSuspendStateBase<Get : Cache, Cache, GetIntermediate> :
	CachedSuspendState<Get, Cache>, StateObject {
	protected abstract suspend fun readValue(): GetIntermediate
	
	
	protected abstract var next: CachedStateRecord<Get, Cache, GetIntermediate>
	
	final override val firstStateRecord: StateRecord
		get() = next
	
	final override fun prependStateRecord(value: StateRecord) {
		@Suppress("UNCHECKED_CAST")
		next = value as CachedStateRecord<Get, Cache, GetIntermediate>
	}
	
	
	final override val cache: Cache
		get() = next.readable(this).getCache()
	
	private val mutex = Mutex() // write mutex
	
	final override suspend fun get(): Get {
		val currentCache = next.readable(this).getCachedValue()
		return if(currentCache is Optional.Present) {
			currentCache.value
		} else {
			refresh()
		}
	}
	
	final override suspend fun refresh(): Get = mutex.withLock {
		val intermediate = readValue()
		
		val actual = next.writable(this) {
			setCache(intermediate)
		}
		actual
	}
	
	abstract class CachedStateRecord<Get : Cache, Cache, GetIntermediate> : StateRecord() {
		abstract fun setCache(value: GetIntermediate): Get
		
		abstract fun getCache(): Cache
		
		abstract fun getCachedValue(): Optional<Get>
	}
	
	override fun toString(): String = next.withCurrent {
		"CachedSuspendState(value=${it.getCache()})@${hashCode()}"
	}
	
	
	/**
	 * A function used by the debugger to display the value of the current value of the mutable
	 * state object without triggering read observers.
	 */
	@Suppress("unused")
	val debuggerDisplayValue: Cache
		@JvmName("getDebuggerDisplayValue")
		get() = next.withCurrent { it }.getCache()
}


abstract class SimpleCachedSuspendState<Get : Cache, Cache>(initialCache: Cache) :
	CachedSuspendStateBase<Get, Cache, Get>() {
	override var next: CachedStateRecord<Get, Cache, Get> = SimpleCachedStateRecord(initialCache)
	
	protected abstract fun asCache(cache: Cache): Optional<Get>
	
	
	private inner class SimpleCachedStateRecord(var currentCache: Cache) :
		CachedStateRecord<Get, Cache, Get>() {
		
		override fun assign(value: StateRecord) {
			@Suppress("UNCHECKED_CAST")
			currentCache = (value as SimpleCachedSuspendState<Get, Cache>.SimpleCachedStateRecord).currentCache
		}
		
		override fun create(): StateRecord = SimpleCachedStateRecord(currentCache)
		
		override fun getCache(): Cache = currentCache
		override fun getCachedValue(): Optional<Get> = asCache(currentCache)
		
		override fun setCache(value: Get): Get {
			currentCache = value
			return value
		}
	}
}


inline fun <T : Any> NullableSuspendState(
	initialCache: T?,
	crossinline block: suspend () -> T
): NullableSuspendState<T> =
	object : SimpleCachedSuspendState<T, T?>(initialCache), NullableSuspendState<T> {
		override suspend fun readValue(): T = block()
		override fun asCache(cache: T?): Optional<T> = Optional.nullable(cache)
	}

inline fun <T> PresentSuspendState(
	initialCache: T,
	crossinline block: suspend () -> T
): PresentSuspendState<T> =
	object : SimpleCachedSuspendState<T, T>(initialCache), PresentSuspendState<T> {
		override suspend fun readValue(): T = block()
		override fun asCache(cache: T): Optional<T> = Optional.of(cache)
	}


