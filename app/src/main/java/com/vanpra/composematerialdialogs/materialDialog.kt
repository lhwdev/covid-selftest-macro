package com.vanpra.composematerialdialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
	properties: DialogProperties = DialogProperties(),
	backgroundColor: Color = MaterialTheme.colors.surface,
	shape: Shape = MaterialTheme.shapes.medium,
	border: BorderStroke? = null,
	elevation: Dp = 24.dp,
	content: @Composable MaterialDialogScope.() -> Unit
) {
	val info = remember {
		MaterialDialogInfo(onCloseRequest)
	}
	info.onCloseRequest = onCloseRequest
	
	val focusManager = LocalFocusManager.current
	
	DisposableEffect(Unit) { // calling this multiple times would be poor for UX; so passing Unit
		// previous focus out of this dialog
		// if(!info.hasFocusOnShow) focusManager.clearFocus()
		
		onDispose {
			// if(info.hasFocusOnShow) focusManager.clearFocus()
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
			Column {
				content(MaterialDialogScope(info, this))
			}
		}
	}
}



internal class MaterialDialogInfo(
	var onCloseRequest: () -> Unit
) {
	var hasFocusOnShow: Boolean = false
}

/**
 * The MaterialDialog class is used to build and display a dialog using both pre-made and
 * custom views
 */
class MaterialDialogScope internal constructor(
	private val info: MaterialDialogInfo,
	private val columnScope: ColumnScope
): ColumnScope by columnScope {
	val onCloseRequest: () -> Unit get() = info.onCloseRequest
	
	var hasFocusOnShow: Boolean
		get() = info.hasFocusOnShow
		set(value) { info.hasFocusOnShow = value }
}
