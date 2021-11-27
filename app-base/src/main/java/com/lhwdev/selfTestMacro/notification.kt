package com.lhwdev.selfTestMacro

import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lhwdev.selfTestMacro.database.DatabaseManager
import com.lhwdev.selfTestMacro.database.DbTestTarget


// TODO: setContentIntent


open class AndroidNotificationChannel(
	val channelId: String,
	val name: String,
	val description: String? = null,
	val importance: Int,
	val priority: Int
) {
	@RequiresApi(Build.VERSION_CODES.O)
	open fun createNotificationChannel(): NotificationChannel = NotificationChannel(channelId, name, importance).also {
		if(description != null) it.description = description
	}
	
	@Suppress("ExplicitThis")
	protected inline fun Context.buildNotification(block: NotificationCompat.Builder.() -> Unit): Notification =
		NotificationCompat.Builder(this, channelId).also {
			it.priority = priority
			it.setSmallIcon(App.appIconForeground)
			it.block()
		}.build()
}


const val sNotificationPrefix = "com.lhwdev.selfTestMacro"

object NotificationIds {
	const val selfTestSuccess = 1
	const val selfTestFailed = 2
	const val updateAvailable = 3
}


object SelfTestSuccessNotification : AndroidNotificationChannel(
	channelId = "$sNotificationPrefix/selfTestSuccess",
	name = "자가진단 완료",
	importance = NotificationManagerCompat.IMPORTANCE_LOW,
	priority = NotificationCompat.PRIORITY_LOW
) {
	fun notificationOf(
		context: Context,
		target: DbTestTarget,
		database: DatabaseManager,
		time: String
	) = context.buildNotification {
		val targetName = with(database) { target.name }
		setContentTitle("${targetName}의 건강상태 자가진단을 완료했어요.")
		setContentText("제출 시간: $time") // TODO: suspicious/quarantined status view
	}
}

object SelfTestFailedNotification : AndroidNotificationChannel(
	channelId = "$sNotificationPrefix/selfTestFailed",
	name = "자가진단 실패",
	importance = NotificationManagerCompat.IMPORTANCE_MAX,
	priority = NotificationCompat.PRIORITY_MAX
) {
	fun notificationOf(
		context: Context,
		target: DbTestTarget,
		database: DatabaseManager,
		message: String
	) = context.buildNotification {
		val targetName = with(database) { target.name }
		setContentTitle("${targetName}의 건강상태 자가진단이 실패했습니다.")
		setContentText(message)
	}
}

object SelfTestScheduleNotification : AndroidNotificationChannel(
	channelId = "$sNotificationPrefix/selfTestSchedule",
	name = "자가진단 예약 안내",
	description = "자가진단 예약을 실행하면서 어떤 일이 있었다면 알려줘요.",
	importance = NotificationManagerCompat.IMPORTANCE_LOW,
	priority = NotificationCompat.PRIORITY_LOW
) {
	fun notificationOf(context: Context, title: String, content: String) = context.buildNotification {
		setContentTitle(title)
		setContentText(content)
	}
}


// object BeforeTestNotification : AndroidNotificationEntry(
// 	channelId = "com.lhwdev.selfTestMacro/beforeSelfTest",
// 	name = "자가진단 예정",
// 	description = "예약된 자가진단이 실행되기 몇십분 전에 소리없이 표시됩니다.",
// 	importance = NotificationManagerCompat.IMPORTANCE_LOW,
// 	priority = NotificationCompat.PRIORITY_LOW
// ) {
// 	fun notificationOf(context: Context) = context.buildNotification {
// 		setContentTitle("자가진단")
// 	}
// }
