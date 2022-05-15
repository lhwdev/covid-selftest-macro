package com.lhwdev.selfTestMacro.api.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lhwdev.selfTestMacro.utils.sEmpty


public interface ExternalState<out T : Any> {
	public val cache: T?
	
	public suspend fun fetch(): T
	
	public suspend fun get(): T
}

public class ExternalStateImpl<out T : Any>(
	private val readBlock: suspend (update: suspend () -> T) -> T
) : ExternalState<T> {
	public var mCache: @UnsafeVariance T? by mutableStateOf(null)
	
	override val cache: T?
		@Suppress("UNCHECKED_CAST")
		get() = if(mCache == sEmpty) null else mCache as T
	
	override suspend fun fetch(): T {
		val value = readBlock { fetch() }
		mCache = value
		return value
	}
	
	override suspend fun get(): T = cache ?: fetch()
}

public abstract class MappedExternalState<F : Any, T : Any>(private val original: ExternalState<F>) : ExternalState<T> {
	private var from: F? = null
	private var mCache: T? = null
	
	override val cache: T?
		get() = mCache.takeIf { original.cache == from }
	
	protected abstract fun map(value: F): T
	
	override suspend fun fetch(): T {
		val from = original.fetch()
		this.from = from
		val value = map(from)
		mCache = value
		return value
	}
	
	public fun overrideNow(value: T?) {
		from = original.cache
		mCache = value
	}
	
	override suspend fun get(): T = cache ?: fetch()
}

public inline fun <T : Any, R : Any> ExternalState<T>.map(
	crossinline block: (T, update: suspend () -> R) -> R
): MappedExternalState<T, R> = object : MappedExternalState<T, R>(original = this) {
	override fun map(value: T): R = block(value) { fetch() }
}
