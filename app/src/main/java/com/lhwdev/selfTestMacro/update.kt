package com.lhwdev.selfTestMacro

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import com.lhwdev.github.repo.Release
import com.lhwdev.github.repo.getReleaseLatest
import com.lhwdev.selfTestMacro.models.Version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


suspend fun Context.getUpdateAvailable() = withContext(Dispatchers.IO) {
	try {
		val latest = App.githubRepo.getReleaseLatest()
		val name = latest.tagName
		require(name.startsWith("v"))
		val version = Version(name.substring(1))
		if(version > App.version) latest else null
	} catch(e: Throwable) {
		onError(e, "getUpdateAvailable")
		null
	}
}


suspend fun Context.checkUpdate() {
	try {
		val update = getUpdateAvailable() ?: return
		showUpdateAvailableNotification(update.tagName)
	} catch(e: Throwable) {
		onError(e, "checkUpdate")
	}
}

suspend fun Activity.checkAndAskUpdate(requestCode: Int): Boolean {
	return try {
		val update = getUpdateAvailable() ?: return false
		
		askUpdate(update, requestCode)
	} catch(e: Throwable) {
		onError(e, "checkAndAskUpdate")
		false
	}
}


const val sUpdateApkExternalPath = "update/download_apk.apk"

suspend fun Activity.askUpdate(update: Release, requestCode: Int): Boolean {
	try {
		val apkAsset = update.assets.find { it.name == "app-release.apk" } ?: return false
		
		val result = promptDialog<Boolean> { onResult ->
			setTitle("업데이트 안내")
			setMessage("버전 ${update.tagName}으로 업데이트할 수 있습니다.")
			setPositiveButton("업데이트") { _, _ -> onResult(true) }
			setNegativeButton("취소", null)
		}
		if(result != true) return false
		val url = apkAsset.browserDownloadUrl
		
		val externalPath = sUpdateApkExternalPath
		val updateApk = File(getExternalFilesDir(null), externalPath)
		
		val downloadManager = getSystemService<DownloadManager>()!!
		val download = DownloadManager.Request(Uri.parse(url.toExternalForm()))
		download.apply {
			setMimeType("application/vnd.android.package-archive")
			setTitle("업데이트를 다운받고 있습니다.")
			setDestinationInExternalFilesDir(this@askUpdate, null, externalPath)
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
						this@askUpdate,
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
		
		return true
	} catch(e: Throwable) {
		onError(e, "askUpdate")
		return false
	}
}


private var sApkDownloadedReceiver: BroadcastReceiver? = null
