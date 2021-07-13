@file:Suppress("NOTHING_TO_INLINE")

package com.vanpra.composematerialdialogs

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lhwdev.selfTestMacro.Navigator
import com.lhwdev.selfTestMacro.Route
import com.lhwdev.selfTestMacro.showRoute
import com.lhwdev.selfTestMacro.showRouteAsync


suspend fun <T> Navigator.showDialog(
	properties: DialogProperties = FloatingDialogProperties,
	routeFactory: (content: @Composable () -> Unit) -> Route = { Route(it) },
	content: @Composable FloatingMaterialDialogScope.(removeRoute: (T) -> Unit) -> Unit
): T? = showRoute(routeFactory = routeFactory) { removeRoute ->
	MaterialDialog(
		onCloseRequest = { removeRoute(null) },
		properties = properties
	) { content(removeRoute) }
}

suspend inline fun Navigator.showDialogUnit(
	properties: DialogProperties = FloatingDialogProperties,
	noinline routeFactory: (content: @Composable () -> Unit) -> Route = { Route(it) },
	noinline content: @Composable (FloatingMaterialDialogScope.(removeRoute: () -> Unit) -> Unit)
) {
	showDialog<Unit>(properties, routeFactory) { content { it(Unit) } }
}

inline fun Navigator.showDialogAsync(
	properties: DialogProperties = FloatingDialogProperties,
	noinline routeFactory: (content: @Composable () -> Unit) -> Route = { Route(it) },
	noinline content: @Composable (FloatingMaterialDialogScope.(removeRoute: () -> Unit) -> Unit)
) {
	showRouteAsync(routeFactory) { removeRoute ->
		MaterialDialog(
			onCloseRequest = removeRoute,
			properties = properties
		) { content(removeRoute) }
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
