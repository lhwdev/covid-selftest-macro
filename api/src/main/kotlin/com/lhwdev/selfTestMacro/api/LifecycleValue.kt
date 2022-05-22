package com.lhwdev.selfTestMacro.api


public class LifecycleValue<T> private constructor(value: T?, public val expiresAt: Long) {
	public companion object {
		public fun <T> empty(): LifecycleValue<T> =
			LifecycleValue(value = null, expiresAt = Long.MIN_VALUE)
		
		public fun <T> T.expiresAt(millis: Long): LifecycleValue<T> =
			LifecycleValue(value = this, expiresAt = millis)
		
		public fun <T> T.expiresIn(millis: Long): LifecycleValue<T> =
			LifecycleValue(value = this, expiresAt = System.currentTimeMillis() + millis)
	}
	
	
	private var mValue: T? = value
	
	public val value: T?
		get() = if(expiresAt > System.currentTimeMillis()) {
			mValue
		} else {
			null // warning: leakage? IDK
		}
	
	public val unsafeValue: T? = mValue
}


public inline fun <T> LifecycleValue<T>.getOrDefault(defaultBlock: () -> T): T = value ?: defaultBlock()
