package com.lhwdev.selfTestMacro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.lhwdev.selfTestMacro.navigation.LocalNavigator


// https://issuetracker.google.com/issues/217770337
val WindowInsets.isVisible: Boolean
	@Composable get() {
		val density = LocalDensity.current
		val direction = LocalLayoutDirection.current
		
		return isVisible(density, direction)
	}

@Composable
fun WindowInsets.rememberIsVisible(): State<Boolean> {
	val pair = rememberUpdatedState(LocalDensity.current to LocalLayoutDirection.current)
	return remember {
		derivedStateOf {
			val (density, direction) = pair.value
			isVisible(density, direction)
		}
	}
}

fun WindowInsets.isVisible(density: Density, direction: LayoutDirection): Boolean {
	return getTop(density) != 0 ||
		getBottom(density) != 0 ||
		getLeft(density, direction) != 0 ||
		getRight(density, direction) != 0
}


val ScrimNavSurfaceColor
	@Composable get() = MaterialTheme.colors.surface.copy(alpha = 0.7f)

sealed interface SystemUiMode {
	object Default : SystemUiMode
}

sealed interface OnScreenSystemUiMode : SystemUiMode {
	class Immersive(val scrimColor: Color = Color.Transparent) : OnScreenSystemUiMode
	class Opaque(val scrimColor: Color) : OnScreenSystemUiMode
}


class Scrims(
	val statusBar: @Composable () -> Unit,
	val statusBarSpacer: @Composable () -> Unit,
	val navigationBar: @Composable () -> Unit,
	val navigationBarSpacer: @Composable () -> Unit,
)

private fun Color.isDarkColor(): Boolean = luminance() < 0.5f


@Composable
fun rememberPreviewUiController(): SystemUiController = LocalPreviewUiController.current


// @ChecksSdkIntAtLeast(api = 23)
// val isSystemUiDarkContentAvailable: Boolean = Build.VERSION.SDK_INT >= 23


private val LocalIsImeVisible: ProvidableCompositionLocal<Boolean> =
	compositionLocalOf { error("not provided") }

val isImeVisible: Boolean @Composable get() = LocalIsImeVisible.current


@Composable
fun rememberUiController(): SystemUiController = if(LocalPreview.current) {
	rememberPreviewUiController()
} else {
	rememberSystemUiController()
}


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
	val enabledState by rememberUpdatedState(realEnabled)
	
	val controller = rememberUiController()
	
	@Composable
	fun StatusBarScrim(color: Color) {
		Box(
			Modifier
				.windowInsetsTopHeight(WindowInsets.statusBars)
				.fillMaxWidth()
				.background(color)
		)
		
		val isDark = LocalContentColor.current.isDarkColor()
		if(enabledState) SideEffect {
			controller.statusBarDarkContentEnabled = isDark
		}
	}
	
	@Composable
	fun NavigationBarScrim(color: Color) {
		Box(
			Modifier
				.windowInsetsBottomHeight(WindowInsets.navigationBars)
				.fillMaxWidth()
				.background(color)
		)
		
		val isDark = LocalContentColor.current.isDarkColor()
		val nav = LocalNavigator
		if(enabledState) SideEffect {
			controller.navigationBarDarkContentEnabled = isDark
		}
	}
	
	val statusBarsState by rememberUpdatedState(statusBars)
	val navigationBarsState by rememberUpdatedState(navigationBars)
	
	val statusBarsInset = WindowInsets.statusBars
	val navigationBarsInset = WindowInsets.navigationBars
	val imeInset = WindowInsets.ime
	
	val scrims = remember {
		Scrims(
			statusBar = {
				val statusBar = statusBarsState
				if(statusBar is OnScreenSystemUiMode.Immersive) {
					StatusBarScrim(statusBar.scrimColor)
				}
			},
			statusBarSpacer = {
				if(statusBarsState is OnScreenSystemUiMode.Immersive)
					Spacer(Modifier.windowInsetsTopHeight(statusBarsInset).fillMaxWidth())
			},
			navigationBar = {
				val navigationBar = navigationBarsState
				if(navigationBar is OnScreenSystemUiMode.Immersive) {
					NavigationBarScrim(navigationBar.scrimColor)
				}
			},
			navigationBarSpacer = {
				if(statusBarsState is OnScreenSystemUiMode.Immersive)
					Spacer(Modifier.windowInsetsBottomHeight(navigationBarsInset).fillMaxWidth())
			}
		)
	}
	
	@Suppress("RedundantExplicitType")
	var modifier: Modifier = Modifier
	if(ime is SystemUiMode.Default) modifier = modifier.windowInsetsPadding(imeInset)
	// if(statusBarMode is OnScreenSystemUiMode.Opaque) modifier = modifier.statusBarsPadding()
	// if(navigationBarMode is OnScreenSystemUiMode.Opaque) modifier = modifier.navigationBarsPadding()
	
	if(statusBars != null) modifier = modifier.consumedWindowInsets(statusBarsInset)
	if(navigationBars != null) modifier = modifier.consumedWindowInsets(navigationBarsInset)
	if(ime != null) modifier = modifier.consumedWindowInsets(imeInset)
	
	Column(
		modifier
	) {
		if(statusBars is OnScreenSystemUiMode.Opaque)
			StatusBarScrim(statusBars.scrimColor)
		
		Column(Modifier.weight(1f)) {
			content(scrims)
		}
		
		if(navigationBars is OnScreenSystemUiMode.Opaque)
			NavigationBarScrim(navigationBars.scrimColor)
	}
}

