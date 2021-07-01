@file:OptIn(ExperimentalMaterialApi::class)

package com.lhwdev.selfTestMacro

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
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
							IconButton(onClick = { route.add { Setup() } }) {
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


@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
private fun EditUsersContent(
	selection: MutableList<DbTestGroup>
) {
	val pref = LocalPreference.current
	
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
								if(selection.isNotEmpty()) {
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
