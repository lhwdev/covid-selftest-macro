package com.lhwdev.selfTestMacro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.accompanist.systemuicontroller.SystemUiController
import com.lhwdev.selfTestMacro.database.PreferenceState
import com.lhwdev.selfTestMacro.debug.DebugContext


val LocalPreference = compositionLocalOf<PreferenceState> { error("not provided") }
val LocalPreview = staticCompositionLocalOf { false }

val LocalPreviewUiController = staticCompositionLocalOf<SystemUiController> { error("not provided") }

val LocalDebugContextGlobal = compositionLocalOf<DebugContext> { error("not provided") }

val LocalDebugContext = compositionLocalOf<DebugContext> { error("not provided") }


@Composable
fun AppCompositionLocalsPack(
	preference: PreferenceState,
	debugContext: DebugContext,
	content: @Composable () -> Unit
) {
	CompositionLocalProvider(
		LocalPreference provides preference,
		LocalDebugContextGlobal provides debugContext,
		LocalDebugContext provides debugContext,
		content = content
	)
}
