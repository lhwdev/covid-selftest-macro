package com.lhwdev.selfTestMacro

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EditUsers() {
	val navigator = currentNavigator
	val pref = LocalPreference.current
	val scope = rememberCoroutineScope()
	val selection = remember { mutableStateListOf<DbTestGroup>() }
	
	BackHandler(enabled = selection.isNotEmpty()) {
		selection.clear()
	}
	
	Surface(color = MaterialTheme.colors.surface) {
		AutoSystemUi { scrims ->
			Scaffold(
				topBar = {
					TopAppBar(
						navigationIcon = if(navigator.isRoot) null else ({
							IconButton(onClick = { navigator.popRoute() }) {
								Icon(
									painterResource(R.drawable.ic_arrow_back_24),
									contentDescription = "뒤로 가기"
								)
							}
						}),
						title = { Text("사용자 편집") },
						actions = {
							var showAddDialog by remember { mutableStateOf(false) }
							
							IconButton(onClick = { showAddDialog = true }) {
								Icon(
									painterResource(R.drawable.ic_add_24),
									contentDescription = "추가"
								)
							}
							
							DropdownMenu(
								expanded = showAddDialog,
								onDismissRequest = { showAddDialog = false }
							) {
								DropdownMenuItem(onClick = {
									showAddDialog = false
									navigator.showRouteAsync { Setup() }
								}) {
									Text("사용자 추가")
								}
								
								DropdownMenuItem(onClick = {
									showAddDialog = false
									navigator.showRouteAsync { NewGroup() }
								}) {
									Text("그룹 만들기")
								}
							}
						},
						backgroundColor = MaterialTheme.colors.surface,
						statusBarScrim = scrims.statusBar
					)
					AnimatedVisibility(
						visible = selection.isNotEmpty(),
						modifier = Modifier.height(IntrinsicSize.Max),
						enter = fadeIn(),
						exit = fadeOut()
					
					) {
						TopAppBar(
							navigationIcon = {
								IconButton(onClick = { selection.clear() }) {
									Icon(
										painterResource(R.drawable.ic_clear_24),
										contentDescription = "선택 취소"
									)
								}
							},
							title = { Text("${selection.size}명 선택") },
							actions = {
								var moreActions by remember { mutableStateOf(false) }
								fun clickAction(block: () -> Unit): () -> Unit = {
									moreActions = false
									block()
								}
								IconButton(onClick = { moreActions = true }) {
									Icon(
										painterResource(R.drawable.ic_more_vert_24),
										contentDescription = "더브기"
									)
								}
								
								DropdownMenu(
									expanded = moreActions,
									onDismissRequest = { moreActions = false }
								) {
									DropdownMenuItem(onClick = clickAction {
										pref.db.testGroups.groups.forEach {
											if(it !in selection) selection += it
										}
									}) { Text("모두 선택") }
									
									DropdownMenuItem(onClick = clickAction {
										scope.launch {
											val answer = navigator.promptYesNoDialog(
												title = {
													val group =
														selection.count { it.target is DbTestTarget.Group }
													val user = selection.size - group
													
													val qualifier = when {
														group == 0 -> "사용자 ${user}명"
														user == 0 -> "그룹 ${group}개"
														else -> "사용자 ${user}명과 그룹 ${group}개"
													}
													Text("${qualifier}를 완전히 삭제할까요?")
												}
											)
											
											if(answer == true) {
												pref.db.removeTestGroups(selection)
												selection.clear()
												if(pref.db.testGroups.groups.isEmpty()) {
													navigator.popRoute()
												}
											}
										}
									}) { Text("삭제") }
								}
							},
							backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
								.compositeOver(MaterialTheme.colors.surface),
							statusBarScrim = scrims.statusBar
						)
					}
				}
			) {
				Column(modifier = Modifier.padding(it)) {
					Box(Modifier.weight(1f)) {
						EditUsersContent(selection = selection)
					}
					scrims.navigationBar()
				}
			}
		}
	}
}


