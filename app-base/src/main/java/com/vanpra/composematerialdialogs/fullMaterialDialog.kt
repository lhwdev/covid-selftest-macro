package com.vanpra.composematerialdialogs

import android.app.Dialog
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import com.lhwdev.selfTestMacro.modules.app_base.R
import com.lhwdev.selfTestMacro.ui.systemUi.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.systemUi.SystemUiMode
import java.util.UUID


@Composable
fun FullMaterialDialog(
	onDismissRequest: () -> Unit,
	properties: DialogProperties,
	content: @Composable FullMaterialDialogScope.() -> Unit
) {
	MaterialDialogBase(
		onCloseRequest = onDismissRequest
	) { info ->
		FullScreenDialog(onDismissRequest = onDismissRequest, properties = properties) {
			AutoSystemUi(
				onScreen = null,
				ime = SystemUiMode.Default
			) {
				FullMaterialDialogScope(info).content()
			}
		}
	}
}

class FullMaterialDialogScope(
	info: MaterialDialogInfo
) : MaterialDialogScope(info)


/**
 * Opens a [full screen dialog](https://material.io/components/dialogs#full-screen-dialog) with the given content.
 *
 * The dialog is visible as long as it is part of the composition hierarchy.
 * In order to let the user dismiss the Dialog, the implementation of [onDismissRequest] should
 * contain a way to remove the dialog from the composition hierarchy.
 *
 * @param onDismissRequest Executes when the user tries to dismiss the dialog.
 * @param properties [DialogProperties] for further customization of this dialog's behavior.
 * @param content The content to be displayed inside the dialog.
 */
@Composable
fun FullScreenDialog(
	onDismissRequest: () -> Unit,
	properties: DialogProperties = DialogProperties(),
	solid: Boolean = false,
	content: @Composable () -> Unit
) {
	val view = LocalView.current
	// val density = LocalDensity.current
	val layoutDirection = LocalLayoutDirection.current
	val composition = rememberCompositionContext()
	val currentContent by rememberUpdatedState(content)
	val dialogId = rememberSaveable { UUID.randomUUID() }
	val dialog = remember(view/*, density*/) {
		DialogWrapper(
			onDismissRequest = onDismissRequest,
			properties = properties,
			composeView = view,
			layoutDirection = layoutDirection,
			// density = density,
			dialogId = dialogId,
			solid = solid
		).apply {
			setContent(composition) {
				Box(
					Modifier
						.semantics { dialog() }
						.fillMaxSize(),
				) {
					currentContent()
				}
			}
		}
	}
	
	DisposableEffect(dialog) {
		dialog.show()
		
		onDispose {
			dialog.dismiss()
			dialog.disposeComposition()
		}
	}
	
	SideEffect {
		dialog.updateParameters(
			onDismissRequest = onDismissRequest,
			properties = properties,
			layoutDirection = layoutDirection
		)
	}
}

@Suppress("ViewConstructor")
private class FullScreenDialogLayout(
	context: Context,
	override val window: Window
) : AbstractComposeView(context), DialogWindowProvider {
	
	private var content: @Composable () -> Unit by mutableStateOf({})
	
	override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
		private set
	
	fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
		setParentCompositionContext(parent)
		this.content = content
		shouldCreateCompositionOnAttachedToWindow = true
		createComposition()
	}
	
	@Composable
	override fun Content() {
		content()
	}
}

