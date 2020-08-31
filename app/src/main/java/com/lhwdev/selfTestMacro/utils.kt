package com.lhwdev.selfTestMacro

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


// our project is too small to use form like 'major.minor.bugFixes'
data class Version(val major: Int, val minor: Int) : Comparable<Version> {
	override fun compareTo(other: Version) =
		if(major == other.major) minor - other.minor
		else major - other.major
}

// {"id": "$id", "priority": "once | every", "title": "$title", "message": "$message"}
class Notice(obj: JSONObject) {
	enum class Priority { once, every }
	
	val id: String = obj.getString("id")
	
	val priority = when(obj.getString("priority")) {
		"once" -> Priority.once
		"every" -> Priority.every
		else -> error("wow")
	}
	
	val title: String = obj.getString("title")
	val message: String = obj.getString("message")
}


fun Version(string: String): Version {
	val split = string.split('.').map { it.toInt() }
	require(split.size == 2)
	return Version(split[0], split[1])
}

data class VersionSpec(val from: Version, val to: Version) {
	operator fun contains(version: Version) = version in from..to
}

fun VersionSpec(string: String): VersionSpec {
	val index = string.indexOf("..")
	if(index == -1) return Version(string).let { VersionSpec(it, it) }
	val from = string.take(index)
	val to = string.drop(index + 2)
	return VersionSpec(Version(from), Version(to))
}

class PreferenceState(val pref: SharedPreferences) {
	var firstState by pref.preferenceInt("first", 0)
	var hour by pref.preferenceInt("hour", -1)
	var min by pref.preferenceInt("min", 0)
	
	var siteString by pref.preferenceString("site", "https://eduro.dge.go.kr")
	var site
		get() = URL(siteString)
		set(value) {
			siteString = value.toString()
		}
	var cert by pref.preferenceString("cert")
	
	var studentInfo
		get() = StudentInfo(
			schoolName = pref.getString("schoolName", "?")!!,
			studentName = pref.getString("studentName", "?")!!
		)
		set(value) = pref.edit {
			putString("schoolName", value.schoolName)
			putString("studentName", value.studentName)
		}
	
	var shownNotices: Set<String>
		get() = pref.getStringSet("shownNotices", setOf())!!
		set(value) = pref.edit {
			putStringSet("shownNotices", value)
		}
}

lateinit var preferenceState: PreferenceState

data class StudentInfo(val schoolName: String, val studentName: String) {
	override fun toString() = "$studentName ($schoolName)"
}


fun SharedPreferences.preferenceInt(key: String, defaultValue: Int) =
	object : ReadWriteProperty<Any?, Int> {
		override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
			edit { putInt(key, value) }
		}
		
		override fun getValue(thisRef: Any?, property: KProperty<*>): Int =
			getInt(key, defaultValue)
	}

fun SharedPreferences.preferenceString(key: String, defaultValue: String? = null) =
	object : ReadWriteProperty<Any?, String?> {
		override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
			edit { putString(key, value) }
		}
		
		override fun getValue(thisRef: Any?, property: KProperty<*>): String? =
			getString(key, defaultValue)
	}

fun Context.prefMain() = getSharedPreferences("main", AppCompatActivity.MODE_PRIVATE)


fun Context.createIntent() = PendingIntent.getBroadcast(
	this, AlarmReceiver.REQUEST_CODE, Intent(this, AlarmReceiver::class.java),
	PendingIntent.FLAG_UPDATE_CURRENT
)

fun Context.updateTime(intent: PendingIntent) {
	val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
	alarmManager.cancel(intent)
	if(preferenceState.hour != -1)
		scheduleNextAlarm(intent, preferenceState.hour, preferenceState.min)
}

