package com.lhwdev.selfTestMacro

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import com.lhwdev.github.repo.Release
import com.lhwdev.github.repo.getRelease
import com.lhwdev.selfTestMacro.database.preferenceState
import com.lhwdev.selfTestMacro.debug.GlobalDebugContext
import com.lhwdev.selfTestMacro.models.Version
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.vanpra.composematerialdialogs.*
import kotlinx.serialization.Serializable
import java.io.File


object LatestVersion {
	@Serializable
	class Root(val appMain: Entry, val components: Map<String, Entry>)
	
	@Serializable
	class Entry(val version: Version, val releaseId: String)
}

enum class UpdateResult { updated, alreadyLatest, error }


// TODO: update components
suspend fun Context.getUpdateAvailable(): LatestVersion.Entry? {
	val pref = preferenceState
	val updateChannel = pref.updateChannel
	
	return try {
		val latest = App.metaBranch.getContent("update/latest-$updateChannel.json")
			.toJsonLoose(LatestVersion.Root.serializer())
		
		if(latest.appMain.version > App.version) latest.appMain else null
	} catch(e: Throwable) {
		GlobalDebugContext.onLightError("getUpdateAvailable", e)
		null
	}
}


suspend fun Context.checkAndNotifyUpdate() {
	try {
		val update = getUpdateAvailable() ?: return
		NotificationManagerCompat.from(this).notify(
			NotificationIds.updateAvailable,
			UpdateAvailableNotification.notificationOf(this, isRequired = false, toVersion = update.toString())
		)
	} catch(e: Throwable) {
		GlobalDebugContext.onLightError("checkUpdate", e)
	}
}

suspend fun Activity.checkAndAskUpdate(navigator: Navigator, requestCode: Int): UpdateResult? {
	return try {
		val update = getUpdateAvailable() ?: return UpdateResult.alreadyLatest
		val release = App.githubRepo.getRelease(update.releaseId)
		askUpdate(navigator, release, requestCode)
	} catch(e: Throwable) {
		GlobalDebugContext.onLightError("checkAndAskUpdate", e)
		UpdateResult.error
	}
}


const val sUpdateApkExternalPath = "update/download_apk.apk"

suspend fun Activity.askUpdate(navigator: Navigator, update: Release, requestCode: Int): UpdateResult? {
	val result = navigator.showDialog<Boolean> { removeRoute ->
		Title { Text("업데이트 안내") }
		Content {
			Text("버전 ${update.tagName}으로 업데이트할 수 있어요.")
			Spacer(Modifier.height(16.dp))
			Text(update.body) // TODO: support markdown
		}
		Buttons {
			PositiveButton(onClick = { removeRoute(true) }) { Text("업데이트") }
			NegativeButton(onClick = requestClose) { Text("취소") }
		}
	}
	if(result != true) return null
	
	return performUpdate(update, requestCode)
}

// suspend fun Activity.checkAndUpdate(requestCode: Int): UpdateResult {
// 	val update = getUpdateAvailable() ?: return UpdateResult.alreadyLatest
// 	val release = App.githubRepo.getRelease(update.releaseId)
// 	return performUpdate(release, requestCode)
// }

suspend fun Activity.performUpdate(update: Release, requestCode: Int): UpdateResult {
	try {
		val apkAsset = update.assets.find { it.name == "app-release.apk" } ?: return UpdateResult.error
		
		val url = apkAsset.browserDownloadUrl
		
		val externalPath = sUpdateApkExternalPath
		val updateApk = File(getExternalFilesDir(null), externalPath)
		
		val downloadManager = getSystemService<DownloadManager>()!!
		val download = DownloadManager.Request(Uri.parse(url.toExternalForm()))
		download.apply {
			setMimeType("application/vnd.android.package-archive")
			setTitle("업데이트를 다운받고 있어요.")
			setDestinationInExternalFilesDir(this@performUpdate, null, externalPath)
		}
		
		val lastDownloadedReceiver = sApkDownloadedReceiver
		if(lastDownloadedReceiver != null) unregisterReceiver(lastDownloadedReceiver)
		val newDownloadedReceiver = object : BroadcastReceiver() {
			override fun onReceive(context: Context, intent: Intent) {
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					val contentUri = FileProvider.getUriForFile(
						context,
						BuildConfig.APPLICATION_ID + ".fileProvider",
						updateApk
					)
					val install = Intent(Intent.ACTION_VIEW)
					install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
					install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
					install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
					install.setDataAndType(contentUri, "application/vnd.android.package-archive")
					ActivityCompat.startActivityForResult(
						this@performUpdate,
						install,
						requestCode,
						null
					)
					unregister(context)
					// finish()
				} else {
					val install = Intent(Intent.ACTION_VIEW)
					install.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
					install.setDataAndType(
						Uri.fromFile(updateApk),
						"application/vnd.android.package-archive"
					)
					context.startActivity(install)
					unregister(context)
				}
			}
			
			fun unregister(context: Context) {
				context.unregisterReceiver(this)
				sApkDownloadedReceiver = null
			}
		}
		registerReceiver(
			newDownloadedReceiver,
			IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
		)
		sApkDownloadedReceiver = newDownloadedReceiver
		
		downloadManager.enqueue(download)
		
		return UpdateResult.updated
	} catch(e: Throwable) {
		GlobalDebugContext.onLightError("askUpdate", e)
		return UpdateResult.error
	}
}


private var sApkDownloadedReceiver: BroadcastReceiver? = null
