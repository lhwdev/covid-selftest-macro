package com.lhwdev.selfTestMacro.ui.pages.main

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbTestSchedule
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.repository.GroupInfo
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.repository.dayOf
import com.lhwdev.selfTestMacro.showToast
import com.lhwdev.selfTestMacro.ui.EmptyRestartable
import com.lhwdev.selfTestMacro.ui.MediumContentColor
import com.lhwdev.selfTestMacro.ui.common.CheckBoxListItem
import com.lhwdev.selfTestMacro.ui.common.SimpleIconButton
import com.lhwdev.selfTestMacro.ui.common.TestTargetListItem
import com.lhwdev.selfTestMacro.ui.primarySurfaceColored
import com.lhwdev.selfTestMacro.ui.systemUi.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.systemUi.Scrims
import com.lhwdev.selfTestMacro.ui.systemUi.TopAppBar
import com.lhwdev.selfTestMacro.ui.utils.AnimateHeight
import com.lhwdev.selfTestMacro.ui.utils.ClickableTextFieldDecoration
import com.lhwdev.selfTestMacro.ui.utils.TimePickerDialog
import com.lhwdev.selfTestMacro.utils.headToLocalizedString
import com.lhwdev.selfTestMacro.utils.rememberTimeStateOf
import com.lhwdev.selfTestMacro.utils.tailToLocalizedString
import com.vanpra.composematerialdialogs.*
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

private class Time(val hour: Int, val minute: Int)

private fun DbTestSchedule.Fixed.toTime() = Time(hour, minute)
private fun Time.toFixed() = DbTestSchedule.Fixed(hour, minute)


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
	
	var fixedTime by remember { mutableStateOf((group.schedule as? DbTestSchedule.Fixed)?.toTime()) }
	
	val random = group.schedule as? DbTestSchedule.Random
	var randomFrom by remember { mutableStateOf(random?.from?.toTime()) }
	var randomTo by remember { mutableStateOf(random?.to?.toTime()) }
	
	var excludeWeekend by remember { mutableStateOf(group.excludeWeekend) }
	var altogether by remember { mutableStateOf(group.schedule.altogether) }
	
	
	TopAppBar(
		navigationIcon = {
			SimpleIconButton(icon = R.drawable.ic_clear_24, contentDescription = "닫기", onClick = dismiss)
		},
		title = { Text("자가진단 예약") },
		backgroundColor = Color.Transparent,
		elevation = 0.dp,
		statusBarScrim = scrims.statusBars
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
			time: Time?,
			initialTimeForPicker: Time = Time(hour = 7, minute = 0),
			setTime: (Time) -> Unit,
			modifier: Modifier = Modifier
		) {
			ClickableTextFieldDecoration(
				onClick = {
					navigator.showDialogAsync { dismiss ->
						TimePickerDialog(
							initialHour = time?.hour ?: initialTimeForPicker.hour,
							initialMinute = time?.minute ?: initialTimeForPicker.minute,
							setTime = { h, m ->
								setTime(Time(h, m))
								dismiss()
							},
							cancel = dismiss
						)
					}
				},
				isEmpty = time == null,
				label = { Text(label) },
				modifier = modifier.padding(8.dp)
			) {
				if(time != null) {
					val text = buildAnnotatedString {
						append(if(time.hour == 0) "12" else "${time.hour}".padStart(2, padChar = '0'))
						withStyle(SpanStyle(color = MediumContentColor)) { append(":") }
						append("${time.minute}".padStart(2, padChar = '0'))
					}
					Text(text)
				}
			}
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
				time = fixedTime,
				setTime = { fixedTime = it },
				modifier = Modifier.fillMaxWidth()
			)
		}
		
		
		/// random
		ScheduleTypeHead(ScheduleType.random, "매일 렌덤 시간")
		
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
						time = randomFrom,
						setTime = { randomFrom = it },
						modifier = Modifier.weight(1f)
					)
					
					Text("~", style = MaterialTheme.typography.body1)
					
					ScheduleTime(
						label = "범위 끝",
						time = randomTo,
						initialTimeForPicker = randomFrom ?: Time(hour = 7, minute = 0),
						modifier = Modifier.weight(1f),
						setTime = { randomTo = it }
					)
				}
				
				// I don't think this feature is needed
				// if(target is DbTestTarget.Group) {
				// 	TextCheckbox(
				// 		text = { Text("그룹원들을 동시에 자가진단") },
				// 		checked = altogether,
				// 		setChecked = { altogether = it }
				// 	)
				//	
				// 	if(altogether)
				// 		Text("임의의 시간이 정해지면 그 때 그룹원들을 모두 자가진단합니다. 이 옵션이 체크되어 있지 않다면 정해진 범위 내에서 각각 따로 실행됩니다.")
				// }
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
		checked = !excludeWeekend, onCheckChanged = { excludeWeekend = !it }
	) { Text("주말에도 자가진단하기") }
	
	EmptyRestartable {
		val tasks = selfTestManager.schedules.getTasks(group)
		
		if(tasks.isNotEmpty()) Surface(
			color = MaterialTheme.colors.primarySurfaceColored,
			contentColor = MaterialTheme.colors.onPrimary
		) {
			val now by rememberTimeStateOf(unit = TimeUnit.DAYS)
			
			val range = tasks.fold(Long.MAX_VALUE to Long.MIN_VALUE) { range, task ->
				min(range.first, task.timeMillis) to max(range.second, task.timeMillis)
			}
			
			val first = Calendar.getInstance().also { it.timeInMillis = range.first }
			val last = Calendar.getInstance().also { it.timeInMillis = range.second }
			
			val sameDay = dayOf(range.first) == dayOf(range.second)
			
			ListItem(
				text = {
					val text = if(sameDay) {
						"${first.headToLocalizedString(now)} " +
							"${first.tailToLocalizedString(now)}~${last.tailToLocalizedString(now)}"
					} else {
						val a = first.headToLocalizedString(now) + " " + first.tailToLocalizedString(now)
						val b = last.headToLocalizedString(now) + " " + last.tailToLocalizedString(now)
						"$a~$b"
					}
					Text(text)
				}
			)
		}
	}
	
	Spacer(Modifier.height(4.dp))
	
	Buttons {
		PositiveButton(onClick = submit@{
			val schedule = when(type) {
				ScheduleType.none -> DbTestSchedule.None
				ScheduleType.fixed -> {
					val time = fixedTime
					if(time == null) {
						context.showToast("시간을 선택해주세요.")
						return@submit
					}
					time.toFixed()
				}
				ScheduleType.random -> {
					val from = randomFrom
					val to = randomTo
					if(from == null || to == null) {
						context.showToast("시간을 선택해주세요.")
						return@submit
					}
					DbTestSchedule.Random(
						from = from.toFixed(),
						to = to.toFixed(),
						altogether = altogether
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
	
	scrims.navigationBars()
}