private suspend fun showDetailsFor(
	navigator: Navigator,
	group: DbTestGroup,
	scope: CoroutineScope
): Unit = navigator.showDialogUnit { removeRoute ->
	fun Modifier.clickAction(block: () -> Unit): Modifier = clickable {
		block()
		removeRoute()
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
					text = { Text(with(pref.db) { target.user.instituteName }) },
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
											secondaryText = { Text(with(pref.db) { itemTarget.allUsers.joinToString { it.user.name } }) },
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
						
						if(moveTarget != null) pref.db.moveToGroup(
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
							painterResource(R.drawable.ic_account_circle_24),
							contentDescription = null
						)
					},
					secondaryText = { Text("자가진단 예약: ${group.scheduleInfo()}") }
				) {
					val text = with(pref.db) { target.allUsers }.joinToString { it.user.name }
					Text(text)
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


@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
private fun EditUsersContent(
	selection: MutableList<DbTestGroup>
) {
	val pref = LocalPreference.current
	val navigator = currentNavigator
	val scope = rememberCoroutineScope()
	
	with(pref.db) {
		val groups = testGroups.groups
		
		Column(
			modifier = Modifier.verticalScroll(rememberScrollState())
		) {
			for(group in groups) key(group) {
				ListItem(
					icon = {
						Box {
							androidx.compose.animation.AnimatedVisibility(
								visible = selection.isEmpty(),
								enter = fadeIn(),
								exit = fadeOut()
							) {
								Icon(
									painterResource(iconFor(group.target)),
									contentDescription = null
								)
							}
							androidx.compose.animation.AnimatedVisibility(
								visible = selection.isNotEmpty(),
								enter = fadeIn(),
								exit = fadeOut()
							) {
								Checkbox(checked = group in selection, onCheckedChange = null)
							}
						}
					},
					text = {
						Row {
							Text(group.target.name)
							
							if(group.target is DbTestTarget.Group) Text(
								" (${group.target.allUsers.size}명)",
								color = MediumContentColor
							)
						}
					},
					secondaryText = if(group.target is DbTestTarget.Group) ({
						Text(
							group.target.allUsers
								.joinToString(separator = ", ", limit = 4) { it.user.name }
						)
					}) else null,
					modifier = Modifier
						.combinedClickable(
							onLongClick = {
								if(selection.isEmpty()) selection += group
							},
							onClick = {
								if(selection.isEmpty()) scope.launch {
									showDetailsFor(navigator, group, scope)
								} else {
									val last = group in selection
									if(last) selection -= group
									else selection += group
								}
							}
						)
						.animateContentSize()
				)
			}
			
			ListItem(
				icon = { Icon(painterResource(R.drawable.ic_add_24), contentDescription = null) },
				modifier = Modifier.clickable { navigator.showRouteAsync { Setup() } }
			) {
				Text("사용자 추가")
			}
		}
	}
}


@Composable
fun NewGroup(): Unit = MaterialDialog(onCloseRequest = currentNavigator.onPopRoute) {
	val navigator = currentNavigator
	val pref = LocalPreference.current
	
	val users = remember {
		pref.db.testGroups.groups.filter { it.target is DbTestTarget.Single }
	}
	
	val selection = remember {
		mutableStateListOf<Boolean>().also {
			it += List(users.size) { false }
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
					Text(with(pref.db) { (user.target as DbTestTarget.Single).user.user.name })
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
			
			this@MaterialDialog.Input(focusOnShow = true, contentPadding = PaddingValues()) {
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
			
			// TODO: select schedule here
		}
	}
	
	Buttons {
		PositiveButton(onClick = {
			val realUsers = users.filterIndexed { index, _ -> selection[index] }
			
			val group = DbTestGroup(
				target = DbTestTarget.Group(name = groupName, userIds = emptyList())
			)
			
			// whether `group` is present in db.testGroups does not matter
			// this automatically adds `group`
			pref.db.moveToGroup(
				target = realUsers.map { (it.target as DbTestTarget.Single).userId to it },
				toGroup = group
			)
		}) { Text("확인") }
		NegativeButton { Text("취소") }
	}
}
