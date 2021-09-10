@file:Suppress("NOTHING_TO_INLINE")

package com.vanpra.composematerialdialogs

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lhwdev.selfTestMacro.navigation.*


suspend fun <T> Navigator.showDialog(
	modifier: Modifier = Modifier,
	properties: DialogProperties = com.vanpra.composematerialdialogs.FloatingDialogProperties,
	maxHeight: Dp = com.vanpra.composematerialdialogs.FloatingDialogMaxHeight,
	routeFactory: (content: @Composable () -> Unit) -> Route = { DialogRoute(content = it) },
	content: @Composable FloatingMaterialDialogScope.(dismiss: (T) -> Unit) -> Unit
): T? = showRoute(routeFactory = routeFactory) { removeRoute ->
	MaterialDialog(
		onCloseRequest = { removeRoute(null) },
		modifier = modifier,
		properties = properties,
		maxHeight = maxHeight
	) { content(removeRoute) }
}

suspend inline fun Navigator.showDialogUnit(
	modifier: Modifier = Modifier,
	properties: DialogProperties = com.vanpra.composematerialdialogs.FloatingDialogProperties,
	maxHeight: Dp = com.vanpra.composematerialdialogs.FloatingDialogMaxHeight,
	noinline routeFactory: (content: @Composable () -> Unit) -> Route = { DialogRoute(content = it) },
	noinline content: @Composable (FloatingMaterialDialogScope.(dismiss: () -> Unit) -> Unit)
) {
	showDialog<Unit>(
		modifier = modifier,
		properties = properties,
		maxHeight = maxHeight,
		routeFactory = routeFactory
	) { content { it(Unit) } }
}

inline fun Navigator.showDialogAsync(
	modifier: Modifier = Modifier,
	properties: DialogProperties = com.vanpra.composematerialdialogs.FloatingDialogProperties,
	maxHeight: Dp = com.vanpra.composematerialdialogs.FloatingDialogMaxHeight,
	noinline routeFactory: (content: @Composable () -> Unit) -> Route = { DialogRoute(content = it) },
	noinline content: @Composable (FloatingMaterialDialogScope.(dismiss: () -> Unit) -> Unit)
) {
	showRouteAsync(routeFactory) { removeRoute ->
		MaterialDialog(
			onCloseRequest = removeRoute,
			modifier = modifier,
			properties = properties,
			maxHeight = maxHeight
		) { content(removeRoute) }
	}
}


suspend fun Navigator.promptYesNoDialog(
	title: @Composable () -> Unit,
	content: @Composable (FloatingMaterialDialogScope.() -> Unit)? = null,
	yesButton: @Composable () -> Unit = { Text("확인") },
	noButton: @Composable () -> Unit = { Text("취소") },
	properties: DialogProperties = com.vanpra.composematerialdialogs.FloatingDialogProperties
): Boolean? = showDialog(properties = properties) { removeRoute ->
	Title { title() }
	
	if(content != null) content()
	
	Buttons {
		PositiveButton(onClick = { removeRoute(true) }, content = yesButton)
		NegativeButton(content = noButton)
	}
}


@Composable
internal fun ThemedDialog(
	onCloseRequest: () -> Unit,
	properties: DialogProperties,
	children: @Composable () -> Unit
) {
	val colors = MaterialTheme.colors
	val typography = MaterialTheme.typography
	Dialog(onDismissRequest = onCloseRequest, properties = properties) {
		MaterialTheme(colors = colors, typography = typography) {
			children()
		}
	}
}

internal fun List<Pair<MaterialDialogButtonTypes, Placeable>>.buttons(type: MaterialDialogButtonTypes) =
	this.filter { it.first == type }.map { it.second }
