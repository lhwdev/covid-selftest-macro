package com.lhwdev.selfTestMacro

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.content_main.submit
import kotlinx.android.synthetic.main.content_main.time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
			pref.firstState = 1
			
			startActivity(Intent(this, FirstActivity::class.java).also {
				it.putExtra("first", true)
			})
			finish()
			return
		}
		
		setContentView(R.layout.activity_main)
		setSupportActionBar(toolbar)
		
		initializeNotificationChannel()
		checkNotice()
		
		title = "자가진단: ${pref.studentInfo}"
		
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
			Toast.makeText(this, "자동예약 취소", Toast.LENGTH_SHORT).show()
			true
		}
		
		submit.setOnClickListener {
			lifecycleScope.launch {
				updateTime()
				submitSuspend()
			}
		}
	}
	
	private fun checkNotice() = lifecycleScope.launch(Dispatchers.IO) {
		val versions =
			JSONObject(URL("https://raw.githubusercontent.com/wiki/lhwdev/covid-selftest-macro/_notice.md").readText())
		
		/*
		 * Spec:
		 * {"$version | all": {"id": "$id", "priority": "once | every", "title": "$title", "message": "$message"}}
		 *
		 * Version spec: 1.0 1.3..2.1 ..1.5
		 */
		
		val thisVersion = Version(BuildConfig.VERSION_NAME)
		
		for(key in versions.keys()) if(key == "all" || thisVersion in VersionSpec(key)) {
			val notice = Notice(versions.getJSONObject(key))
			val show = when(notice.priority) {
				Notice.Priority.once -> notice.id !in preferenceState.shownNotices
				Notice.Priority.every -> true
			}
			
			if(show) withContext(Dispatchers.Main) {
				AlertDialog.Builder(this@MainActivity).apply {
					setTitle(notice.title)
					val message = HtmlCompat.fromHtml(notice.message, 0)
					setMessage(message)
					setPositiveButton("확인", null)
				}.show().apply {
					findViewById<TextView>(android.R.id.message)!!.movementMethod =
						LinkMovementMethod.getInstance()
				}
				
				preferenceState.shownNotices += notice.id
			}
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
			&& !pwrm.isIgnoringBatteryOptimizations(name)) {
			AlertDialog.Builder(this).apply {
				setTitle("베터리 최적화 설정을 꺼야 알림 기능이 정상적으로 작동됩니다.")
				setPositiveButton("설정") { _, _ ->
					@SuppressLint("BatteryLife")
					val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
					intent.data = Uri.parse("package:$packageName")
					startActivityForResult(intent, IGNORE_BATTERY_OPTIMIZATION_REQUEST)
				}
				
				setNegativeButton("취소", null)
			}
			
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
