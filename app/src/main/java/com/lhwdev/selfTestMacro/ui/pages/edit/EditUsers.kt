package com.lhwdev.selfTestMacro.ui.pages.edit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbTestTarget
import com.lhwdev.selfTestMacro.database.removeTestGroups
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.showRouteFactoryAsync
import com.lhwdev.selfTestMacro.ui.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.MediumContentColor
import com.lhwdev.selfTestMacro.ui.TopAppBar
import com.lhwdev.selfTestMacro.ui.common.SimpleIconButton
import com.lhwdev.selfTestMacro.ui.pages.main.iconFor
import com.lhwdev.selfTestMacro.ui.pages.setup.SetupParameters
import com.lhwdev.selfTestMacro.ui.pages.setup.SetupRoute
import com.vanpra.composematerialdialogs.promptYesNoDialog
import com.vanpra.composematerialdialogs.showDialogAsync
import kotlinx.coroutines.launch


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EditUsers() {
	val navigator = LocalNavigator
	val pref = LocalPreference.current
	val scope = rememberCoroutineScope()
	val selection = remember { mutableStateListOf<DbTestGroup>() }
	
	val groups = pref.db.testGroups.groups
	if(!groups.containsAll(selection)) {
		val filtered = selection.filter { it in groups }
		selection.clear()
		selection.addAll(filtered)
	}
	
	BackHandler(enabled = selection.isNotEmpty()) {
		selection.clear()
	}
	
	Surface(color = MaterialTheme.colors.surface) {
		AutoSystemUi { scrims ->
			Scaffold(
				topBar = {
					TopAppBar(
						navigationIcon = if(navigator.isRoot) null else ({
							SimpleIconButton(
								icon = R.drawable.ic_arrow_back_24,
								contentDescription = "뒤로 가기",
								onClick = { navigator.popRoute() }
							)
						}),
						title = { Text("사용자 편집") },
						actions = {
							var showAddDialog by remember { mutableStateOf(false) }
							
							SimpleIconButton(
								icon = R.drawable.ic_add_24, contentDescription = "추가",
								onClick = { showAddDialog = true }
							)
							
							DropdownMenu(
								expanded = showAddDialog,
								onDismissRequest = { showAddDialog = false }
							) {
								DropdownMenuItem(onClick = {
									showAddDialog = false
									navigator.showRouteFactoryAsync { SetupRoute(SetupParameters(endRoute = it)) }
								}) {
									Text("사용자 추가")
								}
								
								DropdownMenuItem(onClick = {
									showAddDialog = false
									navigator.showDialogAsync { SetupGroup(previousGroup = null) }
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
								SimpleIconButton(
									icon = R.drawable.ic_clear_24, contentDescription = "선택 취소",
									onClick = { selection.clear() }
								)
							},
							title = { Text("${selection.size}명 선택") },
							actions = {
								var moreActions by remember { mutableStateOf(false) }
								fun clickAction(block: () -> Unit): () -> Unit = {
									moreActions = false
									block()
								}
								SimpleIconButton(
									icon = R.drawable.ic_more_vert_24, contentDescription = "더브기",
									onClick = { moreActions = true }
								)
								
								DropdownMenu(
									expanded = moreActions,
									onDismissRequest = { moreActions = false }
								) {
									DropdownMenuItem(onClick = clickAction {
										groups.forEach {
											if(it !in selection) selection += it
										}
									}) { Text("모두 선택") }
									
									DropdownMenuItem(onClick = clickAction {
										navigator.showDialogAsync {
											SetupGroup(previousGroup = null, initialSelection = selection)
										}
									}) { Text("그룹 만들기") }
									
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
												if(groups.isEmpty()) {
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


@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
private fun EditUsersContent(
	selection: MutableList<DbTestGroup>
) {
	val pref = LocalPreference.current
	val navigator = LocalNavigator
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
								.joinToString(separator = ", ", limit = 4) { it.name }
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
				modifier = Modifier.clickable {
					navigator.showRouteFactoryAsync { SetupRoute(SetupParameters(endRoute = it)) }
				}
			) {
				Text("사용자 추가")
			}
		}
	}
}
