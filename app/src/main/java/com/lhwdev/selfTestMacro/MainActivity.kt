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
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.lhwdev.selfTestMacro.api.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import java.util.Calendar


const val IGNORE_BATTERY_OPTIMIZATION_REQUEST = 1001
private val defaultQuickTestInfo = QuickTestInfo(
	days = emptySet(),
	behavior = QuickTestInfo.Behavior.negative
)


@Suppress("SpellCheckingInspection")
class MainActivity : AppCompatActivity() {
	private var batteryOptimizationPromptShown = false
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		val session = selfTestSession(this)
		
		val pref = preferenceState
		
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
		suspend fun updateCurrentState() = withContext(Dispatchers.IO) main@{
			val institute = pref.institute!!
			val user = pref.user!! // note: may change
			
			val detailedUserInfo = try {
				val usersIdentifier = user.findUser(session)
				val usersToken = (session.validatePassword(
					institute,
					usersIdentifier,
					user.password
				) as PasswordResult.Success).token
				val users = session.getUserGroup(institute, usersToken)
				session.getUserInfo(institute, singleOfUserGroup(users)!!)
			} catch(e: Throwable) {
				onError(e, "사용자 정보 불러오기")
				showToastSuspendAsync("사용자 정보를 불러오지 못했습니다.")
				return@main
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
		
		fun update() {
			val isSchedulingEnabled = switch_enable.isChecked
			pref.isSchedulingEnabled = isSchedulingEnabled
			if(isSchedulingEnabled) {
				@SuppressLint("SetTextI18n")
				time.text = "매일 자가진단: ${pref.hour}시 ${pref.min}분"
			} else {
				time.text = "시간 예약 안 됨"
			}
			updateTime(intent)
		}
		
		fun pickTime() {
			TimePickerDialog(this, { _, newHour, newMin ->
				checkBatteryOptimizationPermission()
				
				pref.hour = newHour
				pref.min = newMin
				update()
			}, if(pref.hour == -1) 0 else pref.hour, pref.min, false).show()
		}
		
		switch_enable.isChecked = pref.isSchedulingEnabled
		update()
		
		switch_enable.setOnCheckedChangeListener { _, _ ->
			if(pref.hour == -1) {
				pickTime()
			} else {
				update()
			}
		}
		
		switch_random.isChecked = pref.isRandomEnabled
		
		switch_random.setOnCheckedChangeListener { _, isChecked ->
			pref.isRandomEnabled = isChecked
			update()
		}
		
		
		switch_weekend.isChecked = pref.includeWeekend
		
		switch_weekend.setOnCheckedChangeListener { _, isChecked ->
			pref.includeWeekend = isChecked
			updateTime(intent)
		}
		
		
		// switch_isolation.isChecked = pref.isIsolated
		//
		// switch_isolation.setOnCheckedChangeListener { _, isChecked ->
		// 	if(isChecked) {
		// 		AlertDialog.Builder(this).apply {
		// 			setTitle("자가격리자 옵션")
		// 			setMessage(
		// 				"""
		// 				|가정에서 자가격리를 하고 있으신 분은 이 옵션을 선택해주세요. 즉, 자가진단 설문에서 3번 문항에 '예'를 답해야 하는 경우입니다.
		// 				|(참고) 해당 문항:
		// 				|3. 학생 본인 또는 동거인이 방역당국에 의해 현재 자가격리가 이루어지고 있나요?
		// 				|※ 동거인이 자가격리중인 경우, ① 매 등교 희망일로부터 2일 이내 진단검사 결과가 음성인 경우 또는 ② 격리 통지를 받은 ‘즉시’ 자가격리된 동거인과 접촉이 없었던 경우는 ‘아니오’ 선택
		// 			""".trimMargin()
		// 			)
		// 			setPositiveButton("설정") { _, _ ->
		// 				pref.isIsolated = true
		// 			}
		// 			setNegativeButton("취소") { _, _ ->
		// 				switch_isolation.isChecked = false
		// 			}
		// 		}.show()
		// 	} else {
		// 		pref.isIsolated = false
		// 	}
		// }
		
		
		time.setOnClickListener {
			pickTime()
		}
		
		time.setOnLongClickListener {
			switch_enable.isChecked = false
			update()
			Toast.makeText(this, "자동예약 취소", Toast.LENGTH_SHORT).show()
			true
		}
		
		var quickTest = pref.quickTest ?: defaultQuickTestInfo
		var quickTestEnabled = pref.quickTest != null
		
		quick_test_enable.isChecked = quickTestEnabled
		quick_test_result.isVisible = quickTestEnabled
		quick_test_enable.setOnCheckedChangeListener { _, isChecked ->
			quick_test_result.isVisible = isChecked
			quickTestEnabled = isChecked
			
			pref.quickTest = if(isChecked) {
				quickTest
			} else {
				null
			}
		}
		
		fun quickTestDay(button: Button, day: Int) {
			fun up(enabled: Boolean) {
				val color = if(enabled) {
					0x55229cff
				} else {
					0x00000000
				}
				button.setBackgroundColor(color)
			}
			up(day in quickTest.days)
			button.setOnClickListener {
				val lastEnabled = day in quickTest.days
				
				val days = if(lastEnabled) {
					quickTest.days - day
				} else {
					quickTest.days + day
				}
				quickTest = quickTest.copy(days = days)
				pref.quickTest = quickTest
				up(!lastEnabled)
				updateTime(intent)
			}
		}
		
		quickTestDay(monday, Calendar.MONDAY)
		quickTestDay(tuesday, Calendar.TUESDAY)
		quickTestDay(wednesday, Calendar.WEDNESDAY)
		quickTestDay(thursday, Calendar.THURSDAY)
		quickTestDay(friday, Calendar.FRIDAY)
		quickTestDay(saturday, Calendar.SATURDAY)
		quickTestDay(sunday, Calendar.SUNDAY)
		
		quick_test_result_behavior.check(
			when(quickTest.behavior) {
				QuickTestInfo.Behavior.negative -> R.id.result_negative
				QuickTestInfo.Behavior.doNotSubmit -> R.id.disable_macro
			}
		)
		
		quick_test_result_behavior.setOnCheckedChangeListener { _, checkedId ->
			val behavior = when(checkedId) {
				R.id.result_negative -> QuickTestInfo.Behavior.negative
				R.id.disable_macro -> QuickTestInfo.Behavior.doNotSubmit
				else -> error("oh no")
			}
			quickTest = quickTest.copy(behavior = behavior)
			pref.quickTest = quickTest
			updateTime(intent)
		}
		
		submit.setOnClickListener {
			lifecycleScope.launch {
				submitSuspend(session, false, manual = true)
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
		enum class Priority { once, everyWithDoNotShowAgain, every }
	}
	
	private fun checkNotice() = lifecycleScope.launch(Dispatchers.IO) {
		val content: String?
		try {
			content =
				URL("https://raw.githubusercontent.com/wiki/lhwdev/covid-selftest-macro/notice_v4.json").readText()
			
			val notificationObject: NotificationObject = Json {
				ignoreUnknownKeys = true /* loose */
			}.decodeFromString(NotificationObject.serializer(), content)
			
			if(notificationObject.notificationVersion != 4) {
				// incapable of displaying this
				return@launch
			}
			
			Log.d("hOI", notificationObject.toString())
			
			val currentVersion = Version(BuildConfig.VERSION_NAME)
			
			for(entry in notificationObject.entries) {
				var show = when(entry.priority) {
					NotificationEntry.Priority.once -> entry.id !in preferenceState.shownNotices
					NotificationEntry.Priority.everyWithDoNotShowAgain -> entry.id !in preferenceState.doNotShowAgainNotices
					NotificationEntry.Priority.every -> true
				}
				show = show && (entry.version?.let { currentVersion in it } ?: true)
				
				if(show) withContext(Dispatchers.Main) {
					AlertDialog.Builder(this@MainActivity).apply {
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
						findViewById<TextView>(android.R.id.message)!!.movementMethod =
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
		if(isDebugEnabled) menu.add(Menu.NONE, R.id.share_log, Menu.NONE, "로그 공유하기")
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
			R.id.share_log -> {
				AlertDialog.Builder(this).apply {
					setTitle("로그 공유하기")
					setItems(arrayOf("자가진단 기록", "오류 로그")) { _, index ->
						val file = when(index) {
							0 -> sSelfLog
							1 -> sErrorLog
							else -> error("unknown index")
						}
						
						AlertDialog.Builder(this@MainActivity).apply {
							setTitle("개인정보 안내")
							setMessage("이 로그를 공유하면 로그 안에 포함된 개인정보를 로그를 공유하는 대상에게 공개하는 것을 동의하게 됩니다.")
							setPositiveButton("예") { _, _ -> shareErrorLog(file) }
							setNegativeButton("취소", null)
						}.show()
					}
					setNegativeButton("취소", null)
				}.show()
				true
			}
			R.id.info -> {
				val context = this
				AlertDialog.Builder(context).apply {
					setTitle("자가진단 매크로 ${BuildConfig.VERSION_NAME}")
					setMessage(
						HtmlCompat.fromHtml(
							"""
								|이현우 개발<br>
								|<a href='https://github.com/lhwdev/covid-selftest-macro'>자가진단 앱 웹사이트</a><br><br>
								|버그 제보 방법: 개발자 모드 (밑의 버튼) > 체크 박스 누르기 > 버그가 생길 때까지 기다리기 > ...에서 '로그 공유하기'
								|(안뜨면 앱 나갔다 들어오기) > 공유하면 됨
							""".trimMargin(),
							0
						)
					)
					setPositiveButton("ㅇㅋ", null)
					setNeutralButton("개발자 모드") { _, _ ->
						AlertDialog.Builder(context).apply {
							setTitle("개발자 도구")
							val view = LinearLayout(context)
							view.orientation = LinearLayout.VERTICAL
							val checkbox = CheckBox(context)
							checkbox.isChecked = isDebugEnabled
							checkbox.setOnCheckedChangeListener { _, checked ->
								preferenceState.isDebugEnabled = checked
							}
							view.addView(checkbox)
							view.setPadding(
								TypedValue.applyDimension(
									TypedValue.COMPLEX_UNIT_DIP,
									8f,
									resources.displayMetrics
								).toInt()
							)
							setView(view)
							setNegativeButton("닫기", null)
						}.show()
					}
				}.show().apply {
					findViewById<TextView>(android.R.id.message)!!.movementMethod =
						LinkMovementMethod.getInstance()
				}
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}
}
