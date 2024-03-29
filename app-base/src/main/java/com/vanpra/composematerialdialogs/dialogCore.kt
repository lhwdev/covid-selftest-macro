@file:Suppress("unused")

package com.vanpra.composematerialdialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.android.awaitFrame


@Composable
private fun FloatingMaterialDialogScope.BaseTitle(
	modifier: Modifier,
	center: Boolean = false,
	content: @Composable () -> Unit
) {
	ProvideTextStyle(MaterialTheme.typography.h6) {
		Box(
			modifier
				.align(if(center) Alignment.CenterHorizontally else Alignment.Start)
				.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)
		) {
			content()
		}
	}
}


/**
 * Adds a title with the given text to the dialog
 * @param text title text
 */
@Composable
fun FloatingMaterialDialogScope.Title(
	modifier: Modifier = Modifier,
	center: Boolean = false,
	text: @Composable () -> Unit,
) {
	BaseTitle(modifier, center) { text() }
}

/**
 *  Adds a title with the given text and icon to the dialog
 * @param icon optional icon displayed at the start of the title
 * @param text title text from a string literal
 */
@Composable
fun FloatingMaterialDialogScope.IconTitle(
	icon: @Composable () -> Unit,
	text: @Composable () -> Unit
) {
	BaseTitle(Modifier) {
		icon()
		Spacer(Modifier.width(14.dp))
		ProvideTextStyle(MaterialTheme.typography.h6) { text() }
	}
}

/**
 * Create a content in the dialog with appropriate padding
 * @param content the content of the view
 */
@Composable
fun FloatingMaterialDialogScope.Content(
	modifier: Modifier = Modifier,
	verticalArrangement: Arrangement.Vertical = Arrangement.Top,
	horizontalAlignment: Alignment.Horizontal = Alignment.Start,
	content: @Composable ColumnScope.() -> Unit
) {
	CompositionLocalProvider(
		LocalContentColor provides MaterialTheme.colors.onSurface
	) {
		ProvideTextStyle(MaterialTheme.typography.body1) {
			Column(
				modifier = modifier.padding(bottom = 28.dp, start = 24.dp, end = 24.dp),
				verticalArrangement = verticalArrangement,
				horizontalAlignment = horizontalAlignment
			) {
				content()
			}
		}
	}
}


@Composable
fun MaterialDialogScope.Input(
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(
		start = 24.dp,
		end = 24.dp,
		top = 8.dp,
		bottom = 8.dp
	),
	focusOnShow: Boolean = false, // should be invariant in one composition
	input: @Composable () -> Unit
) {
	Box(modifier.padding(contentPadding)) {
		if(focusOnShow) {
			hasFocusOnShow = true
			val focusRequester = remember { FocusRequester() }
			
			LaunchedEffect(Unit) {
				awaitFrame()
				focusRequester.requestFocus()
			}
			
			Box(
				Modifier
					.focusRequester(focusRequester)
					.fillMaxWidth()
			) { input() }
		} else {
			input()
		}
	}
}

