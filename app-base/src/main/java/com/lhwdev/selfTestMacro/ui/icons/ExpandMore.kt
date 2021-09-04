package com.lhwdev.selfTestMacro.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val sExpendMore: ImageVector by lazy {
	ImageVector.Builder(
		name = "ExpandMore-24px", defaultWidth = 24.0.dp,
		defaultHeight = 24.0.dp, viewportWidth = 24.0f, viewportHeight = 24.0f
	).apply {
		path(
			fill = SolidColor(Color(0xFF000000)), stroke = null, strokeLineWidth = 0.0f,
			strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
			pathFillType = NonZero
		) {
			moveTo(16.59f, 8.59f)
			lineTo(12.0f, 13.17f)
			lineTo(7.41f, 8.59f)
			lineTo(6.0f, 10.0f)
			lineToRelative(6.0f, 6.0f)
			lineToRelative(6.0f, -6.0f)
			close()
		}
	}.build()
}

@Suppress("unused")
val FilledIcons.ExpandMore: ImageVector
	get() = sExpendMore
