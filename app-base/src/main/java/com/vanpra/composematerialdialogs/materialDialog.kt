package com.vanpra.composematerialdialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties


@Composable
fun MaterialDialogBase(
	onCloseRequest: () -> Unit,
	content: @Composable (MaterialDialogInfo) -> Unit
) {
	
	val info = remember {
		MaterialDialogInfo(onCloseRequest)
	}
	info.requestClose = onCloseRequest
	
	val focusManager = LocalFocusManager.current
	
	DisposableEffect(Unit) { // calling this multiple times would be poor for UX; so passing Unit
		// previous focus out of this dialog
		if(!info.hasFocusOnShow) focusManager.clearFocus()
		
		onDispose {
			if(info.hasFocusOnShow) focusManager.clearFocus()
		}
	}
	
	content(info)
}


class MaterialDialogInfo(
	var requestClose: () -> Unit
) {
	var hasFocusOnShow: Boolean = false
}


/**
 * The MaterialDialog class is used to build and display a dialog using both pre-made and
 * custom views
 */
abstract class MaterialDialogScope(
	private val info: MaterialDialogInfo
) {
	val requestClose: () -> Unit get() = info.requestClose
	
	var hasFocusOnShow: Boolean
		get() = info.hasFocusOnShow
		set(value) {
			info.hasFocusOnShow = value
		}
}


val FloatingDialogProperties = DialogProperties(usePlatformDefaultWidth = false)
val FloatingDialogMaxHeight = 480.dp


/**
 * A dialog with the given content
 * @param backgroundColor background color of the dialog
 * @param shape shape of the dialog and components used in the dialog
 * @param border border stoke of the dialog
 * @param elevation elevation of the dialog
 * @param content the body content of the dialog
 */
@Composable
fun MaterialDialog(
	onCloseRequest: () -> Unit,
	modifier: Modifier = Modifier,
	properties: DialogProperties = FloatingDialogProperties,
	backgroundColor: Color = MaterialTheme.colors.surface,
	maxHeight: Dp = FloatingDialogMaxHeight,
	shape: Shape = MaterialTheme.shapes.medium,
	border: BorderStroke? = null,
	elevation: Dp = 24.dp,
	content: @Composable FloatingMaterialDialogScope.() -> Unit
) {
	val imeInset = WindowInsets.ime
	
	MaterialDialogBase(
		onCloseRequest = onCloseRequest
	) { info ->
		Dialog(
			onDismissRequest = onCloseRequest,
			properties = properties
		) {
			Box(
				Modifier
					.windowInsetsPadding(imeInset)
					.fillMaxSize() // insets are consumed by itself so need not safeContentPadding(); it would cause problems
					.pointerInput(Unit) {
						if(properties.dismissOnClickOutside) detectTapGestures(
							onPress = { onCloseRequest() }
						)
					},
				contentAlignment = Alignment.Center
			) {
				// val focusRequester = FocusRequester()
				
				Surface(
					modifier = modifier
						.fillMaxWidth(fraction = .81f)
						.sizeIn(maxHeight = maxHeight)
						// .focusRequester(focusRequester)
						.pointerInput(Unit) {
							// override dismissOnClickOutside
							if(properties.dismissOnClickOutside) detectTapGestures()
						},
					shape = shape,
					color = backgroundColor,
					border = border,
					elevation = elevation
				) {
					Column {
						FloatingMaterialDialogScope(info, this).content()
					}
					
					// LaunchedEffect(Unit) {
					// 	if(info.hasFocusOnShow) return@LaunchedEffect
					//	
					// 	awaitFrame()
					// 	focusRequester.requestFocus()
					// }
				}
			}
		}
	}
}

class FloatingMaterialDialogScope(
	info: MaterialDialogInfo,
	columnScope: ColumnScope
) : MaterialDialogScope(info), ColumnScope by columnScope
