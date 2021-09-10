@file:JvmName("AppKt")

package com.lhwdev.selfTestMacro.ui

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.lhwdev.selfTestMacro.database.DatabaseManager
import com.lhwdev.selfTestMacro.database.preferenceState
import com.lhwdev.selfTestMacro.navigation.ComposeNavigationHost
import com.lhwdev.selfTestMacro.navigation.FadeRouteTransition
import com.lhwdev.selfTestMacro.navigation.NavigatorImpl
import com.lhwdev.selfTestMacro.navigation.pushRoute
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.repository.SelfTestManager
import com.lhwdev.selfTestMacro.repository.SelfTestManagerImpl
import com.lhwdev.selfTestMacro.ui.pages.splash.Splash
import kotlinx.coroutines.flow.collect


fun SelfTestManager(context: Context, database: DatabaseManager): SelfTestManager = SelfTestManagerImpl(
	context = context,
	database = database,
	newAlarmIntent = { Intent(it, AlarmManager::class.java) }
)


@Composable
fun ComposeApp(activity: Activity) {
	val context = LocalContext.current
	val pref = remember(context) { context.preferenceState }
	val selfTestManager = remember { SelfTestManager(context.applicationContext, pref.db) }
	selfTestManager.context = context.applicationContext
	
	val navigator = remember {
		val navigator = NavigatorImpl()
		
		navigator.pushRoute(
			transition = FadeRouteTransition(animationSpec = tween(durationMillis = 400))
		) { Splash() }
		
		navigator
	}
	
	LaunchedEffect(pref) {
		snapshotFlow {
			pref.db.testGroups
		}.collect {
			selfTestManager.onScheduleUpdated()
		}
	}
	
	AppTheme {
		CompositionLocalProvider(
			LocalPreference provides pref,
			LocalSelfTestManager provides selfTestManager
		) {
			ProvideAutoWindowInsets {
				ComposeNavigationHost(navigator)
			}
		}
		
		BackHandler(navigator.size > 1) {
			navigator.popLastRoute()
		}
	}
}
