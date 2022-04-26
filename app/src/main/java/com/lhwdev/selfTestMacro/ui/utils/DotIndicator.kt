package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


fun Modifier.dotAlarmIndicator(color: Color): Modifier = drawWithContent {
	drawContent()
	
	val radius = 3.dp.toPx()
	drawCircle(color = color, radius = radius, center = Offset(size.width - radius, size.height - radius))
}
