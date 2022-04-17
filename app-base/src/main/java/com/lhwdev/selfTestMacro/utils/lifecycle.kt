package com.lhwdev.selfTestMacro.utils

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext


val LocalLifecycleState: ProvidableCompositionLocal<State<Lifecycle.State>> =
	compositionLocalOf { error("not provided") }


@Composable
fun Lifecycle.observeAsState(): State<Lifecycle.State> {
	val state = remember { mutableStateOf(currentState) }
	DisposableEffect(this) {
		val observer = LifecycleEventObserver { _, event ->
			state.value = event.targetState
		}
		addObserver(observer)
		onDispose {
			removeObserver(observer)
		}
	}
	return state
}


suspend fun State<Lifecycle.State>.repeatWhile(atLeast: Lifecycle.State, block: suspend () -> Unit) {
	snapshotFlow { value }.collectLatest {
		if(it >= atLeast) while(coroutineContext.isActive) {
			block()
		}
	}
}
