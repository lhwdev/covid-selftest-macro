package com.lhwdev.selfTestMacro

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.content_main.submit
import kotlinx.android.synthetic.main.content_main.time
import kotlinx.coroutines.launch


@Suppress("SpellCheckingInspection")
class MainActivity : AppCompatActivity() {
	
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
