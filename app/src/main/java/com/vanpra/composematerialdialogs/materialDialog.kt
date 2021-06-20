package com.vanpra.composematerialdialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties


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
	autoDismiss: Boolean = true,
	properties: DialogProperties = DialogProperties(),
	backgroundColor: Color = MaterialTheme.colors.surface,
	shape: Shape = MaterialTheme.shapes.medium,
	border: BorderStroke? = null,
	elevation: Dp = 24.dp,
	content: @Composable MaterialDialogScope.() -> Unit
) {
	val scope = remember {
		MaterialDialogScope(autoDismiss = autoDismiss, onCloseRequest = onCloseRequest)
	}
	val focusManager = LocalFocusManager.current
	
	DisposableEffect(Unit) { // calling this multiple times would be poor for UX; so passing Unit
		// previous focus out of this dialog
		if(!scope.hasFocusOnShow) focusManager.clearFocus()
		
		onDispose {
			if(scope.hasFocusOnShow) focusManager.clearFocus()
		}
	}
	
	ThemedDialog(onCloseRequest = onCloseRequest, properties = properties) {
		/* Only using 40.dp padding as 8.dp is already provided */
		Surface(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 40.dp)
				.sizeIn(maxWidth = 560.dp, maxHeight = 560.dp)
				.clipToBounds(),
			shape = shape,
			color = backgroundColor,
			border = border,
			elevation = elevation
		) {
			content(scope)
		}
	}
}


/**
 *  The MaterialDialog class is used to build and display a dialog using both pre-made and
 * custom views
 *
 * @param autoDismiss when true the dialog will be automatically dismissed when a positive or
 * negative button is pressed
 * @param onCloseRequest a callback for when the user tries to exit the dialog by clicking outside
 * the dialog. This callback takes the current MaterialDialog as
 * a parameter to allow for the hide method of the dialog to be called if required. By default
 * this callback hides the dialog.
 */
class MaterialDialogScope(
	val autoDismiss: Boolean = true,
	val onCloseRequest: () -> Unit,
) {
	var hasFocusOnShow: Boolean = false
}
