package com.lhwdev.selfTestMacro.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


public interface ExternalState<out T, out Cache> where T : Cache {
	public val cache: Cache
	
	public suspend fun fetch(): T
	
	public suspend fun get(): T
}

public interface LazyExternalState<out T> : ExternalState<T, T?>

public interface DefaultExternalState<out T> : ExternalState<T, T>

public class LazyExternalStateImpl<out T : Any>(
	initialValue: T? = null,
	private val readBlock: suspend (update: suspend () -> T) -> T
) : LazyExternalState<T> {
	override var cache: @UnsafeVariance T? by mutableStateOf(initialValue)
		private set
	
	override suspend fun fetch(): T {
		val value = readBlock { fetch() }
		cache = value
		return value
	}
	
	override suspend fun get(): T = cache ?: fetch()
}

public class DefaultExternalStateImpl<out T>(
	initialValue: T,
	private val readBlock: suspend (update: suspend () -> T) -> T
) : DefaultExternalState<T> {
	override var cache: @UnsafeVariance T by mutableStateOf(initialValue)
		private set
	
	override suspend fun fetch(): T {
		val value = readBlock { fetch() }
		cache = value
		return value
	}
	
	override suspend fun get(): T = cache ?: fetch()
}

public abstract class MappedExternalState<F : FC, FC, T>(private val original: ExternalState<F, FC>) :
	LazyExternalState<T> {
	private var from: F? = null
	private var mCache: T? = null
	
	public override val cache: T?
		get() = mCache.takeIf { original.cache == from }
	
	protected abstract fun map(value: F): T
	
	override suspend fun fetch(): T {
		val from = original.fetch()
		this.from = from
		val value = map(from)
		mCache = value
		return value
	}
	
	override suspend fun get(): T = cache ?: fetch()
}

public inline fun <T : C, C, R> ExternalState<T, C>.map(
	crossinline block: (T, update: suspend () -> R) -> R
): LazyExternalState<R> = object : MappedExternalState<T, C, R>(original = this) {
	override fun map(value: T): R = block(value) { fetch() }
}
