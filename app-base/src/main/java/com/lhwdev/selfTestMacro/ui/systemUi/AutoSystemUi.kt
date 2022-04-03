package com.lhwdev.selfTestMacro.ui.systemUi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.SystemUiController
import com.lhwdev.selfTestMacro.ui.isDark


sealed interface SystemUiMode {
	object Default : SystemUiMode
}

sealed interface OnScreenSystemUiMode : SystemUiMode {
	class Immersive(val scrimColor: Color = Color.Transparent) : OnScreenSystemUiMode
	class Opaque(val scrimColor: Color) : OnScreenSystemUiMode
}


@Stable
class Scrims {
	var statusBarsItem: SystemUiItem by mutableStateOf(EmptySystemUiItem)
		internal set
	var navigationBarsItem: SystemUiItem by mutableStateOf(EmptySystemUiItem)
		internal set
	
	val statusBars: @Composable () -> Unit = { statusBarsItem.ScrimContent(overrideVisibility = false) }
	val statusBarsSpacer: @Composable () -> Unit = { statusBarsItem.Spacer() }
	val navigationBars: @Composable () -> Unit = { navigationBarsItem.ScrimContent(overrideVisibility = false) }
	val navigationBarsSpacer: @Composable () -> Unit = { navigationBarsItem.Spacer() }
}


// @ChecksSdkIntAtLeast(api = 23)
// val isSystemUiDarkContentAvailable: Boolean = Build.VERSION.SDK_INT >= 23


private val LocalAutoSystemUiEnabled = compositionLocalOf { true }


@Composable
fun EnableAutoSystemUi(enabled: Boolean, content: @Composable () -> Unit) {
	CompositionLocalProvider(
		LocalAutoSystemUiEnabled provides (LocalAutoSystemUiEnabled.current and enabled)
	) {
		content()
	}
}


@Composable
fun AutoSystemUi(
	enabled: Boolean = true,
	onScreen: OnScreenSystemUiMode? = OnScreenSystemUiMode.Immersive(),
	ime: SystemUiMode? = SystemUiMode.Default,
	content: @Composable ColumnScope.(Scrims) -> Unit
) {
	AutoSystemUi(
		enabled = enabled,
		statusBars = onScreen,
		navigationBars = onScreen,
		ime = ime,
		content = content
	)
}

@Stable
interface SystemUiItem {
	@Composable
	fun ScrimContent(overrideVisibility: Boolean)
	
	@Composable
	fun Spacer()
}

@Composable
private fun AutoSystemUiData.rememberVerticalUiItem(
	mode: OnScreenSystemUiMode?,
	heightModifier: Modifier,
	setColor: (isDark: Boolean) -> Unit
): SystemUiItem = if(mode == null) {
	EmptySystemUiItem
} else {
	remember {
		VerticalSystemUiItem(this, setColor)
	}.also {
		it.mode = mode
		it.heightModifier = heightModifier
	}
}

private class VerticalSystemUiItem(val data: AutoSystemUiData, private val setColor: (isDark: Boolean) -> Unit) :
	SystemUiItem {
	var mode: OnScreenSystemUiMode? = null
	lateinit var heightModifier: Modifier
	
	@Composable
	override fun ScrimContent(overrideVisibility: Boolean) {
		data.CommonVerticalScrim(
			mode = mode,
			heightModifier = heightModifier,
			setColor = setColor,
			force = overrideVisibility
		)
	}
	
	@Composable
	override fun Spacer() {
		Box(heightModifier.fillMaxWidth())
	}
}

private object EmptySystemUiItem : SystemUiItem {
	@Composable
	override fun ScrimContent(overrideVisibility: Boolean) {
	}
	
	@Composable
	override fun Spacer() {
	}
}

@Stable
private class AutoSystemUiData(val systemUiController: SystemUiController) {
	var enabled by mutableStateOf(false)
	
	@Composable
	fun VerticalScrimContent(color: Color, heightModifier: Modifier, setColor: (isDark: Boolean) -> Unit) {
		Box(
			heightModifier
				.fillMaxWidth()
				.background(color)
		)
		
		val isDark = LocalContentColor.current.isDark
		DisposableEffect(enabled, isDark) {
			if(enabled) {
				setColor(isDark)
			}
			
			onDispose {}
		}
	}
	
	@Composable
	fun CommonVerticalScrim(
		mode: OnScreenSystemUiMode?,
		heightModifier: Modifier,
		setColor: (isDark: Boolean) -> Unit,
		force: Boolean
	) {
		when(mode) {
			null -> Unit
			is OnScreenSystemUiMode.Opaque -> if(force) VerticalScrimContent(mode.scrimColor, heightModifier, setColor)
			is OnScreenSystemUiMode.Immersive -> VerticalScrimContent(mode.scrimColor, heightModifier, setColor)
		}
	}
	
	@Composable
	fun statusBarUiItem(mode: OnScreenSystemUiMode?) = rememberVerticalUiItem(
		mode = mode,
		heightModifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars),
		setColor = { isDark ->
			systemUiController.statusBarDarkContentEnabled = isDark
			// systemUiController.setStatusBarColor(color = Color.Transparent, darkIcons = isDark)
		}
	)
	
	@Composable
	fun navigationBarUiItem(mode: OnScreenSystemUiMode?) = rememberVerticalUiItem(
		mode = mode,
		heightModifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars),
		setColor = { isDark ->
			systemUiController.navigationBarDarkContentEnabled = isDark
			// systemUiController.setNavigationBarColor(color = Color.Transparent, darkIcons = isDark)
		}
	)
}

// note that this does not clean up
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutoSystemUi(
	enabled: Boolean = true,
	statusBars: OnScreenSystemUiMode? = OnScreenSystemUiMode.Immersive(),
	navigationBars: OnScreenSystemUiMode? = OnScreenSystemUiMode.Immersive(),
	ime: SystemUiMode? = SystemUiMode.Default,
	content: @Composable ColumnScope.(Scrims) -> Unit,
) {
	val realEnabled = enabled and LocalAutoSystemUiEnabled.current
	val controller = rememberUiController()
	
	val data = remember { AutoSystemUiData(controller) }
	data.enabled = realEnabled
	
	val scrims = remember { Scrims() }
	scrims.statusBarsItem = data.statusBarUiItem(mode = statusBars)
	scrims.navigationBarsItem = data.navigationBarUiItem(mode = navigationBars)
	
	@Suppress("RedundantExplicitType")
	var modifier: Modifier = Modifier
	if(ime is SystemUiMode.Default) modifier = modifier.windowInsetsPadding(WindowInsets.ime)
	
	if(statusBars != null) modifier = modifier.consumedWindowInsets(WindowInsets.statusBars)
	if(navigationBars != null) modifier = modifier.consumedWindowInsets(WindowInsets.navigationBars)
	if(ime != null) modifier = modifier.consumedWindowInsets(WindowInsets.ime)
	
	Column(modifier) {
		if(statusBars is OnScreenSystemUiMode.Opaque)
			scrims.statusBarsItem.ScrimContent(overrideVisibility = true)
		
		Column(Modifier.weight(1f)) {
			content(scrims)
		}
		
		if(navigationBars is OnScreenSystemUiMode.Opaque)
			scrims.navigationBarsItem.ScrimContent(overrideVisibility = true)
	}
}

