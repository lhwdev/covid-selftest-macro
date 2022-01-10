@file:Suppress("NOTHING_TO_INLINE")

package com.vanpra.composematerialdialogs

import androidx.compose.foundation.clickable
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lhwdev.selfTestMacro.navigation.*


suspend inline fun <T> Navigator.showDialog(
	modifier: Modifier = Modifier,
	properties: DialogProperties = FloatingDialogProperties,
	maxHeight: Dp = FloatingDialogMaxHeight,
	noinline routeFactory: (content: @Composable () -> Unit) -> Route = { DialogRoute(content = it) },
	crossinline content: @Composable FloatingMaterialDialogScope.(dismiss: (T) -> Unit) -> Unit
): T? = showRouteFactory { removeRoute ->
	routeFactory {
		MaterialDialog(
			onCloseRequest = { removeRoute(null) },
			modifier = modifier,
			properties = properties,
			maxHeight = maxHeight
		) { content(removeRoute) }
	}
}

suspend inline fun Navigator.showDialogUnit(
	modifier: Modifier = Modifier,
	properties: DialogProperties = FloatingDialogProperties,
	maxHeight: Dp = FloatingDialogMaxHeight,
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
	properties: DialogProperties = FloatingDialogProperties,
	maxHeight: Dp = FloatingDialogMaxHeight,
	routeFactory: (content: @Composable () -> Unit) -> Route = { DialogRoute(content = it) },
	noinline content: @Composable (FloatingMaterialDialogScope.(dismiss: () -> Unit) -> Unit)
) {
	showRouteFactoryAsync { removeRoute ->
		routeFactory {
			MaterialDialog(
				onCloseRequest = removeRoute,
				modifier = modifier,
				properties = properties,
				maxHeight = maxHeight
			) { content(removeRoute) }
		}
	}
}

suspend inline fun <T> Navigator.showFullDialog(
	noinline routeFactory: (content: @Composable () -> Unit) -> Route = { FullDialogRoute(content = it) },
	noinline content: @Composable (FullMaterialDialogScope.(dismiss: (T) -> Unit) -> Unit)
): T? = showRouteFactory { remoteRoute ->
	routeFactory {
		FullMaterialDialogStub(
			onCloseRequest = { remoteRoute(null) }
		) { content(remoteRoute) }
	}
}

inline fun Navigator.showFullDialogAsync(
	routeFactory: (content: @Composable () -> Unit) -> Route = { FullDialogRoute(content = it) },
	noinline content: @Composable (FullMaterialDialogScope.(dismiss: () -> Unit) -> Unit)
) {
	showRouteFactoryAsync { remoteRoute ->
		routeFactory {
			FullMaterialDialogStub(
				onCloseRequest = { remoteRoute() }
			) { content(remoteRoute) }
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
	Title { title() }
	
	if(content != null) content()
	
	Buttons {
		PositiveButton(onClick = { removeRoute(true) }, content = yesButton)
		NegativeButton(onClick = requestClose, content = noButton)
	}
}

suspend fun <T> Navigator.promptSelectDialog(
	title: @Composable () -> Unit,
	items: List<T>,
	itemToContent: @Composable (T) -> Unit,
	properties: DialogProperties = FloatingDialogProperties
): T? = showDialog(properties = properties) { removeRoute ->
	Title { title() }
	
	ListContent {
		for(item in items) ListItem(
			modifier = Modifier.clickable { removeRoute(item) }
		) { itemToContent(item) }
	}
	
	Buttons {
		NegativeButton(onClick = requestClose) { Text("취소") }
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
