package com.lhwdev.selfTestMacro.ui.common

import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.lhwdev.selfTestMacro.database.DbTestTarget
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.pages.main.iconFor


@Composable
fun TestTargetListItem(
	target: DbTestTarget
) {
	val pref = LocalPreference.current
	ListItem(
		icon = { Icon(painterResource(pref.db.iconFor(target)), contentDescription = null) },
		text = {
			val text = with(pref.db) {
				when(target) {
					is DbTestTarget.Group -> "${target.name} (${target.userIds.size})"
					is DbTestTarget.Single -> target.name
				}
			}
			Text(text)
		},
		secondaryText = if(target is DbTestTarget.Group) ({
			val users = with(pref.db) { target.allUsers }.joinToString { it.name }
			Text(users)
		}) else null,
		singleLineSecondaryText = false
	)
}
