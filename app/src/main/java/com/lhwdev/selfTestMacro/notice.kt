package com.lhwdev.selfTestMacro

import android.app.Activity
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import com.lhwdev.selfTestMacro.api.JsonLoose
import com.lhwdev.selfTestMacro.database.preferenceState
import com.lhwdev.selfTestMacro.debug.GlobalDebugContext
import com.lhwdev.selfTestMacro.models.VersionSpec
import com.lhwdev.selfTestMacro.navigation.Navigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.net.URL

@Serializable
data class NotificationObject(
	val notificationVersion: Int,
	val entries: List<NotificationEntry>
)

@Serializable
data class NotificationEntry(
	val id: String,
	val version: VersionSpec?, // null to all versions
	val priority: Priority,
	val title: String,
	val message: String
) {
	enum class Priority { once, everyWithDoNotShowAgain, every }
}


suspend fun Activity.checkNotice(navigator: Navigator) = withContext(Dispatchers.IO) {
	// also check updates
	// checkAndAskUpdate(navigator, 1001)
	
	val content: String?
	try {
		@Suppress("BlockingMethodInNonBlockingContext") // withContext(Dispatchers.IO)
		content =
			URL("https://raw.githubusercontent.com/wiki/lhwdev/covid-selftest-macro/notice_v4.json").readText()
		
		val notificationObject = JsonLoose.decodeFromString(NotificationObject.serializer(), content)
		
		if(notificationObject.notificationVersion != 4) {
			// incapable of displaying this
			return@withContext
		}
		
		val currentVersion = App.version
		
		for(entry in notificationObject.entries) {
			var show = when(entry.priority) {
				NotificationEntry.Priority.once -> entry.id !in preferenceState.shownNotices
				NotificationEntry.Priority.everyWithDoNotShowAgain -> entry.id !in preferenceState.doNotShowAgainNotices
				NotificationEntry.Priority.every -> true
			}
			show = show && (entry.version?.let { currentVersion in it } ?: true)
			
			if(show) withContext(Dispatchers.Main) {
				AlertDialog.Builder(this@checkNotice).apply {
					setTitle(entry.title)
					setMessage(HtmlCompat.fromHtml(entry.message, 0))
					setPositiveButton("확인") { _, _ ->
						preferenceState.shownNotices += entry.id
					}
					if(entry.priority == NotificationEntry.Priority.everyWithDoNotShowAgain)
						setNegativeButton("다시 보지 않기") { _, _ ->
							preferenceState.doNotShowAgainNotices += entry.id
							preferenceState.shownNotices += entry.id
						}
				}.show().apply {
					findViewById<TextView>(R.id.message)?.movementMethod =
						LinkMovementMethod.getInstance()
				}
			}
		}
	} catch(e: Exception) {
		// ignore; - network error etc.
		// notification is not that important
		
		GlobalDebugContext.onLightError("notification", throwable = e)
	}
}
