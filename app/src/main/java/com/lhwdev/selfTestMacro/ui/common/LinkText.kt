package com.lhwdev.selfTestMacro.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration


@Composable
fun LinkedText(text: String, onClick: () -> Unit) {
	Text(
		text,
		style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
		modifier = Modifier.clickable(onClick = onClick)
	)
}
