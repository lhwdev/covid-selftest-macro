package com.lhwdev.selfTestMacro.utils

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.*


val sEmpty = object : Any() {
	override fun toString(): String = "sEmpty"
}

private val sApplyObserverHandle = Snapshot.registerApplyObserver { modified, _ ->
	modified.forEach {
		if(it is SynchronizedMutableStateBase<*>) {
			it.apply()
		}
	}
}


@Stable
interface SynchronizedState<out T> : State<T> {
	fun forceRead(): T
}

@Stable
interface SynchronizedMutableState<T> : SynchronizedState<T>, MutableState<T> {
	fun forceWrite()
}

abstract class SynchronizedStateBase<out T>(val policy: SnapshotMutationPolicy<@UnsafeVariance T>) :
	SynchronizedState<T>, StateObject {
	override val value: T
		get() = next.readable(this).value
	
	protected abstract val next: StateStateRecord<T>
	
	final override val firstStateRecord: StateRecord
		get() = next
	
	override fun forceRead(): T {
		return next.readable(this).forceRead()
	}
	
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
	
	abstract class StateStateRecord<out T>(protected var cache: Any? = sEmpty) : StateRecord() {
		override fun assign(value: StateRecord) {
			@Suppress("UNCHECKED_CAST")
			value as StateStateRecord<T>
			
			this.value = value.value
		}
		
		abstract fun read(): T
		
		var currentCache: Any?
			get() = synchronized(this) { cache }
			set(value) = synchronized(this) { cache = value }
		
		@Suppress("UNCHECKED_CAST")
		val currentCacheOrNull: T?
			get() = currentCache.let { if(it == sEmpty) null else it as T }
		
		fun emptyCache() {
			synchronized(this) {
				cache = sEmpty
			}
		}
		
		fun forceRead(): T {
			emptyCache()
			return value
		}
		
		open fun apply() {}
		
		var value: @UnsafeVariance T // `@UnsafeVariance`: should be writable even if variance is out
			get() = synchronized(this) {
				if(cache != sEmpty) {
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
	operator fun component1(): T = value
	
	/**
	 * A function used by the debugger to display the value of the current value of the mutable
	 * state object without triggering read observers.
	 */
	@Suppress("unused")
	val debuggerDisplayValue: T
		@JvmName("getDebuggerDisplayValue")
		get() = next.withCurrent { it.value }
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
abstract class SynchronizedMutableStateBase<T>(policy: SnapshotMutationPolicy<T>) :
	SynchronizedMutableState<T>, SynchronizedStateBase<T>(policy), SnapshotMutableState<T> {
	init {
		sApplyObserverHandle // initialize observer handle, so that this is notified when applied
	}
	
	override var value: T
		get() = super.value
		set(value) = next.withCurrent {
			if(!policy.equivalent(it.value, value)) {
				next.writable(this) { this.value = value }
			}
		}
	
	abstract override val next: MutableStateStateRecord<T>
	
	internal fun apply() {
		forceWrite()
	}
	
	override fun forceWrite() {
		next.withCurrent { it.apply() }
	}
	
	
	abstract class MutableStateStateRecord<T>(cache: Any?) : StateStateRecord<T>(cache) {
		abstract fun write(value: T)
		
		override fun apply() {
			super.apply()
			val currentCache = currentCache
			
			if(currentCache == sEmpty) return
			
			// Is there better way to do this?
			if(Snapshot.current == Snapshot.global { Snapshot.current }) {
				@Suppress("UNCHECKED_CAST")
				write(currentCache as T)
			}
			
		}
	}
	
	final override operator fun component2(): (T) -> Unit = { value = it }
}


abstract class SynchronizedStateImpl<out T>(policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()) :
	SynchronizedStateBase<T>(policy) {
	@Suppress("LeakingThis")
	override var next: StateStateRecord<@UnsafeVariance T> = StateStateRecordImpl(this)
	
	override fun prependStateRecord(value: StateRecord) {
		@Suppress("UNCHECKED_CAST")
		next = value as StateStateRecord<T>
	}
	
	protected abstract fun read(): T
	
	internal open class StateStateRecordImpl<out T>(val state: SynchronizedStateImpl<T>, cache: Any? = sEmpty) :
		StateStateRecord<T>(cache) {
		override fun create(): StateRecord = StateStateRecordImpl(state, currentCache)
		
		override fun read(): T = state.read()
	}
}

abstract class SynchronizedMutableStateImpl<T>(policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()) :
	SynchronizedMutableStateBase<T>(policy) {
	@Suppress("LeakingThis")
	override var next: MutableStateStateRecord<T> = MutableStateStateRecordImpl(this)
	
	override fun forceWrite() {
		next.withCurrent { it.apply() }
	}
	
	override fun prependStateRecord(value: StateRecord) {
		@Suppress("UNCHECKED_CAST")
		next = value as MutableStateStateRecord<T>
	}
	
	protected abstract fun read(): T
	protected abstract fun write(value: T)
	
	private class MutableStateStateRecordImpl<T>(val state: SynchronizedMutableStateImpl<T>, cache: Any? = sEmpty) :
		MutableStateStateRecord<T>(cache) {
		override fun create(): StateRecord = MutableStateStateRecordImpl(state, currentCache)
		
		override fun read(): T = state.read()
		
		override fun write(value: T) {
			state.write(value)
			currentCache = value // maybe previously cleared
		}
	}
}
