package com.lhwdev.selfTestMacro.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.accompanist.systemuicontroller.SystemUiController
import com.lhwdev.selfTestMacro.database.PreferenceState


val LocalPreference = compositionLocalOf<PreferenceState> { error("not provided") }
val LocalPreview = staticCompositionLocalOf { false }

val LocalPreviewUiController = staticCompositionLocalOf<SystemUiController> { error("not provided") }
