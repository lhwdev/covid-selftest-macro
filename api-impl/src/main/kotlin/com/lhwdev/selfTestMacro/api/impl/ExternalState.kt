package com.lhwdev.selfTestMacro.api.impl

import com.lhwdev.selfTestMacro.utils.SynchronizedStateImpl


public class ExternalState<out T>(private val readBlock: () -> T) : SynchronizedStateImpl<T>() {
	override fun read(): T = readBlock()
	
	public val cache: T?
		get() = next.currentCacheOrNull
}
