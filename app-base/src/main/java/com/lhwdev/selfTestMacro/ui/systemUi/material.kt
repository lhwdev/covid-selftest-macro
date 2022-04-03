package com.lhwdev.selfTestMacro.ui.systemUi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TopAppBar(
	title: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	statusBarScrim: @Composable () -> Unit,
	navigationIcon: @Composable (() -> Unit)? = null,
	actions: @Composable RowScope.() -> Unit = {},
	backgroundColor: Color = MaterialTheme.colors.primarySurface,
	contentColor: Color = contentColorFor(backgroundColor),
	elevation: Dp = AppBarDefaults.TopAppBarElevation,
) {
	Surface(
		color = backgroundColor,
		elevation = elevation
	) {
		Column {
			statusBarScrim()
			
			androidx.compose.material.TopAppBar(
				title = title,
				modifier = modifier,
				navigationIcon = navigationIcon,
				actions = actions,
				backgroundColor = Color.Transparent,
				contentColor = contentColor,
				elevation = 0.dp
			)
		}
	}
}
