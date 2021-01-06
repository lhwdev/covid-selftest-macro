package com.lhwdev.selfTestMacro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder


fun Context.initializeNotificationChannel() {
	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		val notificationManager: NotificationManager =
			getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		
		val testCompleteChannel = NotificationChannel(
			TestCompleteNotification.id,
			TestCompleteNotification.visibleName,
			TestCompleteNotification.importance
		)
		notificationManager.createNotificationChannel(testCompleteChannel)
		
		val updateAvailableChannel = NotificationChannel(
			UpdateAvailableNotification.id,
			UpdateAvailableNotification.visibleName,
			UpdateAvailableNotification.importance
		)
		notificationManager.createNotificationChannel(updateAvailableChannel)
	}
}

object TestCompleteNotification {
	const val id = "com.lhwdev.selfTestMacro/selfTestCompleted"
	const val notificationId = 123
	const val failedName = "자가진단에 실패하였습니다."
	const val successName = "자가진단을 완료하였습니다."
	const val visibleName = "자가진단을 완료 알람"
	
	val content = { time: String -> "자가진단을 ${time}에 완료했습니다" }
	
	@RequiresApi(Build.VERSION_CODES.N)
	const val importance = NotificationManager.IMPORTANCE_DEFAULT
}

fun Context.showTestCompleteNotification(time: String) {
	val builder = NotificationCompat.Builder(this, TestCompleteNotification.id).apply {
		setSmallIcon(R.drawable.ic_launcher_foreground)
		setContentTitle(TestCompleteNotification.successName)
		setContentText(TestCompleteNotification.content(time))
		priority = NotificationCompat.PRIORITY_DEFAULT
	}
	
	NotificationManagerCompat.from(this)
		.notify(TestCompleteNotification.notificationId, builder.build())
}

fun Context.showTestFailedNotification(detailedMessage: String) {
	val builder = NotificationCompat.Builder(this, TestCompleteNotification.id).apply {
		setSmallIcon(R.drawable.ic_launcher_foreground)
		setContentTitle(TestCompleteNotification.failedName)
		setContentText(detailedMessage)
		setStyle(NotificationCompat.BigTextStyle().bigText(detailedMessage))
		priority = NotificationCompat.PRIORITY_DEFAULT
	}
	
	NotificationManagerCompat.from(this)
		.notify(TestCompleteNotification.notificationId, builder.build())
}


object UpdateAvailableNotification {
	const val id = "com.lhwdev.selfTestMacro/updateAvailable"
	const val notificationId = 124
	const val name = "자가진단 매크로 업데이트 안내"
	const val visibleName = "업데이트 안내"
	val content = { version: String -> "버전 ${version}이 사용가능합니다." }
	
	@RequiresApi(Build.VERSION_CODES.N)
	const val importance = NotificationManager.IMPORTANCE_DEFAULT
}

fun Context.showUpdateAvailableNotification(version: String) {
	val context = this@showUpdateAvailableNotification
	val builder = NotificationCompat.Builder(context, UpdateAvailableNotification.id).apply {
		setSmallIcon(R.drawable.ic_launcher_foreground)
		setContentTitle(UpdateAvailableNotification.name)
		setContentText(UpdateAvailableNotification.content(version))
		setContentIntent(TaskStackBuilder.create(context).run {
			addNextIntentWithParentStack(Intent(context, UpdateActivity::class.java))
			getPendingIntent(10124, PendingIntent.FLAG_ONE_SHOT)
		})
		priority = NotificationCompat.PRIORITY_DEFAULT
	}
	
	NotificationManagerCompat.from(context)
		.notify(UpdateAvailableNotification.notificationId, builder.build())
}
