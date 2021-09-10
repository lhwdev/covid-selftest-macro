package com.lhwdev.selfTestMacro.ui.pages.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.database.*
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.vanpra.composematerialdialogs.*


@Composable
fun FloatingMaterialDialogScope.NewGroup(initialSelection: List<DbTestGroup> = emptyList()) {
	val navigator = LocalNavigator
	val pref = LocalPreference.current
	
	val users = remember {
		pref.db.testGroups.groups.filter { it.target is DbTestTarget.Single }
	}
	
	val selection = remember {
		mutableStateListOf<Boolean>().also {
			it += List(users.size) { index -> users[index] in initialSelection }
		}
	}
	
	fun selectGroupMember() = navigator.showDialogAsync {
		Title { Text("사용자 추가") }
		ListContent {
			Column {
				for((index, user) in users.withIndex()) ListItem(
					icon = {
						Checkbox(
							checked = selection[index],
							onCheckedChange = null
						)
					},
					modifier = Modifier.clickable { selection[index] = !selection[index] }
				) {
					Text(with(pref.db) { (user.target as DbTestTarget.Single).user.name })
				}
			}
		}
		
		Buttons {
			PositiveButton { Text("확인") }
		}
	}
	
	
	var groupName by remember { mutableStateOf("") }
	
	Title { Text("그룹 만들기") }
	Content {
		Column {
			val focusManager = LocalFocusManager.current
			
			this@NewGroup.Input(focusOnShow = true, contentPadding = PaddingValues()) {
				TextField(
					value = groupName,
					onValueChange = { groupName = it },
					label = { Text("그룹 이름") },
					keyboardOptions = KeyboardOptions(
						keyboardType = KeyboardType.Text,
						imeAction = ImeAction.Next
					),
					keyboardActions = KeyboardActions { focusManager.moveFocus(FocusDirection.Down) },
					modifier = Modifier.fillMaxWidth()
				)
			}
			
			Spacer(Modifier.height(8.dp))
			
			TextButton(onClick = { selectGroupMember() }) { Text("사용자 선택") }
			Text(
				users.filterIndexed { index, _ -> selection[index] }
					.joinToString { with(pref.db) { it.target.name } },
				modifier = Modifier.padding(8.dp)
			)
			
			// TODO: select schedule here
		}
	}
	
	Buttons {
		PositiveButton(onClick = {
			val realUsers = users.filterIndexed { index, _ -> selection[index] }
			
			val ids = pref.db.testGroups.ids
			val group = DbTestGroup(
				target = DbTestTarget.Group(name = groupName, userIds = emptyList()),
				id = ids.nextTestGroupId()
			)
			
			// whether `group` is present in db.testGroups does not matter
			// this automatically adds `group`
			pref.db.moveToTestGroup(
				target = realUsers.map { (it.target as DbTestTarget.Single).userId to it },
				toGroup = group
			)
			
			navigator.popRoute()
		}) { Text("확인") }
		NegativeButton { Text("취소") }
	}
}
