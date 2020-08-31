package com.lhwdev.selfTestMacro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.DateFormat
import java.util.Date


object TestCompleteNotification {
	const val id = "com.lhwdev.selfTestMacro/selfTestCompleted"
	const val notificationId = 123
	const val name = "자가진단 완료"
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
			TestCompleteNotification.name,
			TestCompleteNotification.importance
		).apply {
			description = TestCompleteNotification.description
		}
		
		val notificationManager: NotificationManager =
			getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.createNotificationChannel(channel)
	}
}

fun Context.showTestCompleteNotification() {
	val builder = NotificationCompat.Builder(this, TestCompleteNotification.id).apply {
		setSmallIcon(R.drawable.ic_launcher_foreground)
		setContentTitle(TestCompleteNotification.name)
		setContentText(TestCompleteNotification.content(DateFormat.getDateTimeInstance().format(Date())))
		priority = NotificationCompat.PRIORITY_DEFAULT
	}
	
	with(NotificationManagerCompat.from(this)) {
		// notificationId is a unique int for each notification that you must define
		notify(TestCompleteNotification.notificationId, builder.build())
	}
}
