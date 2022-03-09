package com.lhwdev.selfTestMacro.ui.pages.common

import androidx.compose.foundation.clickable
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import com.lhwdev.selfTestMacro.database.AppDatabase
import com.lhwdev.selfTestMacro.database.DbTestTarget
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.vanpra.composematerialdialogs.*


suspend fun Navigator.promptSelectUserInGroupDialog(
	title: String,
	target: DbTestTarget.Group,
	database: AppDatabase
): DbUser? = showDialog { removeRoute ->
	Title { Text(title) }
	
	ListContent {
		val allUsers = with(database) { target.allUsers }
		
		for(user in allUsers) ListItem(
			modifier = Modifier.clickable { removeRoute(user) },
			icon = { iconFor(user) },
			text = { Text(user.name) }
		)
	}
	
	Buttons {
		NegativeButton(onClick = requestClose)
	}
}
