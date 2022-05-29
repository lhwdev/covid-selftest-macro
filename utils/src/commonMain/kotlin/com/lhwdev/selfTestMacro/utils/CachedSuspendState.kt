package com.lhwdev.selfTestMacro.utils

import androidx.compose.runtime.snapshots.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


interface CachedSuspendState<out T> {
	val cache: T?
	
	suspend fun get(): T
	
	suspend fun refresh(): T
}

interface PresentCachedSuspendState<out T> : CachedSuspendState<T> {
	override val cache: T
}


/**
 * Note that there is no guarantee about what the dispatcher is.
 * You may have to specify it by yourself.
 */
abstract class CachedSuspendStateBase<T, Intermediate> : CachedSuspendState<T>, StateObject {
	protected abstract suspend fun readValue(): Intermediate
	
	
	protected abstract var next: CachedStateRecord<T, Intermediate>
	
	final override val firstStateRecord: StateRecord
		get() = next
	
	final override fun prependStateRecord(value: StateRecord) {
		@Suppress("UNCHECKED_CAST")
		next = value as CachedStateRecord<T, Intermediate>
	}
	
	
	override val cache: T?
		get() = next.readable(this).getCache()
	
	private val mutex = Mutex() // write mutex
	
	final override suspend fun get(): T {
		val currentCache = next.readable(this).getCachedValue()
		return if(currentCache is Optional.Present) {
			currentCache.value
		} else {
			refresh()
		}
	}
	
	final override suspend fun refresh(): T = mutex.withLock {
		val intermediate = readValue()
		
		next.writable(this) {
			setCache(intermediate)
		}
	}
	
	abstract class CachedStateRecord<T, Intermediate> : StateRecord() {
		abstract fun setCache(value: Intermediate): T
		
		abstract fun getCache(): T?
		
		abstract fun getCachedValue(): Optional<T>
	}
	
	override fun toString(): String = next.withCurrent {
		"CachedSuspendState(value=${it.getCache()})@${hashCode()}"
	}
	
	
	/**
	 * A function used by the debugger to display the value of the current value of the mutable
	 * state object without triggering read observers.
	 */
	@Suppress("unused")
	val debuggerDisplayValue: T?
		@JvmName("getDebuggerDisplayValue")
		get() = next.withCurrent { it }.getCache()
}


abstract class SimpleCachedSuspendState<T>(initialCache: T?) : CachedSuspendStateBase<T, T>() {
	override var next: CachedStateRecord<T, T> = SimpleCachedStateRecord(initialCache)
	
	abstract fun asValue(cache: T?): Optional<T>
	
	
	private inner class SimpleCachedStateRecord(var currentCache: T?) :
		CachedStateRecord<T, T>() {
		
		override fun assign(value: StateRecord) {
			@Suppress("UNCHECKED_CAST")
			currentCache = (value as SimpleCachedSuspendState<T>.SimpleCachedStateRecord).currentCache
		}
		
		override fun create(): StateRecord = SimpleCachedStateRecord(currentCache)
		
		override fun getCache(): T? = currentCache
		override fun getCachedValue(): Optional<T> = asValue(currentCache)
		
		override fun setCache(value: T): T {
			currentCache = value
			return value
		}
	}
}

abstract class SimpleCachedSuspendState2<T>(initialCache: T) : CachedSuspendStateBase<T, T>() {
	
	override val cache: T
		get() = (next.readable(this) as SimpleCachedStateRecord).getCache()
	
	override var next: CachedStateRecord<T, T> = SimpleCachedStateRecord(initialCache)
	
	abstract fun asValue(cache: T): Optional<T>
	
	
	private inner class SimpleCachedStateRecord(var currentCache: T) :
		CachedStateRecord<T, T>() {
		
		override fun assign(value: StateRecord) {
			@Suppress("UNCHECKED_CAST")
			currentCache = (value as SimpleCachedSuspendState2<T>.SimpleCachedStateRecord).currentCache
		}
		
		override fun create(): StateRecord = SimpleCachedStateRecord(currentCache)
		
		override fun getCache(): T = currentCache
		override fun getCachedValue(): Optional<T> = asValue(currentCache)
		
		override fun setCache(value: T): T {
			currentCache = value
			return value
		}
	}
}


inline fun <T : Any> CachedSuspendState(
	initialCache: T? = null,
	crossinline block: suspend () -> T
): CachedSuspendState<T> = object : SimpleCachedSuspendState<T>(initialCache) {
	override suspend fun readValue(): T = block()
	override fun asValue(cache: T?): Optional<T> = Optional.nullable(cache)
}

@Suppress("UNCHECKED_CAST")
inline fun <T> PresentCachedSuspendState(
	initialCache: T,
	crossinline block: suspend () -> T
): PresentCachedSuspendState<T> = object : SimpleCachedSuspendState<T>(initialCache), PresentCachedSuspendState<T> {
	override val cache: T
		get() = super.cache as T
	
	override suspend fun readValue(): T = block()
	override fun asValue(cache: T?): Optional<T> = Optional.of(cache as T)
}


@PublishedApi
internal abstract class MappedCachedSuspendState<From : Any, To : Any>(
	private val from: CachedSuspendState<From>
) : CachedSuspendState<To> {
	private var currentCache: Pair<From, To>? = null
	
	abstract fun map(value: From): To
	
	private fun mapAndCache(from: From): To {
		val value = map(from)
		currentCache = from to value
		return value
	}
	
	override val cache: To?
		get() {
			val current = from.cache
			val currentCache = currentCache
			
			return if(currentCache != null && currentCache.first == current) {
				currentCache.second
			} else {
				if(current != null) {
					mapAndCache(current)
				} else {
					null
				}
			}
		}
	
	override suspend fun get(): To = mapAndCache(from.get())
	
	override suspend fun refresh(): To = mapAndCache(from.refresh())
}

inline fun <From : Any, To : Any> CachedSuspendState<From>.map(
	crossinline block: (From, refresh: suspend () -> To) -> To
): CachedSuspendState<To> = object : MappedCachedSuspendState<From, To>(this) {
	override fun map(value: From): To = block(value) { refresh() }
}

@Suppress("UNCHECKED_CAST")
inline fun <From : Any, To : Any> PresentCachedSuspendState<From>.map(
	crossinline block: (From, refresh: suspend () -> To) -> To
): PresentCachedSuspendState<To> = object : MappedCachedSuspendState<From, To>(this), PresentCachedSuspendState<To> {
	override val cache: To get() = super.cache as To
	
	override fun map(value: From): To = block(value) { refresh() }
}
