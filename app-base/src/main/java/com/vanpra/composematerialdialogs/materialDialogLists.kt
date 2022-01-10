@file:Suppress("unused")

package com.vanpra.composematerialdialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val listRatio = 0.6f
val bottomPadding = Modifier.padding(bottom = 8.dp)

/**
 * Adds a selectable plain text list to the dialog
 *
 * @param content the contents to be displayed in the list
 */
@Composable
fun MaterialDialogScope.ListContent(
	modifier: Modifier = Modifier,
	verticalArrangement: Arrangement.Vertical = Arrangement.Top,
	horizontalAlignment: Alignment.Horizontal = Alignment.Start,
	content: @Composable ColumnScope.() -> Unit
) {
	BoxWithConstraints {
		CompositionLocalProvider(
			LocalContentColor provides MaterialTheme.colors.onSurface
		) {
			ProvideTextStyle(MaterialTheme.typography.body1) {
				Column(
					modifier
						.heightIn(max = maxHeight * listRatio)
						.padding(bottom = 8.dp)
						.wrapContentWidth(Alignment.Start)
						.verticalScroll(rememberScrollState()),
					verticalArrangement = verticalArrangement,
					horizontalAlignment = horizontalAlignment
				) {
					content()
				}
			}
		}
	}
}


@Composable
fun SingleChoiceItem(
	selected: Boolean,
	enabled: Boolean = true,
	onSelect: () -> Unit,
	item: @Composable () -> Unit
) {
	Row(
		Modifier
			.fillMaxWidth()
			.height(48.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		RadioButton(
			selected = selected,
			onClick = {
				if(enabled) onSelect()
			},
			enabled = enabled
		)
		
		Spacer(
			modifier = Modifier
				.fillMaxHeight()
				.width(32.dp)
		)
		
		CompositionLocalProvider(
			LocalContentColor provides if(enabled) {
				MaterialTheme.colors.onSurface
			} else {
				MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
			}
		) {
			ProvideTextStyle(MaterialTheme.typography.body1) {
				item()
			}
		}
	}
}
