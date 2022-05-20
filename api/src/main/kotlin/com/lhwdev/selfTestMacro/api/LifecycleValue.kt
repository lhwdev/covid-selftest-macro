package com.lhwdev.selfTestMacro.api

import kotlin.reflect.KProperty


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
			mValue = null
			null
		}
	
	public operator fun getValue(receiver: Any?, property: KProperty<*>): T? = value
}
