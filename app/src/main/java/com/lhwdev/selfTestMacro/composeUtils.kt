package com.lhwdev.selfTestMacro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope


@Composable
fun <T> lazyState(init: suspend CoroutineScope.() -> T): State<T?> {
	val state = remember { mutableStateOf<T?>(null) }
	LaunchedEffect(null) {
		state.value = init()
	}
	return state
}


@Composable
fun TextSwitch(
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit,
	text: @Composable () -> Unit,
	switch: @Composable () -> Unit
) {
	Surface(
		modifier = Modifier.clickable { onCheckedChange(!checked) }
			.fillMaxWidth()
	) {
		Row {
			Box(Modifier.weight(1f)) { text() }
			switch()
		}
	}
}
