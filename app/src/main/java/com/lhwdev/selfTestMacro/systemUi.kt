package com.lhwdev.selfTestMacro

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.accompanist.insets.*
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch


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
	class Opaque(val scrimColor: Color = Color.Black) : OnScreenSystemUiMode
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
fun isImeVisible(): State<Boolean> {
	val view = LocalView.current
	val state = remember { mutableStateOf(false) }
	
	DisposableEffect(view) {
		ViewCompat.setOnApplyWindowInsetsListener(view.rootView) { _, insets ->
			state.value = insets.isVisible(WindowInsetsCompat.Type.ime())
			insets
		}
		
		onDispose { ViewCompat.setOnApplyWindowInsetsListener(view, null) }
	}
	
	return state
}


private val WindowInsets.Type.insets: Insets
	get() = if(animationInProgress) animatedInsets else layoutInsets

/**
 * Immutable implementation of [Insets].
 */
@Immutable
internal class ImmutableInsets(
	override val left: Int = 0,
	override val top: Int = 0,
	override val right: Int = 0,
	override val bottom: Int = 0,
) : Insets

fun lerp(start: Int, stop: Int, fraction: Float): Int = (start + (stop - start) * fraction).toInt()

fun lerp(start: Insets, stop: Insets, fraction: Float): Insets = ImmutableInsets(
	left = lerp(start.left, stop.left, fraction),
	top = lerp(start.top, stop.top, fraction),
	right = lerp(start.right, stop.right, fraction),
	bottom = lerp(start.bottom, stop.bottom, fraction)
)


@Composable
fun <T, V : AnimationVector> animateValueAsAnimatable(
	targetValue: T,
	typeConverter: TwoWayConverter<T, V>,
	animationSpec: AnimationSpec<T> = remember {
		spring(visibilityThreshold = visibilityThreshold)
	},
	visibilityThreshold: T? = null,
	finishedListener: ((T) -> Unit)? = null
): Animatable<T, V> {
	val animatable = remember { Animatable(targetValue, typeConverter) }
	val listener by rememberUpdatedState(finishedListener)
	val animSpec by rememberUpdatedState(animationSpec)
	val channel = remember { Channel<T>(Channel.CONFLATED) }
	SideEffect {
		channel.trySend(targetValue)
	}
	LaunchedEffect(channel) {
		for(target in channel) {
			// This additional poll is needed because when the channel suspends on receive and
			// two values are produced before consumers' dispatcher resumes, only the first value
			// will be received.
			// It may not be an issue elsewhere, but in animation we want to avoid being one
			// frame late.
			val newTarget = channel.tryReceive().getOrNull() ?: target
			launch {
				if(newTarget != animatable.targetValue) {
					animatable.animateTo(newTarget, animSpec)
					listener?.invoke(animatable.value)
				}
			}
		}
	}
	return animatable
}

@Composable
fun ProvideAutoWindowInsets(
	consumeWindowInsets: Boolean = true,
	windowInsetsAnimationsEnabled: Boolean = true,
	content: @Composable () -> Unit
) {
	val isImeVisible by isImeVisible()
	
	ProvideWindowInsets(
		consumeWindowInsets = consumeWindowInsets,
		windowInsetsAnimationsEnabled = windowInsetsAnimationsEnabled
	) {
		// windows inset animation
		if(Build.VERSION.SDK_INT >= 29) {
			var imeMaxHeight by remember { mutableStateOf(0) }
			val insets = LocalWindowInsets.current
			val nav = insets.navigationBars
			val ime = insets.ime
			
			val lerpFraction: Float
			val animationFraction: Float
			
			
			// while ime is showing, navigation bar becomes `opaque state`, but some part of app
			// uses immersive style so expects navigation bar padding to not exist, adding its
			// custom padding.
			
			
			// this is not quite accurate, just an effort not to be seen a lot weird.
			// maybe a little bit better?
			when {
				// 1. not animating
				!ime.animationInProgress -> {
					lerpFraction = if(isImeVisible) 0f else 1f
					animationFraction = 0f
				}
				
				// 2. ime is dismissing
				ime.layoutInsets.bottom == 0 -> { // already dismissed; layoutInsets foretells us
					animationFraction = 1f - ime.animatedInsets.bottom.toFloat() / imeMaxHeight.toFloat()
					lerpFraction = animationFraction
				}
				
				// 3. ime is showing
				else -> {
					imeMaxHeight = ime.layoutInsets.bottom
					animationFraction = ime.animatedInsets.bottom.toFloat() / imeMaxHeight.toFloat()
					lerpFraction = 1f - animationFraction
				}
			}
			
			val navInsets = lerp(Insets.Empty, nav.insets, lerpFraction)
			
			val animationFractionState by rememberUpdatedState(animationFraction)
			val navInsetsState by rememberUpdatedState(navInsets)
			
			val newInsets = remember(insets) {
				object : WindowInsets by insets {
					override val navigationBars: WindowInsets.Type = object : WindowInsets.Type {
						override val isVisible: Boolean get() = nav.isVisible
						override val layoutInsets: Insets get() = navInsetsState
						override val animatedInsets: Insets get() = navInsetsState
						override val animationFraction: Float get() = animationFractionState // I don't want to implement; I'm lazy
						override val animationInProgress: Boolean get() = ime.animationInProgress
					}
				}
			}
			
			CompositionLocalProvider(
				LocalWindowInsets provides newInsets
			) { content() }
		} else {
			content()
		}
	}
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

// note that this does not clean-up
@Composable
fun AutoSystemUi(
	enabled: Boolean,
	statusBarMode: OnScreenSystemUiMode? = OnScreenSystemUiMode.Immersive(),
	navigationBarMode: OnScreenSystemUiMode? = OnScreenSystemUiMode.Immersive(),
	ime: SystemUiMode? = SystemUiMode.Default,
	content: @Composable ColumnScope.(Scrims) -> Unit,
) {
	val appliedUiPaddings = LocalAppliedUiPaddings.current
	val enabledState by rememberUpdatedState(enabled)
	
	val controller = rememberUiController()
	
	
	@Composable
	fun StatusBarScrim(color: Color) {
		Box(Modifier.background(color).statusBarsHeight())
		
		val isDark = LocalContentColor.current.isDarkColor()
		if(enabledState) PreviewSideEffect {
			controller.statusBarDarkContentEnabled = isDark
		}
	}
	
	@Composable
	fun NavigationBarScrim(color: Color) {
		Box(Modifier.background(color).navigationBarsHeight())
		
		val isDark = LocalContentColor.current.isDarkColor()
		if(enabledState) PreviewSideEffect {
			controller.navigationBarDarkContentEnabled = isDark
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
			navigationBar = {
				val navigationBar = navigationBarState
				if(navigationBar is OnScreenSystemUiMode.Immersive) {
					NavigationBarScrim(navigationBar.scrimColor)
				}
			}
		)
	}
	
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
				StatusBarScrim(statusBarMode.scrimColor) // color: ???
			
			content(this, scrims)
			
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

