package com.lhwdev.selfTestMacro.utils


sealed class Optional<out T> {
	companion object {
		fun <T> of(value: T): Present<T> = Present(value)
		fun <T : Any> nullable(value: T?): Optional<T> = if(value == null) empty() else of(value)
		
		fun <T> empty(): Optional<T> = Empty
	}
	
	class Present<T>(val value: T) : Optional<T>()
	
	object Empty : Optional<Nothing>()
}


inline fun <T> Optional<T>.getOrDefault(block: () -> T): T = when(this) {
	Optional.Empty -> block()
	is Optional.Present -> value
}
