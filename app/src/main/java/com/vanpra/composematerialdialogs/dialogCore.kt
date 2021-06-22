@file:Suppress("unused")

package com.vanpra.composematerialdialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

/**
 * Adds a title with the given text to the dialog
 * @param center text is aligned to center when true
 * @param text title text
 */
@Composable
fun MaterialDialogScope.Title(
	center: Boolean = false,
	text: @Composable () -> Unit,
) {
	CompositionLocalProvider(
		LocalContentColor provides MaterialTheme.colors.onSurface
	) {
		ProvideTextStyle(MaterialTheme.typography.h6) {
			Box(
				Modifier
					.fillMaxWidth()
					.padding(start = 24.dp, end = 24.dp)
					.height(64.dp)
					.wrapContentHeight(Alignment.CenterVertically)
					.wrapContentWidth(
						if(center) {
							Alignment.CenterHorizontally
						} else {
							Alignment.Start
						}
					)
			) { text() }
		}
	}
}

/**
 *  Adds a title with the given text and icon to the dialog
 * @param text title text from a string literal
 * @param icon optional icon displayed at the start of the title
 */
@Composable
fun MaterialDialogScope.IconTitle(
	text: @Composable () -> Unit,
	icon: @Composable () -> Unit,
) {
	Row(
		modifier = Modifier
			.padding(start = 24.dp, end = 24.dp)
			.height(64.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		icon()
		Spacer(Modifier.width(14.dp))
		CompositionLocalProvider(
			LocalContentColor provides MaterialTheme.colors.onSurface
		) {
			ProvideTextStyle(MaterialTheme.typography.h6) { text() }
		}
	}
}

/**
 * Create an content in the dialog with appropriate padding
 * @param content the content of the view
 */
@Composable
fun MaterialDialogScope.Content(content: @Composable () -> Unit) {
	CompositionLocalProvider(
		LocalContentColor provides MaterialTheme.colors.onSurface
	) {
		ProvideTextStyle(MaterialTheme.typography.body1) {
			Box(modifier = Modifier.padding(bottom = 28.dp, start = 24.dp, end = 24.dp)) {
				content()
			}
		}
	}
}


/**
 * Adds an input field with the given parameters to the dialog.
 */
@Composable
fun MaterialDialogScope.Input(
	focusOnShow: Boolean = false, // should be invariant in one composition
	input: @Composable () -> Unit
) {
	Box(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)) {
		if(focusOnShow) {
			this@Input.hasFocusOnShow = true
			val focusRequester = FocusRequester()
			
			DisposableEffect(focusRequester) {
				focusRequester.requestFocus()
				onDispose { /*focusRequester.freeFocus()*/ }
			}
			
			Box(Modifier.focusRequester(focusRequester).fillMaxWidth()) { input() } // TODO
		} else {
			input()
		}
	}
}

/*
/**
 */
 * Adds an input field with the given parameters to the dialog.
 * @param label string to be shown in the input field before selection eg. Username
 * @param hint hint to be shown in the input field when it is selected but empty eg. Joe
 * @param prefill string to be input into the text field by default
 * @param waitForPositiveButton if true the [onInput] callback will only be called when the
 * positive button is pressed, otherwise it will be called when the input value is changed
 * @param visualTransformation a visual transformation of the content of the text field
 * @param keyboardOptions software keyboard options which can be used to customize parts
 * of the keyboard
 * @param errorMessage a message to be shown to the user when the input is not valid
 * @param focusRequester a [FocusRequester] which can be used to control the focus state of the
 * text field
 * @param focusOnShow if set to true this will auto focus the text field when the input
 * field is shown
 * @param isTextValid a function which is called to check if the user input is valid
 * @param onInput a function which is called with the user input. The timing of this call is
 * dictated by [waitForPositiveButton]
 *//*
@ExperimentalComposeUiApi
@Composable
fun MaterialDialogScope.Input(
	label: String,
	hint: String = "",
	prefill: String = "",
	waitForPositiveButton: Boolean = true,
	visualTransformation: VisualTransformation = VisualTransformation.None,
	keyboardOptions: KeyboardOptions = KeyboardOptions(),
	keyboardActions: KeyboardActions = KeyboardActions(),
	errorMessage: String = "",
	focusRequester: FocusRequester = FocusRequester.Default,
	focusOnShow: Boolean = false,
	isTextValid: (String) -> Boolean = { true },
	onInput: (String) -> Unit = {},
) {
	var text by remember { mutableStateOf(prefill) }
	val valid = remember(text) { isTextValid(text) }
	val focusManager = LocalFocusManager.current
	
	val positiveEnabledIndex = addPositiveButtonEnabled(valid = valid) {
		focusManager.clearFocus()
	}
	
	DisposableEffect(valid) {
		setPositiveEnabled(positiveEnabledIndex, valid)
		onDispose { }
	}
	
	if(waitForPositiveButton) {
		DialogCallback { onInput(text) }
	} else {
		DisposableEffect(text) {
			onInput(text)
			onDispose { }
		}
	}
	
	Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)) {
		TextField(
			value = text,
			onValueChange = {
				text = it
				if(!waitForPositiveButton) {
					onInput(text)
				}
			},
			label = { Text(label, color = MaterialTheme.colors.onBackground.copy(0.8f)) },
			modifier = Modifier
				.focusRequester(focusRequester)
				.fillMaxWidth(),
			placeholder = { Text(hint, color = MaterialTheme.colors.onBackground.copy(0.5f)) },
			isError = !valid,
			visualTransformation = visualTransformation,
			keyboardOptions = keyboardOptions,
			keyboardActions = keyboardActions,
			textStyle = TextStyle(MaterialTheme.colors.onBackground, fontSize = 16.sp)
		)
		
		if(!valid) {
			Text(
				errorMessage,
				fontSize = 14.sp,
				color = MaterialTheme.colors.error,
				modifier = Modifier.align(Alignment.End)
			)
		}
	}
	
	if(focusOnShow) {
		DisposableEffect(Unit) {
			focusRequester.requestFocus()
			onDispose { }
		}
	}
}*/