@Composable
fun TopAppBar(
	title: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	statusBarScrim: @Composable () -> Unit,
	navigationIcon: @Composable (() -> Unit)? = null,
	actions: @Composable RowScope.() -> Unit = {},
	backgroundColor: Color = MaterialTheme.colors.primarySurface,
	contentColor: Color = contentColorFor(backgroundColor),
	elevation: Dp = AppBarDefaults.TopAppBarElevation,
) {
	Surface(
		color = backgroundColor,
		elevation = elevation
	) {
		Column {
			statusBarScrim()
			
			TopAppBar(
				title = title,
				modifier = modifier,
				navigationIcon = navigationIcon,
				actions = actions,
				backgroundColor = Color.Transparent,
				contentColor = contentColor,
				elevation = 0.dp
			)
		}
	}
}


@Composable
fun AutoScaffold(
	modifier: Modifier = Modifier,
	scaffoldState: ScaffoldState = rememberScaffoldState(),
	topBar: @Composable () -> Unit = {},
	bottomBar: @Composable () -> Unit = {},
	snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
	floatingActionButton: @Composable () -> Unit = {},
	floatingActionButtonPosition: FabPosition = FabPosition.End,
	isFloatingActionButtonDocked: Boolean = false,
	drawerContent: @Composable (ColumnScope.() -> Unit)? = null,
	drawerGesturesEnabled: Boolean = true,
	drawerShape: Shape = MaterialTheme.shapes.large,
	drawerElevation: Dp = DrawerDefaults.Elevation,
	drawerBackgroundColor: Color = MaterialTheme.colors.surface,
	drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
	drawerScrimColor: Color = DrawerDefaults.scrimColor,
	backgroundColor: Color = MaterialTheme.colors.background,
	contentColor: Color = contentColorFor(backgroundColor),
	content: @Composable (PaddingValues) -> Unit,
) {
	Scaffold(
		modifier = modifier,
		scaffoldState = scaffoldState,
		topBar = topBar,
		bottomBar = bottomBar,
		snackbarHost = {
			Box(Modifier.safeContentPadding()) {
				snackbarHost(it)
			}
		},
		floatingActionButton = floatingActionButton,
		floatingActionButtonPosition = floatingActionButtonPosition,
		isFloatingActionButtonDocked = isFloatingActionButtonDocked,
		drawerContent = drawerContent,
		drawerGesturesEnabled = drawerGesturesEnabled,
		drawerShape = drawerShape,
		drawerElevation = drawerElevation,
		drawerBackgroundColor = drawerBackgroundColor,
		drawerContentColor = drawerContentColor,
		drawerScrimColor = drawerScrimColor,
		backgroundColor = backgroundColor,
		contentColor = contentColor,
		content = content
	)
}

