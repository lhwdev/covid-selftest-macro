@file:JvmName("AndroidUtils")

package com.lhwdev.selfTestMacro

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import android.os.Handler
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
import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
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

inline fun <R> tryAtMost(
	maxTrial: Int,
	errorFilter: (Throwable) -> Boolean = { true },
	onError: (th: Throwable) -> Unit = {},
	block: () -> R
): R {
	var trialCount = 0
	while(true) {
		try {
			return block()
		} catch(th: Throwable) {
			if(!errorFilter(th)) throw th
			trialCount++
			if(trialCount >= maxTrial) throw th
			onError(th)
		}
	}
}

@Serializable
data class UserLoginInfo(
	val institute: InstituteResult,
	val userQuery: UserQuery,
	val password: String,
	/*val unstableToken: UsersToken*/
) {
	// suspend inline fun <R> ensureTokenValid(
	// 	session: Session,
	// 	instituteInfo: InstituteInfo,
	// 	onUpdate: (info: UserLoginInfo) -> Unit,
	// 	block: (token: UsersToken) -> R
	// ): R {
	// 	return tryAtMost(
	// 		maxTrial = 3,
	// 		onError = { th ->
	// 			// try once more with refresh token
	// 			println("==== 재시도: ensureTokenValid ====")
	// 			val identifier = try {
	// 				session.findUser(instituteInfo, GetUserTokenRequestBody(
	// 					instituteInfo, identifier.mainUserName, birthday, loginType
	// 				))
	// 			} catch(th: Throwable) {
	// 				identifier
	// 			}
	//
	// 			val result = tryAtMost(3) {
	// 				session.validatePassword(instituteInfo, identifier, password)
	// 			}
	// 			val unstableToken = result as? UsersToken ?: throw IllegalStateException("로그인 실패: $result", th)
	// 			onUpdate(copy(identifier = identifier, unstableToken = unstableToken))
	// 		},
	// 		block = {
	// 			block(unstableToken)
	// 		}
	// 	)
	// }
	
	suspend fun findUser(session: Session, context: Context): UsersToken? {
		val pref = context.preferenceState
		val info = pref.info!!
		val instituteNew = session.getSchoolData(
			regionCode = info.institute.regionCode,
			schoolLevelCode = info.institute.schoolLevelCode!!,
			name = info.institute.info.name
		)
		val result = session.findUser(
			institute = institute,
			searchKey = instituteNew.searchKey,
			userQuery = userQuery,
			password = password
		)
		
		return when(result) {
			is FindUserResult.Failed -> {
				withContext(Dispatchers.Main) {
					context.showToastSuspendAsync(result.errorMessage ?: "?")
				}
				
				null
			}
			is FindUserResult.Success -> result.token
		}
	}
}

class PreferenceState(val pref: SharedPreferences) {
	init {
		// version migration
		when(pref.getInt("lastVersion", -1)) {
			in -1..1023 -> pref.edit { clear() }
			BuildConfig.VERSION_CODE -> Unit // latest
		}
		
		pref.edit { putInt("lastVersion", BuildConfig.VERSION_CODE) }
	}
	
	var isDebugEnabled by pref.preferenceBoolean("isDebugEnabled", false)
	
	var firstState by pref.preferenceInt("first", 0)
	var isSchedulingEnabled by pref.preferenceBoolean("isSchedulingEnabled", false)
	var isRandomEnabled by pref.preferenceBoolean("isRandomEnabled", false)
	var includeWeekend by pref.preferenceBoolean("includeWeekend", false)
	var isIsolated by pref.preferenceBoolean("isIsolated", false)
	
	var hour by pref.preferenceInt("hour", -1)
	var min by pref.preferenceInt("min", 0)
	
	var info: UserLoginInfo?
		by pref.preferenceSerialized("userLoginInfo", UserLoginInfo.serializer())
	
	var quickTest: QuickTestInfo?
		by pref.preferenceSerialized("quickTest", QuickTestInfo.serializer())
	
	var lastSubmit: Long
		get() = pref.getLong("lastSubmit", Long.MIN_VALUE)
		set(value) = pref.edit {
			putLong("lastSubmit", value)
		}
	
	// var appMeta: AppMeta? by pref.preferenceSerialized("appMeta", AppMeta.serializer())
	
	var lastQuestion: String? by pref.preferenceString("lastQuestion")
	
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

// suspend fun PreferenceState.appMeta(): AppMeta.Data {
// 	val day = millisToDaysCumulative(System.currentTimeMillis())
// 	val last = appMeta
// 	if(last != null && last.at == day) return last.data
//	
// 	val data = fetch(URL("https://raw.githubusercontent.com/wiki/lhwdev/covid-selftest-macro/app_meta.json"))
// 		.toJsonLoose(AppMeta.Data.serializer())
// 	appMeta = AppMeta(data, at = day)
// 	return data
// }

// @Serializable
// class AppMeta(val data: Data, val at: Long) {
// 	@Serializable
// 	class Data(
// 		val hcsVersion: String
// 	)
// }

@Serializable
data class QuickTestInfo(
	val days: Set<Int>,
	val behavior: Behavior
) {
	enum class Behavior { negative, doNotSubmit }
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
	formatter: Json = Json
) = object : ReadWriteProperty<Any?, T?> {
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
			updated = true
		}
		return cache
	}
}

fun Context.prefMain() = getSharedPreferences("main", AppCompatActivity.MODE_PRIVATE)


fun Context.createIntent() = PendingIntent.getBroadcast(
	this, AlarmReceiver.REQUEST_CODE, Intent(this, AlarmReceiver::class.java),
	PendingIntent.FLAG_UPDATE_CURRENT or (if(Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_IMMUTABLE else 0)
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


