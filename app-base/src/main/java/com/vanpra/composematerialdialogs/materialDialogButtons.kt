package com.vanpra.composematerialdialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.ui.primaryActive

internal enum class MaterialDialogButtonTypes {
	Text,
	Positive,
	Negative,
	Accessibility
}


private fun List<Pair<MaterialDialogButtonTypes, Placeable>>.buttons(type: MaterialDialogButtonTypes) =
	this.filter { it.first == type }.map { it.second }


/**
 *  Adds buttons to the bottom of the dialog
 * @param content the buttons which should be displayed in the dialog.
 * See [MaterialDialogButtonsScope] for more information about the content
 */
@Composable
fun MaterialDialogScope.Buttons(content: @Composable FloatingMaterialDialogButtonsScope.() -> Unit) {
	ButtonsBase {
		FloatingMaterialDialogButtonsScope(requestClose).content()
	}
}

@Composable
fun MaterialDialogButtons(content: @Composable MaterialDialogButtonsScope.() -> Unit) {
	ButtonsBase {
		MaterialDialogButtonsScopeImpl.content()
	}
}

@Composable
private fun ButtonsBase(content: @Composable () -> Unit) {
	val interButtonPadding = with(LocalDensity.current) { 12.dp.toPx().toInt() }
	val defaultBoxHeight = with(LocalDensity.current) { 36.dp.toPx().toInt() }
	val accessibilityPadding = with(LocalDensity.current) { 12.dp.toPx().toInt() }
	
	Box(
		Modifier
			.fillMaxWidth()
			.padding(top = 8.dp, bottom = 12.dp, end = 8.dp)
			.layoutId("buttons")
	) {
		Layout(
			content = { content() },
			modifier = Modifier
		)
		{ measurables, constraints ->
			val placeables = measurables.map {
				(it.layoutId as MaterialDialogButtonTypes) to it.measure(constraints)
			}
			val totalWidth = placeables.sumOf { it.second.width }
			val column = totalWidth > 0.8 * constraints.maxWidth
			
			val height =
				if(column) {
					val buttonHeight = placeables.sumOf { it.second.height }
					val heightPadding = (placeables.size - 1) * interButtonPadding
					buttonHeight + heightPadding
				} else {
					defaultBoxHeight
				}
			
			layout(constraints.maxWidth, height) {
				var currX = constraints.maxWidth
				var currY = 0
				
				val posButtons = placeables.buttons(MaterialDialogButtonTypes.Positive)
				val negButtons = placeables.buttons(MaterialDialogButtonTypes.Negative)
				val textButtons = placeables.buttons(MaterialDialogButtonTypes.Text)
				val accButtons = placeables.buttons(MaterialDialogButtonTypes.Accessibility)
				
				val buttonInOrder = posButtons + textButtons + negButtons
				buttonInOrder.forEach { button ->
					if(column) {
						button.place(currX - button.width, currY)
						currY += button.height + interButtonPadding
					} else {
						currX -= button.width
						button.place(currX, 0)
					}
				}
				
				if(accButtons.isNotEmpty()) {
					/* There can only be one accessibility button so take first */
					val button = accButtons[0]
					button.place(accessibilityPadding, height - button.height)
				}
			}
		}
	}
}


@LayoutScopeMarker
interface MaterialDialogButtonsScope

class FloatingMaterialDialogButtonsScope(val requestClose: () -> Unit) : MaterialDialogButtonsScope

private object MaterialDialogButtonsScopeImpl : MaterialDialogButtonsScope


@Composable
private fun ButtonBase(
	onClick: () -> Unit,
	layoutId: MaterialDialogButtonTypes,
	content: @Composable () -> Unit
) {
	TextButton(
		onClick = onClick,
		modifier = Modifier.layoutId(layoutId),
		colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.primaryActive)
	) { content() }
}


/**
 * Adds a button which is always enabled to the bottom of the dialog. This should
 * only be used for neutral actions.
 *
 * @param onClick a callback which is called when the button is pressed
 * @param button content shown in the button
 */
@Suppress("unused")
@Composable
fun MaterialDialogButtonsScope.Button(
	onClick: () -> Unit,
	button: @Composable () -> Unit,
) {
	ButtonBase(
		onClick = onClick,
		layoutId = MaterialDialogButtonTypes.Text
	) {
		// ProvideTextStyle(TextStyle())
		button()
	}
}

/**
 * Adds a positive button to the dialog
 *
 * @param onClick a callback which is called when the button is pressed
 * @param content the content shown in the button
 */
@Suppress("unused")
@Composable
fun MaterialDialogButtonsScope.PositiveButton(
	onClick: () -> Unit,
	content: @Composable () -> Unit = { Text("확인") }
) {
	ButtonBase(
		onClick = onClick,
		layoutId = MaterialDialogButtonTypes.Positive
	) {
		content()
	}
}

/**
 * Adds a negative button to the dialog
 *
 * @param onClick a callback which is called when the button is pressed
 * @param content the content shown in the button
 * even if autoDismissing is disabled
 */
@Suppress("unused")
@Composable
fun MaterialDialogButtonsScope.NegativeButton(
	onClick: () -> Unit,
	content: @Composable () -> Unit = { Text("취소") }
) {
	ButtonBase(
		onClick = onClick,
		layoutId = MaterialDialogButtonTypes.Negative
	) {
		content()
	}
}

/**
 * Adds an accessibility button to the bottom left of the dialog
 *
 * @param onClick a callback which is called when the button is pressed
 * @param icon the icon to be shown on the button
 */
@Suppress("unused")
@Composable
fun MaterialDialogButtonsScope.AccessibilityButton(
	onClick: () -> Unit,
	icon: @Composable () -> Unit
) {
	Box(
		Modifier
			.size(48.dp)
			.layoutId(MaterialDialogButtonTypes.Accessibility)
			.clickable(onClick = onClick),
		contentAlignment = Alignment.Center
	) {
		Box(Modifier.size(24.dp)) {
			icon()
		}
	}
}
