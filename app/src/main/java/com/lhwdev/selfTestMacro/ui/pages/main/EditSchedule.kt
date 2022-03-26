package com.lhwdev.selfTestMacro.ui.pages.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbTestSchedule
import com.lhwdev.selfTestMacro.database.DbTestTarget
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.repository.GroupInfo
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.showToast
import com.lhwdev.selfTestMacro.ui.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.MediumContentColor
import com.lhwdev.selfTestMacro.ui.Scrims
import com.lhwdev.selfTestMacro.ui.TopAppBar
import com.lhwdev.selfTestMacro.ui.common.CheckBoxListItem
import com.lhwdev.selfTestMacro.ui.common.SimpleIconButton
import com.lhwdev.selfTestMacro.ui.common.TestTargetListItem
import com.lhwdev.selfTestMacro.ui.utils.AnimateHeight
import com.lhwdev.selfTestMacro.ui.utils.TextCheckbox
import com.lhwdev.selfTestMacro.ui.utils.TimePickerDialog
import com.vanpra.composematerialdialogs.*


internal fun Navigator.showScheduleSelfTest(
	info: GroupInfo
): Unit = showFullDialogAsync { dismiss ->
	Surface(color = MaterialTheme.colors.background) {
		AutoSystemUi { scrims ->
			@Suppress("ExplicitThis")
			this@showFullDialogAsync.ScheduleContent(info, dismiss, scrims)
		}
	}
}

