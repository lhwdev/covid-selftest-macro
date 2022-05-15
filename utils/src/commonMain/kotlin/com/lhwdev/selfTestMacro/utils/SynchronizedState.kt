package com.lhwdev.selfTestMacro.utils

import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.snapshots.*


val sEmpty = object : Any() {
	override fun toString(): String = "sEmpty"
}

private val sApplyObserverHandle = Snapshot.registerApplyObserver { modified, _ ->
	modified.forEach {
		if(it is SynchronizedMutableStateImpl<*>) {
			it.apply()
		}
	}
}

/**
 * A single value holder whose reads and writes are observed by Compose, and the value is synchronized to
 * external sources.
 * Write operation is done only if the value is written on global snapshot.
 *
 * Additionally, writes to it are transacted as part of the [Snapshot] system.
 *
 * @param policy a policy to control how changes are handled in a mutable snapshot.
 *
 * @see SnapshotMutationPolicy
 */
abstract class SynchronizedMutableStateImpl<T>(override val policy: SnapshotMutationPolicy<T>) :
	StateObject, SnapshotMutableState<T> {
	init {
		sApplyObserverHandle
	}
	
	@Suppress("UNCHECKED_CAST")
	final override var value: T
		get() = next.readable(this).value
		set(value) = next.withCurrent {
			if(!policy.equivalent(it.value, value)) {
				next.writable(this) { this.value = value }
			}
		}
	
	fun apply() {
		next.withCurrent { it.apply() }
	}
	
	
	protected abstract val next: StateStateRecord<T>
	
	final override val firstStateRecord: StateRecord
		get() = next
	
	@Suppress("UNCHECKED_CAST")
	final override fun mergeRecords(
		previous: StateRecord,
		current: StateRecord,
		applied: StateRecord
	): StateRecord? {
		val previousRecord = previous as StateStateRecord<T>
		val currentRecord = current as StateStateRecord<T>
		val appliedRecord = applied as StateStateRecord<T>
		return if(policy.equivalent(currentRecord.value, appliedRecord.value))
			current
		else {
			val merged = policy.merge(
				previousRecord.value,
				currentRecord.value,
				appliedRecord.value
			)
			if(merged != null) {
				appliedRecord.create().also {
					it as StateStateRecord<T>
					it.value = merged
				}
			} else {
				null
			}
		}
	}
	
	override fun toString(): String = next.withCurrent {
		"SynchronizedState(value=${it.value})@${hashCode()}"
	}
	
	protected abstract class StateStateRecord<T>(protected var cache: Any? = sEmpty) : StateRecord() {
		override fun assign(value: StateRecord) {
			@Suppress("UNCHECKED_CAST")
			value as StateStateRecord<T>
			
			this.value = value.value
		}
		
		abstract fun read(): T
		abstract fun write(value: T)
		
		fun emptyCache() {
			cache = sEmpty
		}
		
		fun apply() {
			if(cache == sEmpty) return
			
			// Is there better way to do this?
			if(Snapshot.current == Snapshot.global { Snapshot.current }) {
				@Suppress("UNCHECKED_CAST")
				write(cache as T)
			}
		}
		
		var value: T
			get() = if(cache != sEmpty) {
				@Suppress("UNCHECKED_CAST")
				cache as T
			} else synchronized(this) {
				if(cache != sEmpty) { // double check
					@Suppress("UNCHECKED_CAST")
					cache as T
				} else {
					val new = read()
					cache = new
					new
				}
			}
			set(value) = synchronized(this) {
				if(cache != value) {
					cache = value
					apply()
				}
			}
	}
	
	/**
	 * The componentN() operators allow state objects to be used with the property destructuring
	 * syntax
	 *
	 * ```
	 * var (foo, setFoo) = remember { mutableStateOf(0) }
	 * setFoo(123) // set
	 * foo == 123 // get
	 * ```
	 */
	final override operator fun component1(): T = value
	
	final override operator fun component2(): (T) -> Unit = { value = it }
	
	/**
	 * A function used by the debugger to display the value of the current value of the mutable
	 * state object without triggering read observers.
	 */
	@Suppress("unused")
	val debuggerDisplayValue: T
		@JvmName("getDebuggerDisplayValue")
		get() = next.withCurrent { it.value }
}
