@file:Suppress("UNCHECKED_CAST", "NAME_SHADOWING")

package com.vanpra.composematerialdialogs

import androidx.compose.foundation.clickable
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import com.lhwdev.selfTestMacro.navigation.*


suspend fun <T> Navigator.showDialog(
	route: Route = Route.Empty,
	modifier: Modifier = Modifier,
	properties: DialogProperties = FloatingDialogProperties,
	maxHeight: Dp = FloatingDialogMaxHeight,
	content: @Composable FloatingMaterialDialogScope.(dismiss: (T) -> Unit) -> Unit
): T? = showRouteFactory { removeRoute ->
	DialogRoute(OnRouteRemoved to { removeRoute(null) }) {
		MaterialDialog(
			onCloseRequest = { removeRoute(null) },
			modifier = modifier,
			properties = properties,
			maxHeight = maxHeight
		) { content(removeRoute) }
	}.merge(route)
}

suspend fun Navigator.showDialogUnit(
	route: Route = Route.Empty,
	modifier: Modifier = Modifier,
	properties: DialogProperties = FloatingDialogProperties,
	maxHeight: Dp = FloatingDialogMaxHeight,
	content: @Composable (FloatingMaterialDialogScope.(dismiss: () -> Unit) -> Unit)
) {
	showDialog<Nothing?>(
		modifier = modifier,
		properties = properties,
		maxHeight = maxHeight,
		route = route
	) { content { it(null) } }
}

fun Navigator.showDialogAsync(
	route: Route = Route.Empty,
	modifier: Modifier = Modifier,
	properties: DialogProperties = FloatingDialogProperties,
	maxHeight: Dp = FloatingDialogMaxHeight,
	content: @Composable (FloatingMaterialDialogScope.(dismiss: () -> Unit) -> Unit)
) {
	showRouteFactoryAsync { removeRoute ->
		val removeRouteUnit = { removeRoute(null) }
		DialogRoute(OnRouteRemoved to { removeRoute(null) }) {
			MaterialDialog(
				onCloseRequest = removeRouteUnit,
				modifier = modifier,
				properties = properties,
				maxHeight = maxHeight
			) { content(removeRouteUnit) }
		}.merge(route)
	}
}


suspend fun <T> Navigator.showFullDialog(
	route: Route = Route.Empty,
	properties: DialogProperties = DialogProperties(),
	content: @Composable (FullMaterialDialogScope.(dismiss: (T) -> Unit) -> Unit)
): T? = showRouteFactory { removeRoute ->
	FullDialogRoute(OnRouteRemoved to { removeRoute(null) }) {
		FullMaterialDialog(
			onDismissRequest = { removeRoute(null) },
			properties = properties
		) { content(removeRoute) }
	}
}

fun Navigator.showFullDialogAsync(
	route: Route = Route.Empty,
	properties: DialogProperties = DialogProperties(),
	content: @Composable (FullMaterialDialogScope.(dismiss: () -> Unit) -> Unit)
) {
	showRouteFactoryAsync { removeRoute ->
		val removeRouteUnit = { removeRoute(null) }
		FullDialogRoute(OnRouteRemoved to { removeRoute(null) }) {
			FullMaterialDialog(
				onDismissRequest = removeRouteUnit,
				properties = properties
			) { content(removeRouteUnit) }
		}
	}
}

suspend fun Navigator.promptYesNoDialog(
	title: @Composable () -> Unit,
	content: @Composable (FloatingMaterialDialogScope.() -> Unit)? = null,
	yesButton: @Composable () -> Unit = { Text("확인") },
	noButton: @Composable () -> Unit = { Text("취소") },
	properties: DialogProperties = FloatingDialogProperties
): Boolean? = showDialog(properties = properties) { removeRoute ->
	Title(text = title)
	
	if(content != null) content()
	
	Buttons {
		PositiveButton(onClick = { removeRoute(true) }, content = yesButton)
		NegativeButton(onClick = requestClose, content = noButton)
	}
}

suspend fun <T> Navigator.promptSelectDialog(
	title: @Composable () -> Unit,
	items: List<T>,
	itemToText: @Composable (T) -> Unit,
	properties: DialogProperties = FloatingDialogProperties
): T? = showDialog(properties = properties) { removeRoute ->
	Title(text = title)
	
	ListContent {
		for(item in items) ListItem(
			modifier = Modifier.clickable { removeRoute(item) }
		) { itemToText(item) }
	}
	
	Buttons {
		NegativeButton(onClick = requestClose)
	}
}