@Composable
private fun FullMaterialDialogScope.ScheduleContent(info: GroupInfo, dismiss: () -> Unit, scrims: Scrims) = Column {
	val context = LocalContext.current
	val selfTestManager = LocalSelfTestManager.current
	val navigator = LocalNavigator
	
	val group = info.group
	val target = group.target
	
	
	var type by remember {
		mutableStateOf(
			when(group.schedule) {
				DbTestSchedule.None -> ScheduleType.none
				is DbTestSchedule.Fixed -> ScheduleType.fixed
				is DbTestSchedule.Random -> ScheduleType.random
			}
		)
	}
	
	var hour by remember {
		mutableStateOf((group.schedule as? DbTestSchedule.Fixed)?.hour ?: -1)
	}
	var minute by remember {
		mutableStateOf((group.schedule as? DbTestSchedule.Fixed)?.minute ?: 0)
	}
	
	val random = group.schedule as? DbTestSchedule.Random
	var fromHour by remember { mutableStateOf(random?.from?.hour ?: -1) }
	var fromMinute by remember { mutableStateOf(random?.from?.minute ?: 0) }
	var toHour by remember { mutableStateOf(random?.to?.hour ?: -1) }
	var toMinute by remember { mutableStateOf(random?.to?.minute ?: 0) }
	
	var excludeWeekend by remember { mutableStateOf(group.excludeWeekend) }
	var altogether by remember { mutableStateOf(group.schedule.altogether) }
	
	
	TopAppBar(
		navigationIcon = {
			SimpleIconButton(icon = R.drawable.ic_clear_24, contentDescription = "닫기", onClick = dismiss)
		},
		title = { Text("자가진단 예약") },
		backgroundColor = Color.Transparent,
		elevation = 0.dp,
		statusBarScrim = scrims.statusBar
	)
	Divider()
	
	Column(
		modifier = Modifier
			.verticalScroll(rememberScrollState())
			.weight(1f)
	) {
		@Composable
		fun Header(text: String) {
			Text(
				text = text,
				style = MaterialTheme.typography.subtitle1,
				modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 20.dp)
			)
		}
		
		TestTargetListItem(target)
		
		Spacer(Modifier.height(12.dp))
		
		Header("자가진단 예약")
		
		@Composable
		fun ScheduleTypeHead(targetType: ScheduleType, text: String) {
			ListItem(
				icon = { RadioButton(selected = type == targetType, onClick = null) },
				modifier = Modifier.clickable {
					if(targetType == ScheduleType.random && targetType != type) {
						altogether = false // good default?
					}
					type = targetType
				}
			) {
				Text(text)
			}
		}
		
		@Composable
		fun ScheduleTime(
			label: String,
			hour: Int,
			minute: Int,
			setTime: (hour: Int, minute: Int) -> Unit,
			modifier: Modifier = Modifier
		) = Box(
			modifier
				.padding(8.dp)
				.clickable {
					navigator.showDialogAsync { dismiss ->
						TimePickerDialog(
							initialHour = if(hour == -1) 7 else hour,
							initialMinute = minute,
							setTime = { h, m ->
								setTime(h, m)
								dismiss()
							},
							cancel = dismiss
						)
					}
				}
		) {
			TextFieldDefaults.TextFieldDecorationBox(
				value = if(hour == -1) "" else "a",
				label = { Text(label) },
				innerTextField = {
					if(hour != -1) {
						val text = buildAnnotatedString {
							append(if(hour == 0) "12" else "$hour".padStart(2, padChar = '0'))
							withStyle(SpanStyle(color = MediumContentColor)) { append(":") }
							append("$minute".padStart(2, padChar = '0'))
						}
						Text(text)
					}
				},
				enabled = true, singleLine = true, visualTransformation = VisualTransformation.None,
				interactionSource = remember { MutableInteractionSource() }
			)
		}
		
		
		/// none
		ScheduleTypeHead(ScheduleType.none, "꺼짐")
		
		/// fixed
		ScheduleTypeHead(ScheduleType.fixed, "매일 특정 시간")
		
		AnimateHeight(
			visible = type == ScheduleType.fixed,
			modifier = Modifier.padding(horizontal = 16.dp)
		) {
			ScheduleTime(
				label = "시간 설정",
				hour = hour,
				minute = minute,
				setTime = { h, m ->
					hour = h
					minute = m
				},
				modifier = Modifier.fillMaxWidth()
			)
		}
		
		
		/// random
		ScheduleTypeHead(ScheduleType.random, "렌덤 시간")
		
		AnimateHeight(
			visible = type == ScheduleType.random,
			modifier = Modifier.padding(horizontal = 16.dp)
		) {
			Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
				Text(
					"시작 시간과 끝 시간 사이 임의의 시간에 자가진단을 합니다.",
					style = MaterialTheme.typography.body2
				)
				
				Row(verticalAlignment = Alignment.CenterVertically) {
					ScheduleTime(
						label = "범위 시작",
						hour = fromHour, minute = fromMinute,
						modifier = Modifier.weight(1f),
						setTime = { h, m ->
							fromHour = h
							fromMinute = m
						}
					)
					
					Text("~", style = MaterialTheme.typography.body1)
					
					ScheduleTime(
						label = "범위 끝",
						hour = toHour, minute = toMinute,
						modifier = Modifier.weight(1f),
						setTime = { h, m ->
							toHour = h
							toMinute = m
						}
					)
				}
				
				if(target is DbTestTarget.Group) {
					TextCheckbox(
						text = { Text("그룹원들을 동시에 자가진단") },
						checked = altogether,
						setChecked = { altogether = it }
					)
					
					if(altogether)
						Text("임의의 시간이 정해지면 그 때 그룹원들을 모두 자가진단합니다. 이 옵션이 체크되어 있지 않다면 정해진 범위 내에서 각각 따로 실행됩니다.")
				}
			}
		}
	}
	
	Spacer(Modifier.height(16.dp))
	
	// ListItem {
	// 	TextCheckbox(
	// 		text = { Text("주말에는 자가진단하지 않기") },
	// 		checked = excludeWeekend,
	// 		setChecked = { excludeWeekend = it }
	// 	)
	// }
	
	CheckBoxListItem(
		checked = excludeWeekend, onCheckChanged = { excludeWeekend = it }
	) { Text("주말에는 자가진단하지 않기") }
	
	Spacer(Modifier.height(4.dp))
	
	Buttons {
		PositiveButton(onClick = submit@{
			val schedule = when(type) {
				ScheduleType.none -> DbTestSchedule.None
				ScheduleType.fixed -> {
					if(hour == -1) {
						context.showToast("시간을 선택해주세요.")
						return@submit
					}
					DbTestSchedule.Fixed(hour = hour, minute = minute)
				}
				ScheduleType.random -> {
					if(fromHour == -1 || toHour == -1) {
						context.showToast("시간을 선택해주세요.")
						return@submit
					}
					DbTestSchedule.Random(
						from = DbTestSchedule.Fixed(hour = fromHour, minute = fromMinute),
						to = DbTestSchedule.Fixed(hour = toHour, minute = toMinute),
						altogether = true
					)
				}
			}
			selfTestManager.updateSchedule(
				target = group,
				new = DbTestGroup(
					id = group.id,
					target = group.target,
					schedule = schedule,
					excludeWeekend = excludeWeekend
				)
			)
			
			dismiss()
		})
		NegativeButton(onClick = requestClose)
	}
	
	scrims.navigationBar()
}
