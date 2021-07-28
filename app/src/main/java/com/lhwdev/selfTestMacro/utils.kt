@file:JvmName("AndroidUtils")

package com.lhwdev.selfTestMacro

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Handler
import android.util.Base64
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.doOnPreDraw
import androidx.core.view.setPadding
import com.google.android.material.snackbar.Snackbar
import com.lhwdev.selfTestMacro.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import java.util.WeakHashMap
import kotlin.coroutines.resume
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


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

@Serializable
data class UserLoginInfo(val identifier: UserIdentifier, val token: UsersToken)

class PreferenceState(val pref: SharedPreferences) {
	init {
		// version migration
		when(pref.getInt("lastVersion", -1)) {
			in -1..999 -> pref.edit { clear() }
			in 1000..1006 -> pref.edit { clear() }
			BuildConfig.VERSION_CODE -> Unit // latest
		}
		
		pref.edit { putInt("lastVersion", BuildConfig.VERSION_CODE) }
	}
	
	var isDebugEnabled by pref.preferenceBoolean("isDebugEnabled", false)
	
	var firstState by pref.preferenceInt("first", 0)
	var isSchedulingEnabled by pref.preferenceBoolean("isSchedulingEnabled", false)
	var hour by pref.preferenceInt("hour", -1)
	var min by pref.preferenceInt("min", 0)
	
	var user by pref.preferenceSerialized("userLoginInfo", UserLoginInfo.serializer())
	var institute by pref.preferenceSerialized("institute", InstituteInfo.serializer())
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

fun SharedPreferences.preferenceBoolean(key: String, defaultValue: Boolean) =
	object : ReadWriteProperty<Any?, Boolean> {
		override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
			edit { putBoolean(key, value) }
		}
		
		override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
			getBoolean(key, defaultValue)
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

fun Int.toPx() = (this * Resources.getSystem().displayMetrics.density).toInt()

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

suspend fun <R : Any> Context.promptDialog(
	block: AlertDialog.Builder.(result: (R) -> Unit) -> Unit
): R? = withContext(Dispatchers.Main) {
	suspendCancellableCoroutine { cont ->
		var invoked = false
		fun onResult(result: R?) {
			if(!invoked) {
				invoked = true
				cont.resume(result)
			}
		}
		var dialog: AlertDialog? = null
		dialog = AlertDialog.Builder(this@promptDialog).apply {
			block {
				onResult(it)
				dialog?.dismiss()
			}
			
			setOnDismissListener {
				onResult(null)
			}
			
			setOnCancelListener {
				onResult(null)
			}
			
		}.show()
		cont.invokeOnCancellation { dialog.dismiss() }
	}
}

suspend fun Context.promptInput(block: AlertDialog.Builder.(edit: EditText, okay: () -> Unit) -> Unit): String? =
	promptDialog { onResult ->
		val view = EditText(context)
		val okay = { onResult(view.text.toString()) }
		view.setPadding(16.toPx())
		view.imeOptions = EditorInfo.IME_ACTION_DONE
		view.setOnEditorActionListener { _, _, _ ->
			okay()
			true
		}
		view.doOnPreDraw { view.requestFocus() }
		setView(view)
		setPositiveButton("확인") { _, _ ->
			okay()
		}
		setNegativeButton("취소", null)
		block(view, okay)
	}
