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


private val sDefaultDialogRouteFactory: ContentRouteFactory<Any?> =
	{ content, removeRoute -> DialogRoute(content = content, onRouteRemoved = { removeRoute(null) }) }


suspend fun <T> Navigator.showDialog(
	modifier: Modifier = Modifier,
	properties: DialogProperties = FloatingDialogProperties,
	maxHeight: Dp = FloatingDialogMaxHeight,
	routeFactory: ContentRouteFactory<T> = sDefaultDialogRouteFactory as ContentRouteFactory<T>,
	content: @Composable FloatingMaterialDialogScope.(dismiss: (T) -> Unit) -> Unit
): T? = showRouteFactory { removeRoute ->
	routeFactory({
		MaterialDialog(
			onCloseRequest = { removeRoute(null) },
			modifier = modifier,
			properties = properties,
			maxHeight = maxHeight
		) { content(removeRoute) }
	}, removeRoute)
}

suspend fun Navigator.showDialogUnit(
	modifier: Modifier = Modifier,
	properties: DialogProperties = FloatingDialogProperties,
	maxHeight: Dp = FloatingDialogMaxHeight,
	routeFactory: ContentRouteFactory<Nothing?> = sDefaultDialogRouteFactory as ContentRouteFactory<Nothing?>,
	content: @Composable (FloatingMaterialDialogScope.(dismiss: () -> Unit) -> Unit)
) {
	@Suppress("RemoveExplicitTypeArguments")
	showDialog<Nothing?>(
		modifier = modifier,
		properties = properties,
		maxHeight = maxHeight,
		routeFactory = routeFactory
	) { content { it(null) } }
}

fun Navigator.showDialogAsync(
	modifier: Modifier = Modifier,
	properties: DialogProperties = FloatingDialogProperties,
	maxHeight: Dp = FloatingDialogMaxHeight,
	routeFactory: ContentRouteFactory<Nothing?> = sDefaultDialogRouteFactory as ContentRouteFactory<Nothing?>,
	content: @Composable (FloatingMaterialDialogScope.(dismiss: () -> Unit) -> Unit)
) {
	showRouteFactoryAsync { removeRoute ->
		val removeRouteUnit = { removeRoute(null) }
		routeFactory({
			MaterialDialog(
				onCloseRequest = removeRouteUnit,
				modifier = modifier,
				properties = properties,
				maxHeight = maxHeight
			) { content(removeRouteUnit) }
		}, removeRoute)
	}
}

private val sFullscreenDialogRoute: ContentRouteFactory<Any?> =
	{ content, removeRoute -> FullDialogRoute(content = content, onRouteRemoved = { removeRoute(null) }) }

suspend fun <T> Navigator.showFullDialog(
	properties: DialogProperties = DialogProperties(),
	routeFactory: ContentRouteFactory<T?> = sFullscreenDialogRoute as ContentRouteFactory<T?>,
	content: @Composable (FullMaterialDialogScope.(dismiss: (T) -> Unit) -> Unit)
): T? = showRouteFactory { remoteRoute ->
	routeFactory({
		FullMaterialDialog(
			onCloseRequest = { remoteRoute(null) },
			properties = properties
		) { content(remoteRoute) }
	}, remoteRoute)
}

fun Navigator.showFullDialogAsync(
	properties: DialogProperties = DialogProperties(),
	routeFactory: ContentRouteFactory<Nothing?> = sFullscreenDialogRoute as ContentRouteFactory<Nothing?>,
	content: @Composable (FullMaterialDialogScope.(dismiss: () -> Unit) -> Unit)
) {
	showRouteFactoryAsync { remoteRoute ->
		val removeRouteUnit = { remoteRoute(null) }
		routeFactory({
			FullMaterialDialog(
				onCloseRequest = removeRouteUnit,
				properties = properties
			) { content(removeRouteUnit) }
		}, remoteRoute)
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
	itemToContent: @Composable (T) -> Unit,
	properties: DialogProperties = FloatingDialogProperties
): T? = showDialog(properties = properties) { removeRoute ->
	Title(text = title)
	
	ListContent {
		for(item in items) ListItem(
			modifier = Modifier.clickable { removeRoute(item) }
		) { itemToContent(item) }
	}
	
	Buttons {
		NegativeButton(onClick = requestClose) { Text("취소") }
	}
}
