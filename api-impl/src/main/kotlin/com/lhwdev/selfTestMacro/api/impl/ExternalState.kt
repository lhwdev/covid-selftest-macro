package com.lhwdev.selfTestMacro.api.impl


abstract class ExternalState<T> :
	SynchronizedMutableStateImpl<T>(structuralEqualityPolicy()), PreferenceItemState<T> {
	@Suppress("LeakingThis")
	override var next: StateStateRecord<T> = StateStateRecordImpl(this)
	
	override fun onPropertyUpdated() {
		next.withCurrent { it.emptyCache() }
	}
	
	override fun forceWrite() {
		next.withCurrent { it.apply() }
	}
	
	override fun prependStateRecord(value: StateRecord) {
		@Suppress("UNCHECKED_CAST")
		next = value as StateStateRecord<T>
	}
	
	abstract fun read(): T
	abstract fun write(value: T)
	
	private class StateStateRecordImpl<T>(val state: PreferenceItemStateImpl<T>, cache: Any? = sEmpty) :
		StateStateRecord<T>(cache) {
		override fun create(): StateRecord = StateStateRecordImpl(state, cache)
		
		override fun read(): T = state.read()
		
		override fun write(value: T) {
			state.write(value)
			cache = value // previously cleared by onPropertyUpdated -> it.emptyCache
		}
	}
}
