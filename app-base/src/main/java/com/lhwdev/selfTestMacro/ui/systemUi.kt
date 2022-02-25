package com.lhwdev.selfTestMacro.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.accompanist.insets.*
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController


data class AppliedUiPaddings(
	val statusBar: Boolean = false,
	val navigationBar: Boolean = false,
	val ime: Boolean = false,
) {
	fun merge(other: AppliedUiPaddings): AppliedUiPaddings = AppliedUiPaddings(
		statusBar = other.statusBar || statusBar,
		navigationBar = other.navigationBar || navigationBar,
		ime = other.ime || ime
	)
}

@SuppressLint("CompositionLocalNaming")
val sLocalAppliedUiPaddings = compositionLocalOf { AppliedUiPaddings() }
val LocalAppliedUiPaddings: CompositionLocal<AppliedUiPaddings> = sLocalAppliedUiPaddings

@Composable
fun ProvideAppliedUiPaddings(paddings: AppliedUiPaddings, content: @Composable () -> Unit) {
	CompositionLocalProvider(
		sLocalAppliedUiPaddings provides LocalAppliedUiPaddings.current.merge(paddings),
		content = content
	)
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
fun ProvideIsImeVisible(content: @Composable () -> Unit) {
	val view = LocalView.current
	var state by remember { mutableStateOf(false) }
	
	DisposableEffect(view) {
		ViewCompat.setOnApplyWindowInsetsListener(view.rootView) { _, insets ->
			val s = insets.isVisible(WindowInsetsCompat.Type.ime())
			state = s
			insets
		}
		
		onDispose { ViewCompat.setOnApplyWindowInsetsListener(view, null) }
	}
	
	CompositionLocalProvider(LocalIsImeVisible provides state) {
		content()
	}
}


@Composable
fun ProvideAutoWindowInsets(
	consumeWindowInsets: Boolean = true,
	windowInsetsAnimationsEnabled: Boolean = true,
	content: @Composable () -> Unit
) {
	ProvideIsImeVisible {
		val isImeVisible = isImeVisible
		
		ProvideWindowInsets(
			consumeWindowInsets = consumeWindowInsets,
			windowInsetsAnimationsEnabled = windowInsetsAnimationsEnabled
		) {
			ProvideAppliedUiPaddings(
				AppliedUiPaddings(navigationBar = isImeVisible)
			) {
				content()
			}
		}
	}
}


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
	onScreenMode: OnScreenSystemUiMode? = OnScreenSystemUiMode.Immersive(),
	ime: SystemUiMode? = SystemUiMode.Default,
	content: @Composable ColumnScope.(Scrims) -> Unit
) {
	AutoSystemUi(
		enabled = enabled,
		statusBarMode = onScreenMode,
		navigationBarMode = onScreenMode,
		ime = ime,
		content = content
	)
}

// note that this does not clean up
@Composable
fun AutoSystemUi(
	enabled: Boolean = true,
	statusBarMode: OnScreenSystemUiMode? = OnScreenSystemUiMode.Immersive(),
	navigationBarMode: OnScreenSystemUiMode? = OnScreenSystemUiMode.Immersive(),
	ime: SystemUiMode? = SystemUiMode.Default,
	appliedUiPaddings: AppliedUiPaddings = LocalAppliedUiPaddings.current,
	content: @Composable ColumnScope.(Scrims) -> Unit,
) {
	val realEnabled = enabled and LocalAutoSystemUiEnabled.current
	val enabledState by rememberUpdatedState(realEnabled)
	
	val controller = rememberUiController()
	
	@Composable
	fun StatusBarScrim(color: Color) {
		Box(
			Modifier
				.statusBarsHeight()
				.fillMaxWidth()
				.background(color)
		)
		
		val isDark = LocalContentColor.current.isDarkColor()
		if(enabledState) DisposableEffect(Unit) {
			println("status $isDark last=${controller.statusBarDarkContentEnabled}")
			controller.statusBarDarkContentEnabled = isDark
			println("status updated? last=${controller.statusBarDarkContentEnabled}")
			onDispose {}
		}
	}
	
	@Composable
	fun NavigationBarScrim(color: Color) {
		Box(
			Modifier
				.navigationBarsHeight()
				.fillMaxWidth()
				.background(color)
		)
		
		val isDark = LocalContentColor.current.isDarkColor()
		if(enabledState) DisposableEffect(Unit) {
			controller.navigationBarDarkContentEnabled = isDark
			onDispose {}
		}
	}
	
	val statusBarState by rememberUpdatedState(
		if(appliedUiPaddings.statusBar) null else statusBarMode
	)
	val navigationBarState by rememberUpdatedState(
		if(appliedUiPaddings.navigationBar) null else navigationBarMode
	)
	val imeState = if(appliedUiPaddings.ime) null else ime
	
	
	val scrims = remember {
		Scrims(
			statusBar = {
				val statusBar = statusBarState
				if(statusBar is OnScreenSystemUiMode.Immersive) {
					StatusBarScrim(statusBar.scrimColor)
				}
			},
			statusBarSpacer = {
				if(statusBarState is OnScreenSystemUiMode.Immersive)
					Spacer(Modifier.statusBarsHeight().fillMaxWidth())
			},
			navigationBar = {
				val navigationBar = navigationBarState
				if(navigationBar is OnScreenSystemUiMode.Immersive) {
					NavigationBarScrim(navigationBar.scrimColor)
				}
			},
			navigationBarSpacer = {
				if(statusBarState is OnScreenSystemUiMode.Immersive)
					Spacer(Modifier.navigationBarsHeight().fillMaxWidth())
			}
		)
	}
	
	@Suppress("RedundantExplicitType")
	var modifier: Modifier = Modifier
	if(imeState is SystemUiMode.Default) modifier = modifier.imePadding()
	// if(statusBarMode is OnScreenSystemUiMode.Opaque) modifier = modifier.statusBarsPadding()
	// if(navigationBarMode is OnScreenSystemUiMode.Opaque) modifier = modifier.navigationBarsPadding()
	
	ProvideAppliedUiPaddings(
		AppliedUiPaddings(
			statusBar = statusBarMode != null,
			navigationBar = navigationBarMode != null,
			ime = ime != null
		)
	) {
		Column(modifier) {
			if(statusBarMode is OnScreenSystemUiMode.Opaque)
				StatusBarScrim(statusBarMode.scrimColor)
			
			Column(Modifier.weight(1f)) {
				content(scrims)
			}
			
			if(navigationBarMode is OnScreenSystemUiMode.Opaque)
				NavigationBarScrim(navigationBarMode.scrimColor)
		}
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
			Box(Modifier.navigationBarsWithImePadding()) {
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

