@file:JvmName("AndroidUtils")

package com.lhwdev.selfTestMacro

import android.app.PendingIntent
import android.content.*
import android.os.Build
import android.os.Handler
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.content.getSystemService
import com.google.android.material.snackbar.Snackbar
import com.lhwdev.selfTestMacro.api.LoginType
import com.lhwdev.selfTestMacro.api.SchoolInfo
import com.lhwdev.selfTestMacro.api.UserInfo
import com.lhwdev.selfTestMacro.api.encodeBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.util.WeakHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


val sDummyForInitialization: Unit = run {
	// NO_WRAP: this is where I was confused for a few days
	encodeBase64 = { Base64.encodeToString(it, Base64.NO_WRAP) }
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

fun EditText.isEmpty() = text == null || text.isEmpty()

@Serializable
data class UserSetting(
	val loginType: LoginType,
	val region: String,
	val level: Int,
	val schoolName: String,
	val studentName: String,
	val studentBirth: String
)

class PreferenceState(val pref: SharedPreferences) {
	init {
		// version migration
		when(pref.getInt("lastVersion", -1)) {
			in -1..999 -> pref.edit { clear() }
			BuildConfig.VERSION_CODE -> Unit // latest
		}
		
		pref.edit { putInt("lastVersion", BuildConfig.VERSION_CODE) }
	}
	
	var firstState by pref.preferenceInt("first", 0)
	var hour by pref.preferenceInt("hour", -1)
	var min by pref.preferenceInt("min", 0)
	
	var user by pref.preferenceSerialized("userInfo", UserInfo.serializer())
	var school by pref.preferenceSerialized("schoolInfo", SchoolInfo.serializer())
	var setting by pref.preferenceSerialized("userSetting", UserSetting.serializer())
	
	var shownNotices: Set<String>
		get() = pref.getStringSet("shownNotices", setOf())!!
		set(value) = pref.edit {
			putStringSet("shownNotices", value)
		}
	var doNotShowAgainNotices: Set<String>
		get() = pref.getStringSet("shownNotices", setOf())!!
		set(value) = pref.edit {
			putStringSet("shownNotices", value)
		}
}


// I knew that global things are bad in android(i waz lazy), but didn't know would be by far worst.
// Only one line below caused TWO bugs; I WON'T do like this in the future
//
// but, some decent ways to do this?
// 1. always passing it through argument: so complicated
// 2. keeping global with the initialization of Application; here [MainApplication]
// 3. passing via argument, but through extension receiver
// 4. like Ambient? : unsafe(though safer in Jetpack Compose)
// 5. ThreadLocal: what else from the original one

//lateinit var preferenceState: PreferenceState
//val isPreferenceInitialized get() = ::preferenceState.isInitialized

// ok, maybe decent way; pooling from cache
private val preferenceStateMap = WeakHashMap<Context, PreferenceState>()

val Context.preferenceState: PreferenceState
	get() = preferenceStateMap.getOrPut(applicationContext) {
		PreferenceState(prefMain())
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

@OptIn(ExperimentalSerializationApi::class)
fun <T> SharedPreferences.preferenceSerialized(
	key: String,
	serializer: KSerializer<T>,
	formatter: StringFormat = Json
) =
	object : ReadWriteProperty<Any?, T?> {
		var updated = false
		var cache: T? = null
		
		override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
			cache = value
			updated = true
			edit {
				if(value == null) remove(key)
				else putString(key, formatter.encodeToString(serializer, value))
			}
		}
		
		override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
			if(!updated) {
				val string = getString(key, null)
				cache = if(string == null) null else formatter.decodeFromString(serializer, string)
			}
			return cache
		}
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

fun View.showSnackBar(message: String, duration: Int = 3000) {
	Snackbar.make(this, message, duration).show()
}
