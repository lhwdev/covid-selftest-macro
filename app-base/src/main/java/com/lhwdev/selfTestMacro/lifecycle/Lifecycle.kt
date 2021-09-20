package com.lhwdev.selfTestMacro.lifecycle

import androidx.compose.runtime.*


enum class Lifecycle {
	created,
	destroyed,
	initialized,
	resumed,
	started;
}


val LocalLifecycle: ProvidableCompositionLocal<Lifecycle> = compositionLocalOf { Lifecycle.started }


@Composable
fun ProvideLifecycle(lifecycle: Lifecycle, content: @Composable () -> Unit) {
	val last = LocalLifecycle.current
	val merged = if(lifecycle > last) last else lifecycle // min
	CompositionLocalProvider(
		LocalLifecycle provides merged,
		content = content
	)
}


@Composable
fun rememberLifecycle(): State<Lifecycle> = rememberUpdatedState(LocalLifecycle.current)
