package com.lhwdev.selfTestMacro.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotMutableState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile


/**
 * used for optimization
 */
@Composable
fun EmptyRestartable(content: @Composable () -> Unit) {
	content()
}


@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun changed(value: Any?): Boolean = currentComposer.changed(value)

@Composable
fun assertConstant(value: Any?) {
	val last = remember { value }
	if(last != value) error("assertConstant: $value is not consistent from $last")
}


@Composable
fun <T> lazyState(
	defaultValue: T,
	key: Any? = null,
	allowInit: Boolean = true,
	init: suspend CoroutineScope.() -> T
): State<T> {
	val state = remember { mutableStateOf(defaultValue) }
	val allowInitState by rememberUpdatedState(allowInit)
	LaunchedEffect(key) {
		state.value = defaultValue
		snapshotFlow { allowInitState }.takeWhile { !it }.collect()
		state.value = init()
	}
	return state
}


@Stable // actually not, but used to bypass recomposition
class Ref<T>(override var value: T) : SnapshotMutableState<T> {
	override fun component1(): T = value
	override fun component2(): (T) -> Unit = { value = it }
	override val policy: SnapshotMutationPolicy<T> get() = referentialEqualityPolicy() // does not matter
}


