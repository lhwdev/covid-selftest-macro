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
								1
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
											val answer = promptYesNoDialog(
												navigator,
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
): Unit = navigator.showRouteUnit { removeRoute ->
	fun Modifier.clickAction(block: () -> Unit): Modifier = clickable {
		block()
		removeRoute()
	}
	
	val target = group.target
	val pref = LocalPreference.current
	
	val groupName = with(pref.db) { target.name }
	
	MaterialDialog(onCloseRequest = removeRoute) {
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
			Column {
				// group members
				if(target is DbTestTarget.Group) ListItem(
					icon = {
						Icon(
							painterResource(R.drawable.ic_account_circle_24),
							contentDescription = null
						)
					},
					secondaryText = { Text("TODO: wow") }
				) {
					val text = with(pref.db) { target.allUsers }.joinToString { it.user.name }
					Text(text)
				}
				
				// disband group
				ListItem(modifier = Modifier.clickAction {
					scope.launch {
						var inheritSchedule by mutableStateOf(false)
						
						val doDisband = promptYesNoDialog(
							navigator,
							title = { Text("${groupName}을 해제할까요?") },
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
						val doDelete = promptYesNoDialog(
							navigator,
							title = { Text("${groupName}을 삭제할까요?") }
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
				icon = {},
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
	val scope = rememberCoroutineScope()
	
	fun selectGroupMember() = scope.launch {
		val member = navigator.showRoute<List<DbTestGroup>> { removeRoute ->
			MaterialDialog(onCloseRequest = { removeRoute(emptyList()) }) {
			
			}
		}
	}
	
	Title { Text("그룹 만들기") }
	Content {
		var groupName by remember { mutableStateOf("") }
		val focusManager = LocalFocusManager.current
		
		Input(focusOnShow = true) {
			TextField(
				value = groupName,
				onValueChange = { groupName = it },
				label = { Text("그룹 이름") },
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
				keyboardActions = KeyboardActions { focusManager.moveFocus(FocusDirection.Down) }
			)
			
			TextButton(onClick = { selectGroupMember() }) {
			
			}
		}
	}
}
