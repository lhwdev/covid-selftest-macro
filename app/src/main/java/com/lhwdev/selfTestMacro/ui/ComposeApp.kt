@file:JvmName("AppKt")

package com.lhwdev.selfTestMacro.ui

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.database.DatabaseManager
import com.lhwdev.selfTestMacro.database.preferenceState
import com.lhwdev.selfTestMacro.debug.DebugContext
import com.lhwdev.selfTestMacro.debug.isDebugEnabled
import com.lhwdev.selfTestMacro.debug.rememberDebugContext
import com.lhwdev.selfTestMacro.navigation.ComposeNavigationHost
import com.lhwdev.selfTestMacro.navigation.FadeRouteTransition
import com.lhwdev.selfTestMacro.navigation.NavigatorImpl
import com.lhwdev.selfTestMacro.navigation.pushRoute
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.repository.SelfTestManager
import com.lhwdev.selfTestMacro.repository.SelfTestManagerImpl
import com.lhwdev.selfTestMacro.ui.pages.splash.Splash
import kotlinx.coroutines.flow.collect


fun SelfTestManager(
	context: Context,
	database: DatabaseManager,
	debugContext: DebugContext
): SelfTestManager = SelfTestManagerImpl(
	context = context,
	database = database,
	newAlarmIntent = { Intent(it, AlarmManager::class.java) },
	debugContext = debugContext
)


@Composable
fun ComposeApp(activity: Activity) {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	
	val pref = remember(context) { context.preferenceState }
	
	val debugContext = rememberDebugContext(
		flags = DebugContext.DebugFlags(
			enabled = context.isDebugEnabled,
			debuggingWithIde = App.flavor == "dev"
		)
	)
	
	val selfTestManager = remember {
		SelfTestManager(
			context.applicationContext, pref.db, debugContext
		)
	}
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
		AppCompositionLocalsPack(
			preference = pref,
			debugContext = debugContext
		) {
			CompositionLocalProvider(
				LocalSelfTestManager provides selfTestManager
			) {
				ProvideAutoWindowInsets {
					Box {
						if(pref.isVirtualServer) Text(
							"가상 서버",
							modifier = Modifier
								.background(Color.Black.copy(alpha = .3f))
								.align(Alignment.TopEnd)
						)
						
						ComposeNavigationHost(navigator)
					}
				}
			}
			
			BackHandler(navigator.size > 1) {
				navigator.popLastRoute()
			}
		}
	}
}
