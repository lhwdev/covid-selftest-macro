package com.lhwdev.selfTestMacro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


operator fun NotificationManagerCompat.set(id: Int, tag: String? = null, value: Notification?) {
	if(value == null) {
		cancel(tag, id)
	} else {
		notify(tag, id, value)
	}
}


open class AndroidNotificationChannel(
	val channelId: String,
	val name: String,
	val description: String? = null,
	val importance: Int,
	val priority: Int
) {
	@RequiresApi(Build.VERSION_CODES.O)
	private var notificationChannelCache: NotificationChannel? = null
	
	val notificationChannel: NotificationChannel
		@RequiresApi(Build.VERSION_CODES.O)
		get() = notificationChannelCache ?: run {
			val new = createNotificationChannel()
			notificationChannelCache = new
			new
		}
	
	@RequiresApi(Build.VERSION_CODES.O)
	fun acquireNotificationChannel(context: Context): NotificationChannel =
		context.getSystemService<NotificationManager>()!!.getNotificationChannel(channelId)
	
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

object AppNotificationIds {
	const val selfTestSuccess = 1
	const val selfTestFailed = 2
	const val selfTestProgress = 3
	const val updateAvailable = 10
}


object AppNotifications {
	object SelfTestSuccess : AndroidNotificationChannel(
		channelId = "$sNotificationPrefix/selfTestSuccess",
		name = "자가진단 완료",
		importance = NotificationManagerCompat.IMPORTANCE_LOW,
		priority = NotificationCompat.PRIORITY_LOW
	) {
		private val dateFormat = SimpleDateFormat("MM/dd kk:mm", Locale.US)
		
		fun notificationOf(
			context: Context,
			target: String,
			time: Long?
		) = context.buildNotification {
			setContentTitle("${target}의 건강상태 자가진단을 완료했어요.")
			if(time != null) {
				// TODO: suspicious/quarantined status view
				setContentText("${dateFormat.format(Date(time))}에 자가진단을 제출했어요.")
			}
		}
	}
	
	object SelfTestFailed : AndroidNotificationChannel(
		channelId = "$sNotificationPrefix/selfTestFailed",
		name = "자가진단 실패",
		importance = NotificationManagerCompat.IMPORTANCE_MAX,
		priority = NotificationCompat.PRIORITY_MAX
	) {
		fun notificationOf(
			context: Context,
			target: String,
			message: String
		) = context.buildNotification {
			setContentTitle("${target}의 건강상태 자가진단이 실패했습니다.")
			setContentText(message)
		}
	}
	
	object SelfTestProgress : AndroidNotificationChannel(
		channelId = "$sNotificationPrefix/selfTestProgress",
		name = "자가진단 실행 현황",
		description = "자가진단 예약을 실행하는 중에 현황을 알려줘요.",
		importance = NotificationManagerCompat.IMPORTANCE_LOW,
		priority = NotificationCompat.PRIORITY_LOW
	) {
		@RequiresApi(Build.VERSION_CODES.O)
		override fun createNotificationChannel(): NotificationChannel = super.createNotificationChannel().also {
			
		}
		
		fun notificationOf(
			context: Context,
			allUsersCount: Int,
			doneCount: Int,
			failedCount: Int
		) = context.buildNotification {
			setContentTitle("자가진단 실행 현황")
			setContentText(
				if(failedCount == 0) {
					"${allUsersCount}명 중 ${doneCount}명의 자가진단을 완료했어요."
				} else {
					"${allUsersCount}명 중 ${failedCount}명의 자가진단을 실패했고, ${doneCount}명의 자가진단을 성공적으로 완료했어요."
				}
			)
			setLights(0xffff2200.toInt(), 1000, 0)
		}
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
