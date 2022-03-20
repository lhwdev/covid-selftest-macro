package com.lhwdev.selfTestMacro.ui.pages.common

import androidx.compose.foundation.clickable
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.lhwdev.selfTestMacro.database.AppDatabase
import com.lhwdev.selfTestMacro.database.DbTestTarget
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.navigation.Route
import com.vanpra.composematerialdialogs.*


suspend fun Navigator.promptSelectUserInGroupDialog(
	title: String,
	target: DbTestTarget.Group,
	database: AppDatabase
): DbUser? = showDialog(Route(name = "selectUserInGroupDialog")) { removeRoute ->
	Title { Text(title) }
	
	ListContent {
		val allUsers = with(database) { target.allUsers }
		
		for(user in allUsers) ListItem(
			modifier = Modifier.clickable { removeRoute(user) },
			icon = { Icon(painterResource(iconFor(user)), contentDescription = null) },
			text = { Text(user.name) }
		)
	}
	
	Buttons {
		NegativeButton(onClick = requestClose)
	}
}
