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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.database.*
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.primaryActive
import com.vanpra.composematerialdialogs.*


@Composable
fun FloatingMaterialDialogScope.SetupGroup(
	previousGroup: DbTestGroup?,
	initialSelection: List<DbTestGroup> = emptyList(),
	initialName: String = previousGroup?.target?.let { with(LocalPreference.current.db) { it.name } } ?: ""
) {
	val navigator = LocalNavigator
	val pref = LocalPreference.current
	val existing = remember(previousGroup) {
		if(previousGroup == null) emptyList()
		else with(pref.db) { previousGroup.target.allUsers }
	}
	
	val users = remember {
		pref.db.testGroups.groups.values.filter { it.target is DbTestTarget.Single }
	}
	
	val oldSelection = remember {
		mutableStateListOf<Boolean>().also {
			it += List(existing.size) { true }
		}
	}
	val newSelection = remember {
		mutableStateListOf<Boolean>().also {
			it += List(users.size) { index -> users[index] in initialSelection }
		}
	}
	
	fun selectGroupMember() = navigator.showDialogAsync {
		Title { Text("사용자 선택") }
		ListContent {
			for((index, user) in existing.withIndex()) ListItem(
				icon = {
					Checkbox(
						checked = oldSelection[index],
						onCheckedChange = null
					)
				},
				modifier = Modifier.clickable { oldSelection[index] = !oldSelection[index] }
			) {
				Text(user.name)
			}
			
			for((index, user) in users.withIndex()) ListItem(
				icon = {
					Checkbox(
						checked = newSelection[index],
						onCheckedChange = null
					)
				},
				modifier = Modifier.clickable { newSelection[index] = !newSelection[index] }
			) {
				Text(with(pref.db) { (user.target as DbTestTarget.Single).user.name })
			}
		}
		
		Buttons {
			PositiveButton(onClick = requestClose) { Text("확인") }
		}
	}
	
	
	var groupName by remember { mutableStateOf(initialName) }
	
	Title { Text(if(previousGroup == null) "그룹 만들기" else "그룹 수정") }
	Content {
		Column {
			val focusManager = LocalFocusManager.current
			
			this@SetupGroup.Input(focusOnShow = true, contentPadding = PaddingValues()) {
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
			
			val usersString = buildAnnotatedString {
				val old = existing.filterIndexed { index, _ -> oldSelection[index] }
				val new = users.filterIndexed { index, _ -> newSelection[index] }
				
				for((index, user) in old.withIndex()) {
					if(index != 0) append(", ")
					
					withStyle(SpanStyle(color = MaterialTheme.colors.primaryActive(.7f))) {
						append(user.name)
					}
				}
				
				if(old.isNotEmpty() && new.isNotEmpty()) append(", ")
				
				for((index, user) in new.withIndex()) {
					if(index != 0) append(", ")
					
					append(with(pref.db) { user.target.name })
				}
			}
			
			Text(
				text = usersString,
				modifier = Modifier.padding(8.dp)
			)
			
			// TODO: select schedule here
		}
	}
	
	Buttons {
		PositiveButton(onClick = {
			val realUsers = users.filterIndexed { index, _ -> newSelection[index] }
			
			val ids = pref.db.testGroups.groups.keys.toMutableList()
			
			if(previousGroup == null) {
				val group = DbTestGroup(
					target = DbTestTarget.Group(name = groupName, userIds = emptyList()),
					id = ids.nextTestGroupId()
				)
				
				// whether `group` is present in db.testGroups does not matter
				// this automatically adds `group`
				pref.db.moveToTestGroup(
					target = realUsers,
					toGroup = group
				)
			} else transactDb {
				val removed = existing.filterIndexed { index, _ -> !oldSelection[index] }
				var group = previousGroup
				
				if(removed.isNotEmpty()) group = pref.db.moveOutFromTestGroup(
					fromGroup = group,
					target = removed.map {
						DbTestGroup(
							id = ids.nextTestGroupId(),
							target = DbTestTarget.Single(userId = it.id)
						)
					}
				)
				
				@Suppress("UnnecessaryVariable")
				val added = realUsers
				
				if(added.isNotEmpty()) group = pref.db.moveToTestGroup(
					target = added,
					toGroup = group
				)
				
				pref.db.replaceTestGroupDangerous(
					from = group,
					to = group.copy(target = (group.target as DbTestTarget.Group).copy(name = groupName))
				)
			}
			
			navigator.popRoute()
		}) { Text("확인") }
		NegativeButton(onClick = requestClose) { Text("취소") }
	}
}
