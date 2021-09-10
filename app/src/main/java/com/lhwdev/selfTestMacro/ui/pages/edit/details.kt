package com.lhwdev.selfTestMacro.ui.pages.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.database.*
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.scheduleInfo
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


internal suspend fun showDetailsFor(
	navigator: Navigator,
	group: DbTestGroup,
	scope: CoroutineScope
): Unit = navigator.showDialogUnit { removeRoute ->
	fun Modifier.clickAction(block: () -> Unit): Modifier = clickable {
		removeRoute()
		block()
	}
	
	val target = group.target
	val pref = LocalPreference.current
	
	val groupName = with(pref.db) { target.name }
	
	Scaffold(
		topBar = {
			Column {
				TopAppBar(
					navigationIcon = {
						IconButton(onClick = removeRoute) {
							Icon(
								painterResource(R.drawable.ic_clear_24),
								contentDescription = "닫기"
							)
						}
					},
					title = {
						if(target is DbTestTarget.Group) {
							Text("$groupName (${group.target.allUserIds.size}명)")
						} else {
							Text(groupName)
						}
					},
					backgroundColor = Color.Transparent,
					elevation = 0.dp
				)
				Divider()
			}
		}
	) {
		when(target) {
			is DbTestTarget.Single -> Column {
				ListItem(
					icon = {
						Icon(
							painterResource(R.drawable.ic_account_circle_24),
							contentDescription = null
						)
					},
					text = { Text(with(pref.db) { target.user.institute.name }) },
					secondaryText = { Text("자가진단 예약: ${group.scheduleInfo()}") }
				)
				
				ListItem(modifier = Modifier.clickAction {
					scope.launch {
						val doDelete = navigator.promptYesNoDialog(
							title = { Text("${with(pref.db) { target.name }}을(를) 삭제할까요?") }
						)
						
						if(doDelete == true) pref.db.removeTestGroup(group)
					}
				}) { Text("사용자 삭제") }
				val list = pref.db.testGroups.groups.filter {
					it.target is DbTestTarget.Group
				}
				
				if(list.isNotEmpty()) ListItem(modifier = Modifier.clickAction {
					scope.launch {
						val moveTarget = navigator.showDialog<DbTestGroup> { removeRoute ->
							Title { Text("이동할 그룹 선택") }
							ListContent {
								Column {
									for(item in list) {
										val itemTarget = item.target as DbTestTarget.Group
										ListItem(
											text = { Text(itemTarget.name) },
											secondaryText = { Text(with(pref.db) { itemTarget.allUsers.joinToString { it.name } }) },
											modifier = Modifier.clickable {
												removeRoute(item)
											}
										)
									}
								}
							}
							Buttons {
								NegativeButton { Text("취소") }
							}
						}
						
						if(moveTarget != null) pref.db.moveToTestGroup(
							target = listOf(target.userId to group),
							toGroup = moveTarget
						)
					}
				}) { Text("그룹 이동") }
			}
			is DbTestTarget.Group -> Column {
				// group members
				ListItem(
					icon = {
						Icon(
							painterResource(R.drawable.ic_group_24),
							contentDescription = null
						)
					},
					secondaryText = { Text("자가진단 예약: ${group.scheduleInfo()}") }
				) {
					val text = with(pref.db) { target.allUsers }.joinToString { it.name }
					Text(text)
				}
				
				// rename
				ListItem(modifier = Modifier.clickAction {
					navigator.showDialogAsync { removeRoute ->
						var name by remember {
							mutableStateOf(
								TextFieldValue(
									text = groupName,
									selection = TextRange(0, groupName.length)
								)
							)
						}
						
						fun done() {
							pref.db.renameTestGroup(group, name.text)
							removeRoute()
						}
						
						Title { Text("그룹 이름 바꾸기") }
						
						Input(focusOnShow = true) {
							TextField(
								name, onValueChange = { name = it },
								keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
								keyboardActions = KeyboardActions { done() }
							)
						}
						
						Buttons {
							PositiveButton(onClick = { done() }) { Text("변경") }
							NegativeButton { Text("취소") }
						}
					}
				}) {
					Text("그룹 이름 바꾸기")
				}
				
				// disband group
				ListItem(modifier = Modifier.clickAction {
					scope.launch {
						var inheritSchedule by mutableStateOf(false)
						
						val doDisband = navigator.promptYesNoDialog(
							title = { Text("${groupName}을(를) 해제할까요?") },
							content = {
								ListItem(
									icon = {
										Checkbox(
											checked = inheritSchedule,
											onCheckedChange = null
										)
									},
									text = { Text("그룹의 자가진단 예약 상태 유지") },
									modifier = Modifier.clickable {
										inheritSchedule = !inheritSchedule
									}
								)
							}
						)
						
						if(doDisband == true) {
							pref.db.disbandGroup(group, inheritSchedule = inheritSchedule)
						}
					}
				}) {
					Text("그룹 해제")
				}
				
				// remove group and group members entirely
				ListItem(modifier = Modifier.clickAction {
					scope.launch {
						val doDelete = navigator.promptYesNoDialog(
							title = { Text("${groupName}을(를) 삭제할까요?") }
						)
						
						if(doDelete == true) pref.db.removeTestGroup(group)
					}
				}) { Text("그룹과 그룹원 삭제") }
			}
			
		}
	}
}
