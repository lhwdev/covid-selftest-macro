package com.lhwdev.selfTestMacro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


object TestCompleteNotification {
	const val id = "com.lhwdev.selfTestMacro/selfTestCompleted"
	const val notificationId = 123
	const val failedName = "자가진단에 실패하였습니다."
	const val successName = "자가진단을 완료하였습니다."
	const val description = "자가진단을 완료하면 알람을 표시합니다."
	val content = { time: String -> "자가진단을 ${time}에 완료했습니다" }
	
	@RequiresApi(Build.VERSION_CODES.N)
	const val importance = NotificationManager.IMPORTANCE_DEFAULT
}


fun Context.initializeNotificationChannel() {
	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		// selfTestCompleted
		val channel = NotificationChannel(
			TestCompleteNotification.id,
			TestCompleteNotification.successName,
			TestCompleteNotification.importance
		).apply {
			description = TestCompleteNotification.description
		}
		
		val notificationManager: NotificationManager =
			getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.createNotificationChannel(channel)
	}
}

fun Context.showTestCompleteNotification(time: String) {
	val builder = NotificationCompat.Builder(this, TestCompleteNotification.id).apply {
		setSmallIcon(R.drawable.ic_launcher_foreground)
		setContentTitle(TestCompleteNotification.successName)
		setContentText(TestCompleteNotification.content(time))
		priority = NotificationCompat.PRIORITY_DEFAULT
	}
	
	with(NotificationManagerCompat.from(this)) {
		// notificationId is a unique int for each notification that you must define
		notify(TestCompleteNotification.notificationId, builder.build())
	}
}

fun Context.showTestFailedNotification(detailedMessage: String) {
	val builder = NotificationCompat.Builder(this, TestCompleteNotification.id).apply {
		setSmallIcon(R.drawable.ic_launcher_foreground)
		setContentTitle(TestCompleteNotification.failedName)
		setContentText(detailedMessage)
		setStyle(NotificationCompat.BigTextStyle().bigText(detailedMessage))
		priority = NotificationCompat.PRIORITY_DEFAULT
	}
	
	with(NotificationManagerCompat.from(this)) {
		// notificationId is a unique int for each notification that you must define
		notify(TestCompleteNotification.notificationId, builder.build())
	}
}
