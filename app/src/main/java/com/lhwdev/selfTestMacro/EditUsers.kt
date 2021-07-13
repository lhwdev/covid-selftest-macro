package com.lhwdev.selfTestMacro

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.promptYesNoDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EditUsers() {
	val route = LocalRoute.current
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
							IconButton(onClick = { showRouteAsync(route) { Setup() } }) {
								Icon(
									painterResource(R.drawable.ic_add_24),
									contentDescription = "사용자 추가"
								)
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
								fun actionButton(block: () -> Unit): () -> Unit = {
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
									DropdownMenuItem(onClick = actionButton {
										pref.db.testGroups.groups.forEach {
											if(it !in selection) selection += it
										}
									}) { Text("모두 선택") }
									
									DropdownMenuItem(onClick = actionButton {
										scope.launch {
										
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
	route: Route,
	group: DbTestGroup,
	scope: CoroutineScope
): Unit = showRouteUnit(route) { removeRoute ->
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
					}
				) {
					val text = with(pref.db) { target.allUsers }.joinToString { it.user.name }
					Text(text)
				}
				
				// disband group
				ListItem(modifier = Modifier.clickAction {
					scope.launch {
						var inheritSchedule by mutableStateOf(false)
						
						val doDisband = promptYesNoDialog(
							route,
							title = { Text("$groupName 그룹을 해제할까요?") },
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
							route,
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
	val route = LocalRoute.current
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
									showDetailsFor(route, group, scope)
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
		}
	}
}
