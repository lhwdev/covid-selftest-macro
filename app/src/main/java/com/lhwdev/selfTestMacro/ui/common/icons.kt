package com.lhwdev.selfTestMacro.ui.common

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.lhwdev.selfTestMacro.R


@Composable
fun BackButton(onClick: () -> Unit) {
	IconButton(onClick = onClick) {
		Icon(
			painterResource(R.drawable.ic_arrow_left_24),
			contentDescription = "뒤로 가기"
		)
	}
}
