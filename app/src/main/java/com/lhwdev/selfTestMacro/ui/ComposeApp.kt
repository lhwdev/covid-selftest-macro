@file:JvmName("AppKt")

package com.lhwdev.selfTestMacro.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.lhwdev.selfTestMacro.AlarmReceiver
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.database.DatabaseManager
import com.lhwdev.selfTestMacro.database.preferenceState
import com.lhwdev.selfTestMacro.debug.*
import com.lhwdev.selfTestMacro.debuggingWithIde
import com.lhwdev.selfTestMacro.navigation.ComposeNavigationHost
import com.lhwdev.selfTestMacro.navigation.FadeRouteTransition
import com.lhwdev.selfTestMacro.navigation.NavigatorImpl
import com.lhwdev.selfTestMacro.navigation.pushRoute
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.repository.SelfTestManager
import com.lhwdev.selfTestMacro.repository.SelfTestManagerImpl
import com.lhwdev.selfTestMacro.ui.pages.splash.Splash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


fun SelfTestManager(
	context: Context,
	database: DatabaseManager,
	debugContext: DebugContext,
	defaultCoroutineScope: CoroutineScope
): SelfTestManager = SelfTestManagerImpl(
	context = context,
	database = database,
	newAlarmIntent = { Intent(it, AlarmReceiver::class.java) },
	debugContext = debugContext,
	defaultCoroutineScope = defaultCoroutineScope
)


@Composable
fun ComposeApp(activity: Activity) {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	
	val pref = remember(context) { context.preferenceState }
	
	val debugContext = rememberRootDebugContext(
		contextName = "ComposeApp",
		flags = DebugContext.DebugFlags(
			enabled = context.isDebugEnabled,
			debuggingWithIde = App.debuggingWithIde
		),
		showErrorInfo = UiDebugContext::showErrorDialog,
		manager = DefaultDebugManager(androidContext = context, workScope = CoroutineScope(Dispatchers.Default))
	)
	
	val selfTestManager = remember {
		SelfTestManager(
			context = context.applicationContext,
			database = pref.db,
			debugContext = debugContext,
			defaultCoroutineScope = CoroutineScope(Dispatchers.Default)
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
