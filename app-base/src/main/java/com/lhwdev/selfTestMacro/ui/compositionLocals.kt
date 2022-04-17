package com.lhwdev.selfTestMacro.ui

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.accompanist.systemuicontroller.SystemUiController
import com.lhwdev.selfTestMacro.database.PreferenceState
import com.lhwdev.selfTestMacro.debug.LocalDebugContext
import com.lhwdev.selfTestMacro.debug.UiDebugContext
import com.lhwdev.selfTestMacro.utils.LocalLifecycleState
import com.lhwdev.selfTestMacro.utils.observeAsState


val LocalPreference = compositionLocalOf<PreferenceState> { error("not provided") }
val LocalPreview = staticCompositionLocalOf { false }

val LocalPreviewUiController = staticCompositionLocalOf<SystemUiController> { error("not provided") }

val LocalSnackbarHost = compositionLocalOf<SnackbarHostState> { error("not provided") }


@Composable
fun AppCompositionLocalsPack(
	preference: PreferenceState,
	debugContext: UiDebugContext,
	content: @Composable () -> Unit
) {
	CompositionLocalProvider(
		LocalLifecycleState provides LocalLifecycleOwner.current.lifecycle.observeAsState(),
		
		LocalPreference provides preference,
		LocalDebugContext provides debugContext,
		
		content = content
	)
}
