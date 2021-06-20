package com.lhwdev.selfTestMacro

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
private val sLocalAppliedUiPaddings = compositionLocalOf { AppliedUiPaddings() }
val LocalAppliedUiPaddings: CompositionLocal<AppliedUiPaddings> = sLocalAppliedUiPaddings

@Composable
fun ProvideAppliedUiPaddings(paddings: AppliedUiPaddings, content: @Composable () -> Unit) {
	CompositionLocalProvider(
		sLocalAppliedUiPaddings provides LocalAppliedUiPaddings.current.merge(paddings),
		content = content
	)
}


fun Modifier.autoPadding(
	statusBar: Boolean = true,
): Modifier = composed(inspectorInfo = debugInspectorInfo {
	name = "autoPadding"
	properties["statusBar"] = statusBar
}) {
	val insets = LocalWindowInsets.current
	
	padding(
		rememberInsetsPaddingValues(
			derivedWindowInsetsTypeOf(
				*listOfNotNull(
					if(statusBar) insets.statusBars else null,
					insets.navigationBars,
					insets.ime
				).toTypedArray()
			)
		)
	)
}


val ScrimLightColor = Color.White.copy(alpha = 0.1f)
val ScrimDarkColor = Color.Black.copy(alpha = 0.1f)
val ScrimDarkerColor = Color.Black.copy(alpha = 0.17f)

sealed interface SystemUiMode {
	object Default : SystemUiMode
}

sealed interface OnScreenSystemUiMode : SystemUiMode {
	class Immersive(val scrimColor: Color = ScrimDarkColor) : OnScreenSystemUiMode
	object Opaque : OnScreenSystemUiMode
}


class Scrims(
	val statusBar: @Composable () -> Unit,
	val navigationBar: @Composable () -> Unit,
)

private fun Color.isDarkColor(): Boolean = luminance() < 0.5f


@Composable
fun rememberPreviewUiController(): SystemUiController = LocalPreviewUiController.current


val isSystemUiDarkContentAvailable: Boolean = Build.VERSION.SDK_INT >= 23


@Composable
fun PreviewSideEffect(effect: () -> Unit) {
	if(LocalPreview.current) effect()
	else SideEffect(effect)
}

@Composable
fun rememberUiController(): SystemUiController = if(LocalPreview.current) {
	rememberPreviewUiController()
} else {
	rememberSystemUiController()
}


@Composable
fun AutoSystemUi(
	enabled: Boolean,
	onScreenMode: OnScreenSystemUiMode? = OnScreenSystemUiMode.Immersive(),
	ime: SystemUiMode? = SystemUiMode.Default,
	content: @Composable (Scrims) -> Unit,
) {
	AutoSystemUi(
		enabled = enabled,
		statusBarMode = onScreenMode,
		navigationBarMode = onScreenMode,
		ime = ime,
		content = content
	)
}

// note that this does not clean-up
@Composable
fun AutoSystemUi(
	enabled: Boolean,
	statusBarMode: OnScreenSystemUiMode? = OnScreenSystemUiMode.Immersive(),
	navigationBarMode: OnScreenSystemUiMode? = OnScreenSystemUiMode.Immersive(),
	ime: SystemUiMode? = SystemUiMode.Default,
	content: @Composable (Scrims) -> Unit,
) {
	val appliedUiPaddings = LocalAppliedUiPaddings.current
	val enabledState by rememberUpdatedState(enabled)
	
	val statusBarState by rememberUpdatedState(
		if(appliedUiPaddings.statusBar) null else statusBarMode
	)
	val navigationBarState by rememberUpdatedState(
		if(appliedUiPaddings.navigationBar) null else navigationBarMode
	)
	val imeState = if(appliedUiPaddings.ime) null else ime
	
	val controller = rememberUiController()
	
	val scrims = remember {
		Scrims(
			statusBar = {
				val statusBar = statusBarState
				if(statusBar is OnScreenSystemUiMode.Immersive) {
					Box(Modifier
						.background(statusBar.scrimColor)
						.statusBarsHeight())
					val isDark = LocalContentColor.current.isDarkColor()
					if(enabledState) PreviewSideEffect {
						controller.statusBarDarkContentEnabled = isDark
					}
				}
			},
			navigationBar = {
				val navigationBar = navigationBarState
				if(navigationBar is OnScreenSystemUiMode.Immersive) {
					Box(Modifier
						.background(navigationBar.scrimColor)
						.navigationBarsHeight())
					val isDark = LocalContentColor.current.isDarkColor()
					if(enabledState) PreviewSideEffect {
						controller.navigationBarDarkContentEnabled = isDark
					}
				}
			}
		)
	}
	
	var modifier: Modifier = Modifier
	if(imeState is SystemUiMode.Default) modifier = modifier.imePadding()
	if(statusBarMode is OnScreenSystemUiMode.Opaque) modifier = modifier.statusBarsPadding()
	if(navigationBarMode is OnScreenSystemUiMode.Opaque) modifier = modifier.navigationBarsPadding()
	
	ProvideAppliedUiPaddings(
		AppliedUiPaddings(
			statusBar = statusBarMode != null,
			navigationBar = navigationBarMode != null,
			ime = ime != null
		)
	) {
		Box(modifier) {
			content(scrims)
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
			Box(Modifier.navigationBarsPadding()) {
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

