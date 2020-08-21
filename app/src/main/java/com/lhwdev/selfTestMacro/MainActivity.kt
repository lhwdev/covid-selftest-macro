package com.lhwdev.selfTestMacro

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.fab
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.content_main.submit
import kotlinx.android.synthetic.main.content_main.time
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


fun SharedPreferences.preferenceInt(key: String, defaultValue: Int) =
	object : ReadWriteProperty<Any?, Int> {
		override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
			edit { putInt(key, value) }
		}
		
		override fun getValue(thisRef: Any?, property: KProperty<*>): Int =
			getInt(key, defaultValue)
	}


@Suppress("SpellCheckingInspection")
class MainActivity : AppCompatActivity() {
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		setSupportActionBar(toolbar)
		
		val pref = getSharedPreferences("main", Context.MODE_PRIVATE)
		var hour by pref.preferenceInt("hour", -1)
		var min by pref.preferenceInt("min", 0)
		
		val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
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
		
		fab.setOnClickListener { view ->
			Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
				.setAction("Action", null).show()
		}
		
		fun updateTime() {
			time.text = if(hour == -1) "시간 예약 안 됨" else "$hour 시 $min 분"
			if(hour != -1) {
				alarmManager.cancel(intent)
				scheduleNextAlarm(intent, hour, min)
//				val serv = service
//				if(serv == null) {
//					serviceIntent.putExtra("hour", hour)
//					serviceIntent.putExtra("min", min)
//					startService(serviceIntent)
//					bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
//					return
//				}
			}
		}
		
		updateTime()
		
		time.setOnClickListener {
			TimePickerDialog(this, { _, newHour, newMin ->
				hour = newHour
				min = newMin
				updateTime()
			}, if(hour == -1) 0 else hour, min, false).show()
		}
		
		time.setOnLongClickListener {
			hour = -1
			Toast.makeText(this, "자동예약 취소", Toast.LENGTH_SHORT).show()
			true
		}
		
		submit.setOnClickListener {
			updateTime()
			doSubmit()
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
			R.id.action_settings -> true
			else -> super.onOptionsItemSelected(item)
		}
	}
}
