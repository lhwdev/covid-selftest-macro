package com.lhwdev.selfTestMacro.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource


@Composable
fun SimpleIconButton(
	@DrawableRes icon: Int,
	contentDescription: String?,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	IconButton(onClick = onClick, modifier = modifier) {
		Icon(
			painterResource(icon),
			contentDescription = contentDescription
		)
	}
}
