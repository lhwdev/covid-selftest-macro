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


val <T> Optional<T>.valueOrNull: T? get() = (this as? Optional.Present)?.value

inline fun <T> Optional<T>.getOrDefault(block: () -> T): T = when(this) {
	Optional.Empty -> block()
	is Optional.Present -> value
}

inline fun <T, R> Optional<T>.ifPresent(block: (T) -> R): R? =
	(this as? Optional.Present)?.let { block(it.value) }

inline fun <T, R> Optional<T>.map(block: (T) -> R): Optional<R> = if(this is Optional.Present) {
	Optional.of(block(value))
} else {
	Optional.empty()
}
