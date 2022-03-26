package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vanpra.composematerialdialogs.*


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
		NumberPicker(
			value = hour,
			setValue = { hour = it },
			modifier = Modifier.width(65.dp),
			range = 0..23 // HOUR_OF_DAY is in 0..23; 0 means 12 AM (midnight), 12 means 12 PM (noon)
		) {
			Text("$it", style = MaterialTheme.typography.h4)
		}
		Text("시", style = MaterialTheme.typography.h5)
		
		Spacer(Modifier.width(30.dp))
		
		NumberPicker(
			value = minute,
			setValue = { minute = it },
			modifier = Modifier.width(65.dp),
			range = 0..59
		) {
			Text("$it", style = MaterialTheme.typography.h4)
		}
		Text("분", style = MaterialTheme.typography.h5)
	}
	
	Buttons {
		PositiveButton(onClick = { setTime(hour, minute) })
		NegativeButton(onClick = cancel)
	}
}
