package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.LocalWindowInsets
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop


@Composable
private fun DefaultNumberPicker(value: Int, setValue: (Int) -> Unit, range: IntRange) {
	NumberPicker(
		value = value,
		setValue = setValue,
		onEditInput = { endEdit ->
			var text by remember { mutableStateOf("$value") }
			var everGainFocus by remember { mutableStateOf(false) }
			val maxLength = remember(range) { range.last.toString().length }
			val focusRequester = remember { FocusRequester() }
			
			BasicTextField(
				value = text,
				onValueChange = change@{
					if(it.length > maxLength) return@change
					text = it
				},
				textStyle = MaterialTheme.typography.h4.copy(textAlign = TextAlign.Center),
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
				keyboardActions = KeyboardActions {
					text.toIntOrNull()?.let { setValue(it) }
					endEdit()
				},
				decorationBox = { innerTextField ->
					Box(Modifier.drawBehind {
						drawLine(
							Color.Black.copy(alpha = .5f),
							Offset(0f, size.height),
							Offset(size.width, size.height)
						)
					}) { innerTextField() }
				},
				modifier = Modifier
					.focusRequester(focusRequester)
					.onFocusChanged {
						if(it.isFocused) {
							everGainFocus = true
						} else {
							if(everGainFocus) endEdit()
						}
					},
				singleLine = true
			)
			
			val insets = LocalWindowInsets.current
			LaunchedEffect(Unit) {
				focusRequester.requestFocus()
				snapshotFlow { insets.ime.isVisible }
					.drop(1)
					.collect {
						if(!it) endEdit()
					}
			}
		},
		modifier = Modifier.width(65.dp),
		range = range
	) {
		Text("$it", style = MaterialTheme.typography.h4)
	}
}


@Composable
fun FloatingMaterialDialogScope.TimePickerDialog(
	initialHour: Int,
	initialMinute: Int,
	setTime: (hour: Int, minute: Int) -> Unit,
	cancel: () -> Unit
) {
	var hour by remember { mutableStateOf(initialHour) }
	var minute by remember { mutableStateOf(initialMinute) }
	
	Title(center = true) { Text("시간 설정") }
	
	Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterHorizontally)) {
		DefaultNumberPicker(
			value = hour,
			setValue = { hour = it },
			range = 0..23 // HOUR_OF_DAY is in 0..23; 0 means 12 AM (midnight), 12 means 12 PM (noon)
		)
		Text("시", style = MaterialTheme.typography.h5)
		
		Spacer(Modifier.width(30.dp))
		
		DefaultNumberPicker(
			value = minute,
			setValue = { minute = it },
			range = 0..59
		)
		Text("분", style = MaterialTheme.typography.h5)
	}
	
	Buttons {
		PositiveButton(onClick = { setTime(hour, minute) })
		NegativeButton(onClick = cancel)
	}
}
