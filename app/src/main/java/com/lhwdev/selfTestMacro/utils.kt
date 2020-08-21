package com.lhwdev.selfTestMacro

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.widget.Toast
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlin.concurrent.thread


fun Context.createIntent() = PendingIntent.getBroadcast(
	this, AlarmReceiver.REQUEST_CODE, Intent(this, AlarmReceiver::class.java),
	PendingIntent.FLAG_UPDATE_CURRENT)


@SuppressLint("NewApi")
fun Context.scheduleNextAlarm(intent: PendingIntent, hour: Int, min: Int, nextDay: Boolean = false) {
	(getSystemService(Context.ALARM_SERVICE) as AlarmManager).setExact(AlarmManager.RTC_WAKEUP, Calendar.getInstance().run {
		val new = clone() as Calendar
		new[Calendar.HOUR_OF_DAY] = hour
		new[Calendar.MINUTE] = min
		new[Calendar.SECOND] = 0
		new[Calendar.MILLISECOND] = 0
		if(nextDay || new <= this) new.add(Calendar.DAY_OF_YEAR, 1)
		Log.e("HOI", "$this")
		Log.e("HOI", "$new")
		new.timeInMillis
	}, intent)
}

@Suppress("NOTHING_TO_INLINE")
inline fun Context.runOnUiThread(noinline action: () -> Unit) {
	Handler(mainLooper).post(action)
}

fun Context.doSubmit() {
	thread thread@{
		
		// 1. check
		val checkRequest = URL("https://eduro.dge.go.kr/stv_cvd_co01_000.do")
			.openConnection() as HttpURLConnection
		checkRequest.requestMethod = "POST"
		DataOutputStream(checkRequest.outputStream).use {
			// 자가진단 사이트 소스코드 뜯어보면 제출 버튼 눌렀을 때 보내는 urlencode된 ajax 데이터를 넣으면 됩니다.
			it.writeBytes("rtnRsltCode=SUCCESS&qstnCrtfcNoEncpt=<여기>&schulNm=&stdntName=&rspns01=1&rspns02=1&rspns07=0&rspns08=0&rspns09=0")
			it.flush()
		}
//				val message = checkRequest.responseCode.toString() + ":" + checkRequest.inputStream.reader().readText()
//				/*if(checkRequest.responseCode == 200)*/ runOnUiThread {
//					AlertDialog.Builder(this).apply {
//						setMessage(message)
//					}.show()
//				}
		
		fun showError(num: Int) {
			runOnUiThread {
				Toast.makeText(this, "failed $num", Toast.LENGTH_LONG).show()
			}
		}
		
		if(checkRequest.responseCode != 200) {
			showError(1)
			return@thread
		}
		
		val response = JSONObject(checkRequest.inputStream.reader().readText())
		val resultSVO = response.getJSONObject("resultSVO")
		
		if(resultSVO.getString("rtnRsltCode") != "SUCCESS") {
			showError(2)
			return@thread
		}
		
		val schulNm = resultSVO.getString("schulNm")
		val stdntName = resultSVO.getString("stdntName")
		Log.i("HOI", "$schulNm $stdntName")
		
		// 2. send form
		val request = URL("https://eduro.dge.go.kr/stv_cvd_co02_000.do")
			.openConnection() as HttpURLConnection
		request.requestMethod = "POST"
		request.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
		request.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8")
		HttpURLConnection.setFollowRedirects(true)
		DataOutputStream(request.outputStream).use {
			// 이건 form.submit이 실행될 때
			it.writeBytes("rtnRsltCode=SUCCESS&qstnCrtfcNoEncpt=<여기>&schulNm=&stdntName=&rspns01=1&rspns01=2&rspns02=1&rspns03=1&rspns05=1&rspns13=1&rspns14=1&rspns15=1&rspns04=1&rspns11=1&rspns07=0&rspns07=1&rspns08=0&rspns08=1&rspns09=0&rspns09=1")
			it.flush()
		}
		Log.i("HOI", "${request.responseMessage} / ${request.inputStream.reader().readText()}")
		
		runOnUiThread {
			Toast.makeText(this, "자가진단 완료", Toast.LENGTH_SHORT).show()
		}
		
		// log
		File(getExternalFilesDir(null)!!, "log.txt").appendText("self-tested at ${DateFormat.getDateTimeInstance().format(Date())} ${request.responseMessage}\n")
	}
	
}