@OptIn(ExperimentalComposeUiApi::class)
private class DialogWrapper(
	private var onDismissRequest: () -> Unit,
	private var properties: DialogProperties,
	private val composeView: View,
	layoutDirection: LayoutDirection,
	// density: Density,
	dialogId: UUID,
	solid: Boolean
) : Dialog(
	composeView.context,
	if(solid) R.style.FullScreenDialog_NoAnimation else R.style.FullScreenDialog
),
	ViewRootForInspector {
	
	private val dialogLayout: FullScreenDialogLayout
	
	// private val maxSupportedElevation = 30.dp
	
	override val subCompositionView: AbstractComposeView get() = dialogLayout
	
	init {
		val window = window ?: error("Dialog has no window")
		window.setBackgroundDrawableResource(android.R.color.transparent)
		window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
		// window.setLayout(300, 500)
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		dialogLayout = FullScreenDialogLayout(context, window).apply {
			// Set unique id for AbstractComposeView. This allows state restoration for the state
			// defined inside the Dialog via rememberSaveable()
			setTag(androidx.compose.ui.R.id.compose_view_saveable_id_tag, "Dialog:$dialogId")
			setTag(R.id.FullScreenDialog_Dialog, this@DialogWrapper)
			// // Enable children to draw their shadow by not clipping them
			// clipChildren = false
			// // Allocate space for elevation
			// with(density) { elevation = maxSupportedElevation.toPx() }
			// // Simple outline to force window manager to allocate space for shadow.
			// // Note that the outline affects clickable area for the dismiss listener. In case of
			// // shapes like circle the area for dismiss might be to small (rectangular outline
			// // consuming clicks outside the circle).
			// outlineProvider = object : ViewOutlineProvider() {
			// 	override fun getOutline(view: View, result: Outline) {
			// 		result.setRect(0, 0, view.width, view.height)
			// 		// We set alpha to 0 to hide the view's shadow and let the composable to draw
			// 		// its own shadow. This still enables us to get the extra space needed in the
			// 		// surface.
			// 		result.alpha = 0f
			// 	}
			// }
		}
		
		// /**
		//  * Disables clipping for [this] and all its descendant [ViewGroup]s until we reach a
		//  * [FullScreenDialogLayout] (the [ViewGroup] containing the Compose hierarchy).
		//  */
		// fun ViewGroup.disableClipping() {
		// 	clipChildren = false
		// 	if(this is FullScreenDialogLayout) return
		// 	for(i in 0 until childCount) {
		// 		(getChildAt(i) as? ViewGroup)?.disableClipping()
		// 	}
		// }
		//
		// // Turn of all clipping so shadows can be drawn outside the window
		// (window.decorView as? ViewGroup)?.disableClipping()
		setContentView(dialogLayout)
		ViewTreeLifecycleOwner.set(dialogLayout, ViewTreeLifecycleOwner.get(composeView))
		ViewTreeViewModelStoreOwner.set(dialogLayout, ViewTreeViewModelStoreOwner.get(composeView))
		ViewTreeSavedStateRegistryOwner.set(
			dialogLayout,
			ViewTreeSavedStateRegistryOwner.get(composeView)
		)
		
		// Initial setup
		updateParameters(onDismissRequest, properties, layoutDirection)
	}
	
	private fun setLayoutDirection(layoutDirection: LayoutDirection) {
		dialogLayout.layoutDirection = when(layoutDirection) {
			LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
			LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
		}
	}
	
	fun setContent(parentComposition: CompositionContext, children: @Composable () -> Unit) {
		dialogLayout.setContent(parentComposition, children)
	}
	
	private fun setSecurePolicy(securePolicy: SecureFlagPolicy) {
		val secureFlagEnabled =
			securePolicy.shouldApplySecureFlag(composeView.isFlagSecureEnabled())
		window!!.setFlags(
			if(secureFlagEnabled) {
				WindowManager.LayoutParams.FLAG_SECURE
			} else {
				WindowManager.LayoutParams.FLAG_SECURE.inv()
			},
			WindowManager.LayoutParams.FLAG_SECURE
		)
	}
	
	fun updateParameters(
		onDismissRequest: () -> Unit,
		properties: DialogProperties,
		layoutDirection: LayoutDirection
	) {
		this.onDismissRequest = onDismissRequest
		this.properties = properties
		setSecurePolicy(properties.securePolicy)
		setLayoutDirection(layoutDirection)
	}
	
	fun disposeComposition() {
		dialogLayout.disposeComposition()
	}
	
	override fun onTouchEvent(event: MotionEvent): Boolean {
		val result = super.onTouchEvent(event)
		if(result && properties.dismissOnClickOutside) {
			onDismissRequest()
		}
		
		return result
	}
	
	override fun cancel() {
		// Prevents the dialog from dismissing itself
		return
	}
	
	override fun onBackPressed() {
		if(properties.dismissOnBackPress) {
			onDismissRequest()
		}
	}
}


internal fun SecureFlagPolicy.shouldApplySecureFlag(isSecureFlagSetOnParent: Boolean): Boolean {
	return when(this) {
		SecureFlagPolicy.SecureOff -> false
		SecureFlagPolicy.SecureOn -> true
		SecureFlagPolicy.Inherit -> isSecureFlagSetOnParent
	}
}

internal fun View.isFlagSecureEnabled(): Boolean {
	val windowParams = rootView.layoutParams as? WindowManager.LayoutParams
	if(windowParams != null) {
		return (windowParams.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
	}
	return false
}
