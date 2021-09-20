package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


@Composable
fun TimePickerDialog(initialHour: Int, initialMinute: Int) {
	var hour by remember { mutableStateOf(initialHour) }
	var minute by remember { mutableStateOf(initialMinute) }
	
	ProvideTextStyle(MaterialTheme.typography.h4) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			NumberPicker(
				value = hour,
				setValue = { hour = it },
				modifier = Modifier.weight(1f),
				range = 0..23 // HOUR_OF_DAY is in 0..23; 0 means 12 AM (midnight), 12 means 12 PM (noon)
			) {
				Text(if(it == 0) "24" else "$it")
			}
			Text("시")
			
			Spacer(Modifier.weight(.3f))
			
			NumberPicker(
				value = minute,
				setValue = { minute = it },
				modifier = Modifier.weight(1f),
				range = 0..59
			) {
				Text("$it")
			}
			Text("분")
		}
	}
}
