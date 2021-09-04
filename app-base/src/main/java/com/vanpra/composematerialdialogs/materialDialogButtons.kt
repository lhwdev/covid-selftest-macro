package com.vanpra.composematerialdialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

internal enum class MaterialDialogButtonTypes {
	Text,
	Positive,
	Negative,
	Accessibility
}

/**
 *  Adds buttons to the bottom of the dialog
 * @param content the buttons which should be displayed in the dialog.
 * See [MaterialDialogButtonsScope] for more information about the content
 */
@Composable
fun MaterialDialogScope.Buttons(content: @Composable MaterialDialogButtonsScope.() -> Unit) {
	val interButtonPadding = with(LocalDensity.current) { 12.dp.toPx().toInt() }
	val defaultBoxHeight = with(LocalDensity.current) { 36.dp.toPx().toInt() }
	val accessibilityPadding = with(LocalDensity.current) { 12.dp.toPx().toInt() }
	
	Box(
		Modifier
			.fillMaxWidth()
			.padding(top = 8.dp, bottom = 8.dp, end = 8.dp)
			.layoutId("buttons")
	) {
		Layout(
			content = { content(MaterialDialogButtonsScope(this@Buttons)) },
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

/**
 * A class used to build a buttons layout for a MaterialDialog. This should be used in conjunction
 * with the [MaterialDialogScope.Buttons] function
 */
class MaterialDialogButtonsScope(private val scope: MaterialDialogScope) {
	/**
	 * Adds a button which is always enabled to the bottom of the dialog. This should
	 * only be used for neutral actions.
	 *
	 * @param onClick a callback which is called when the button is pressed
	 * @param button content shown in the button
	 */
	@Composable
	fun Button(
		onClick: () -> Unit = {},
		button: @Composable () -> Unit,
	) {
		TextButton(
			onClick = onClick,
			modifier = Modifier.layoutId(MaterialDialogButtonTypes.Text),
		) {
			// ProvideTextStyle(TextStyle())
			// TODO: uppercase
			button()
		}
	}
	
	/**
	 * Adds a positive button to the dialog
	 *
	 * @param onClick a callback which is called when the button is pressed
	 * @param content the content shown in the button
	 */
	@Composable
	fun PositiveButton(
		onClick: () -> Unit = scope.onCloseRequest,
		content: @Composable () -> Unit
	) {
		TextButton(
			onClick = onClick,
			modifier = Modifier.layoutId(MaterialDialogButtonTypes.Positive),
			enabled = true
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
	@Composable
	fun NegativeButton(
		onClick: () -> Unit = scope.onCloseRequest,
		content: @Composable () -> Unit
	) {
		TextButton(
			onClick = onClick,
			modifier = Modifier.layoutId(MaterialDialogButtonTypes.Negative)
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
	@Composable
	fun AccessibilityButton(
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
}
