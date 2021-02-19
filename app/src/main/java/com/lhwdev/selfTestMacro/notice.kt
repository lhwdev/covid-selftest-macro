package com.lhwdev.selfTestMacro

import android.app.Activity
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import com.lhwdev.selfTestMacro.NotificationEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URL


suspend fun Activity.checkNotice() = withContext(Dispatchers.IO) {
	// also check updates
	val update = getUpdateAvailable()
	if(update != null) {
		askUpdate(update, 1001)
		return@withContext
	}
	
	val content: String?
	try {
		content =
			URL("https://raw.githubusercontent.com/wiki/lhwdev/covid-selftest-macro/notice_v4.json").readText()
		
		val notificationObject = Json {
			ignoreUnknownKeys = true /* loose */
		}.decodeFromString(NotificationObject.serializer(), content)
		
		if(notificationObject.notificationVersion != 4) {
			// incapable of displaying this
			return@withContext
		}
		
		Log.d("hOI", notificationObject.toString())
		
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
		// ignore; - network error or etc
		// notification is not that important
		
		onError(e, "알림")
	}
}
