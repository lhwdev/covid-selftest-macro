package com.lhwdev.selfTestMacro.ui

import android.content.Context
import androidx.compose.runtime.Immutable
import com.lhwdev.selfTestMacro.navigation.Navigator
import kotlinx.coroutines.CoroutineScope


@Immutable
data class UiContext(
	val context: Context,
	val navigator: Navigator,
	val showMessage: suspend (message: String, action: String) -> Unit,
	val scope: CoroutineScope
)
