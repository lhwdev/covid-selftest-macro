package com.lhwdev.selfTestMacro

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONObject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


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

fun EditText.isEmpty() = text == null || text.isEmpty()

class PreferenceState(val pref: SharedPreferences) {
	init {
		// version migration
		when(pref.getInt("lastVersion", -1)) {
			-1 -> pref.edit { clear() }
			BuildConfig.VERSION_CODE -> Unit // latest
		}
		
		pref.edit { putInt("lastVersion", BuildConfig.VERSION_CODE) }
	}
	
	var firstState by pref.preferenceInt("first", 0)
	var hour by pref.preferenceInt("hour", -1)
	var min by pref.preferenceInt("min", 0)
	
	var userCache: TestUser? = null
	
	var user: TestUser
		get() = userCache ?: run {
			val value = Json.decodeFromString(TestUser.serializer(), pref.getString("user", null)!!)
			userCache = value
			value
		}
		set(value) {
			userCache = value
			pref.edit { putString("user", Json.encodeToString(TestUser.serializer(), value)) }
		}
	
	var shownNotices: Set<String>
		get() = pref.getStringSet("shownNotices", setOf())!!
		set(value) = pref.edit {
			putStringSet("shownNotices", value)
		}
}

lateinit var preferenceState: PreferenceState


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