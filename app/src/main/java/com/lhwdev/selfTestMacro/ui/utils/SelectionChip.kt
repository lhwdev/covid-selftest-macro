package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhwdev.selfTestMacro.ui.primaryContainer


// from M3 but using M2 library?!
// https://m3.material.io/components/chips
@Composable
fun SelectionChip(
	selected: Boolean,
	setSelected: (Boolean) -> Unit,
	trailingIconSelected: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	content: @Composable RowScope.() -> Unit
) {
	val selectedAnimation = updateTransition(selected, label = "selected")
	
	Surface(
		onClick = { setSelected(!selected) },
		shape = RoundedCornerShape(8.dp),
		color = with(MaterialTheme.colors) { if(selected) primaryContainer else surface },
		border = if(selected) null else BorderStroke(width = 1.dp, color = MaterialTheme.colors.onSurface),
		contentColor = MaterialTheme.colors.onSurface,
		modifier = modifier.height(32.dp)
	) {
		Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
			AnimatedVisibility(selected) {
				CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.onSurface) {
					trailingIconSelected()
				}
			}
			
			Spacer(Modifier.width(8.dp))
			
			ProvideTextStyle(
				TextStyle(fontSize = 14.sp, fontWeight = if(selected) FontWeight.Bold else FontWeight.Normal)
			) { content() }
			
			Spacer(Modifier.width(8.dp))
		}
	}
}
