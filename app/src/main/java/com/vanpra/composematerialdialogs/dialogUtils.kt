package com.vanpra.composematerialdialogs

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties


@Composable
internal fun ThemedDialog(
	onCloseRequest: () -> Unit,
	properties: DialogProperties,
	children: @Composable () -> Unit,
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