@SuppressLint("NewApi")
fun Context.scheduleNextAlarm(
	intent: PendingIntent,
	hour: Int,
	min: Int,
	nextDay: Boolean = false
) {
	(getSystemService(Context.ALARM_SERVICE) as AlarmManager).setExact(
		AlarmManager.RTC_WAKEUP,
		Calendar.getInstance().run {
			val new = clone() as Calendar
			new[Calendar.HOUR_OF_DAY] = hour
			new[Calendar.MINUTE] = min
			new[Calendar.SECOND] = 0
			new[Calendar.MILLISECOND] = 0
			if(nextDay || new <= this) new.add(Calendar.DAY_OF_YEAR, 1)
			Log.e("HOI", "$this")
			Log.e("HOI", "$new")
			new.timeInMillis
		},
		intent
	)
}

@Suppress("NOTHING_TO_INLINE")
inline fun Context.runOnUiThread(noinline action: () -> Unit) {
	Handler(mainLooper).post(action)
}

suspend fun Context.showToastSuspendAsync(message: String, isLong: Boolean = false) =
	withContext(Dispatchers.Main) {
		showToast(message, isLong)
	}

fun Context.showToast(message: String, isLong: Boolean = false) {
	Toast.makeText(this, message, if(isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

@Suppress("SpellCheckingInspection")
suspend fun checkStudentInfoSuspend(site: URL, cert: String) = withContext(Dispatchers.IO) {
	val checkRequest = URL(site, "/stv_cvd_co01_000.do")
		.openConnection() as HttpURLConnection
	checkRequest.requestMethod = "POST"
	DataOutputStream(checkRequest.outputStream).use {
		it.writeBytes(
			"rtnRsltCode=SUCCESS&qstnCrtfcNoEncpt=${
				URLEncoder.encode(
					cert,
					"utf-8"
				)
			}&schulNm=&stdntName=&rspns01=1&rspns02=1&rspns07=0&rspns08=0&rspns09=0"
		)
		it.flush()
	}
	
	val responceContent = checkRequest.inputStream.reader().readText()
	
	if(checkRequest.responseCode != 200) {
		Log.e("HOI", responceContent)
		throw IOException("self test failed: ${checkRequest.responseMessage}")
	}
	
	val response = JSONObject(responceContent)
	val resultSVO = response.getJSONObject("resultSVO")
	
	if(resultSVO.getString("rtnRsltCode") != "SUCCESS")
		throw IOException("self test failed: validation failed - ${checkRequest.responseMessage} / $responceContent")
	
	Log.i("HOI", "got student info: $responceContent")
	
	val schulNm = resultSVO.getString("schulNm")
	val stdntName = resultSVO.getString("stdntName")
	
	StudentInfo(schoolName = schulNm, studentName = stdntName)
}

@Suppress("SpellCheckingInspection")
suspend fun Context.submitSuspend() = withContext(Dispatchers.IO) {
	val site = preferenceState.site
	val cert = preferenceState.cert
	
	//  send form
	val request = URL(site, "/stv_cvd_co02_000.do")
		.openConnection() as HttpURLConnection
	request.requestMethod = "POST"
	request.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
	request.setRequestProperty(
		"User-Agent",
		"Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8"
	)
	HttpURLConnection.setFollowRedirects(true)
	DataOutputStream(request.outputStream).use {
		it.writeBytes(
			"rtnRsltCode=SUCCESS&qstnCrtfcNoEncpt=${
				URLEncoder.encode(
					cert,
					"utf-8"
				)
			}&schulNm=&stdntName=&rspns01=1&rspns01=2&rspns02=1&rspns03=1&rspns05=1&rspns13=1&rspns14=1&rspns15=1&rspns04=1&rspns11=1&rspns07=0&rspns07=1&rspns08=0&rspns08=1&rspns09=0&rspns09=1"
		)
		it.flush()
	}
	Log.i("HOI", "${request.responseMessage} / ${request.inputStream.reader().readText()}")
	
	showToastSuspendAsync("자가진단 완료")
	
	// log
	File(getExternalFilesDir(null)!!, "log.txt")
		.appendText(
			"self-tested at ${
				DateFormat.getDateTimeInstance().format(Date())
			} ${request.responseMessage}\n"
		)
}
