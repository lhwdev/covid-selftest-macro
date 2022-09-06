package com.lhwdev.selfTestMacro.api.utils

import kotlin.math.max


public class LifecycleValue<T> private constructor(value: T?, public val expiresAt: Long) {
	public companion object {
		public fun <T> empty(): LifecycleValue<T> =
			LifecycleValue(value = null, expiresAt = Long.MIN_VALUE)
		
		public fun <T> T.expiresAt(millis: Long): LifecycleValue<T> =
			LifecycleValue(value = this, expiresAt = millis)
		
		public fun <T> T.expiresIn(millis: Long): LifecycleValue<T> = // max: possible overflow
			LifecycleValue(value = this, expiresAt = max(millis, System.currentTimeMillis() + millis))
		
		public fun <T> T.infinite(): LifecycleValue<T> =
			LifecycleValue(value = this, expiresAt = Long.MAX_VALUE)
	}
	
	
	private val mValue: T? = value
	
	public val expired: Boolean get() = expiresAt > System.currentTimeMillis()
	
	public val value: T?
		get() = if(expired) {
			mValue
		} else {
			null // warning: leakage? IDK
		}
	
	public val unsafeValue: T? = mValue
}


public inline fun <T> LifecycleValue<T>.getOrDefault(defaultBlock: () -> T): T = value ?: defaultBlock()
