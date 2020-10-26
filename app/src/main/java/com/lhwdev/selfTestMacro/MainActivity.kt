package com.lhwdev.selfTestMacro

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import com.lhwdev.selfTestMacro.api.getDetailedUserInfo
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL


const val IGNORE_BATTERY_OPTIMIZATION_REQUEST = 1001


@Suppress("SpellCheckingInspection")
class MainActivity : AppCompatActivity() {
	
	private var batteryOptimizationPromptShown = false
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		val pref = PreferenceState(prefMain())
		preferenceState = pref
		
		val isFirst = pref.firstState == 0
		if(isFirst && !intent.getBooleanExtra("doneFirst", false)) {
			startActivity(Intent(this, FirstActivity::class.java).also {
				it.putExtra("first", true)
			})
			finish()
			return
		}
		
		pref.firstState = 1
		
		setContentView(R.layout.activity_main)
		setSupportActionBar(toolbar)
		
		initializeNotificationChannel()
		checkNotice()
		
		@SuppressLint("SetTextI18n")
		suspend fun updateCurrentState() = withContext(Dispatchers.IO) {
			val detailedUserInfo = try {
				getDetailedUserInfo(pref.school!!, pref.user!!)
			} catch(e: Throwable) {
				Log.e("hOI", null, e)
				showToastSuspendAsync("사용자 정보를 불러오지 못했습니다.")
				return@withContext
			}
			
			withContext(Dispatchers.Main) {
				@Suppress("SetTextI18n")
				text_currentUserState.text =
					detailedUserInfo.toUserInfoString() + "\n" + detailedUserInfo.toLastRegisterInfoString()
			}
		}
		
		lifecycleScope.launch {
			updateCurrentState()
		}
		
		val intent = createIntent()

//		val serviceIntent = Intent(this, MyService::class.java)
//		var service: MyService? = null
//		val connection = object : ServiceConnection {
//			override fun onServiceDisconnected(name: ComponentName) {
//				service = null
//			}
//
//			override fun onServiceConnected(name: ComponentName, theService: IBinder) {
//				service = (theService as MyService.LocalBinder).service
//			}
//		}
		
		fun updateTime() {
			time.text = if(pref.hour == -1) "시간 예약 안 됨" else "매일 자가진단: ${pref.hour}시 ${pref.min}분"
			updateTime(intent)
		}
		
		updateTime()
		
		time.setOnClickListener {
			TimePickerDialog(this, { _, newHour, newMin ->
				checkBatteryOptimizationPermission()
				
				pref.hour = newHour
				pref.min = newMin
				updateTime()
			}, if(pref.hour == -1) 0 else pref.hour, pref.min, false).show()
		}
		
		time.setOnLongClickListener {
			pref.hour = -1
			updateTime()
			Toast.makeText(this, "자동예약 취소", Toast.LENGTH_SHORT).show()
			true
		}
		
		submit.setOnClickListener {
			lifecycleScope.launch {
				submitSuspend(false)
				updateCurrentState()
			}
		}
	}
	
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
		enum class Priority { once, every }
	}
	
	private fun checkNotice() = lifecycleScope.launch(Dispatchers.IO) {
		try {
			val content =
				URL("https://raw.githubusercontent.com/wiki/lhwdev/covid-selftest-macro/_notice").readText()
			
			val notificationObject = Json {
				ignoreUnknownKeys = true /* loose */
			}.decodeFromString(NotificationObject.serializer(), content)
			
			if(notificationObject.notificationVersion != 2) {
				// incapable of displaying this
				return@launch
			}
			
			for(entry in notificationObject.entries) {
				val show = when(entry.priority) {
					NotificationEntry.Priority.once -> entry.id !in preferenceState.shownNotices
					NotificationEntry.Priority.every -> true
				}
				
				if(show) withContext(Dispatchers.Main) {
					AlertDialog.Builder(this@MainActivity).apply {
						setTitle(entry.title)
						setMessage(HtmlCompat.fromHtml(entry.message, 0))
						setPositiveButton("확인", null)
					}.show().apply {
						findViewById<TextView>(android.R.id.message)!!.movementMethod =
							LinkMovementMethod.getInstance()
					}
				}
			}
		} catch(e: Exception) {
			// ignore; - network error or etc
			// notification is not that important
		}
	}
	
	/*
	 * > Task automation app
	 *   App's core function is scheduling automated actions, such as for instant messaging, voice calling, or new photo management
	 *   Acceptable
	 *
	 * I won't upload it on Play Store (so far), but it's always good to follow guidelines
	 * In case I upload, I won't use this api, instead will use foreground service.
	 * https://developer.android.com/training/monitoring-device-state/doze-standby#exemption-cases
	 */
	private fun checkBatteryOptimizationPermission() {
		val pwrm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
		val name = applicationContext.packageName
		
		if(!batteryOptimizationPromptShown
			&& android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
			&& !pwrm.isIgnoringBatteryOptimizations(name)
		) {
			AlertDialog.Builder(this).apply {
				setTitle("베터리 최적화 설정을 꺼야 알림 기능이 정상적으로 작동됩니다.")
				setPositiveButton("설정") { _, _ ->
					@SuppressLint("BatteryLife")
					val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
					intent.data = Uri.parse("package:$packageName")
					startActivityForResult(intent, IGNORE_BATTERY_OPTIMIZATION_REQUEST)
				}
				
				setNegativeButton("취소", null)
			}.show()
			
			batteryOptimizationPromptShown = true
		}
	}
	
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.menu_main, menu)
		return true
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return when(item.itemId) {
			R.id.action_settings -> {
				startActivity(Intent(this, FirstActivity::class.java))
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}
}
