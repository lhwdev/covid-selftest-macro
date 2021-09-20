package com.lhwdev.selfTestMacro.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope


@Composable
fun DisposableLifecycleEffect(
	minimumLifecycle: Lifecycle,
	vararg keys: Any?,
	effect: DisposableEffectScope.() -> DisposableEffectResult
) {
	if(LocalLifecycle.current >= minimumLifecycle) DisposableEffect(*keys, effect = effect)
}
