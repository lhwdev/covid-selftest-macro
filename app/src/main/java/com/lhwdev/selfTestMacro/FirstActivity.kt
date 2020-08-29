package com.lhwdev.selfTestMacro

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.activity_first.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.URL


class FirstActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_first)
		window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
		val pref = PreferenceState(prefMain())
		val first = intent.hasExtra("first")
		preferenceState = pref
		
		setSupportActionBar(toolbar)
		
		input_url.setText(pref.siteString ?: "https://eduro.dge.go.kr")
		
		spinner_method.adapter = ArrayAdapter(
			this,
			android.R.layout.simple_list_item_1,
			listOf("내부 인증코드(11자리 이상)", "기타")
		)
		
		var result: String? = null
		
		spinner_method.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(
				parent: AdapterView<*>,
				view: View,
				position: Int,
				id: Long
			) {
				blank_method.removeAllViews()
				when(position) {
					0 -> {
						val edit =
							layoutInflater.inflate(R.layout.layout_method_certcode, blank_method)
						val editText = edit.findViewById<TextInputEditText>(R.id.input_method)
						editText.setText(pref.cert ?: "")
						editText.addTextChangedListener(object : TextWatcher {
							override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
							override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
							
							override fun afterTextChanged(s: Editable) {
								val text = editText.text!!.toString()
								result = text
								button_done.isEnabled = text.length >= 11
							}
						})
					}
					1 -> {
						val dialog = Dialog(this@FirstActivity)
						val web = WebView(this@FirstActivity)
						dialog.setContentView(web)
						@SuppressLint("SetJavaScriptEnabled")
						web.settings.javaScriptEnabled = true
						web.webViewClient = object : WebViewClient() {
							override fun onPageFinished(view: WebView, url: String) {
								super.onPageFinished(view, url)
								if(url.endsWith("/stv_cvd_co00_000.do")) // typed information
									web.evaluateJavascript(
										"""
										$("#qstnCrtfcNoEncpt").val()
									"""
									) {
										dialog.dismiss()
										result = it.removePrefix("\"").removeSuffix("\"")
										button_done.isEnabled = true
									}
							}
						}
						dialog.show()
						dialog.window!!.setLayout(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.MATCH_PARENT
						)
						web.loadUrl(input_url.text!!.toString() + "/hcheck/index.jsp")
					}
				}
			}
			
			override fun onNothingSelected(parent: AdapterView<*>) {
			}
		}
		
		button_done.setOnClickListener {
			if(result == null) {
				Toast.makeText(this, "학생정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
			} else lifecycleScope.launch {
				Log.i("HOI", "cert: $result")
				
				val site = input_url.text!!.toString()
				
				val studentInfo = try {
					checkStudentInfoSuspend(URL(site), result!!)
				} catch(e: IOException) {
					showToastSuspendAsync("잘못된 학생 정보입니다.")
					Log.e("HOI", "retrieving student info returned with error", e)
					return@launch
				}
				
				pref.siteString = site
				pref.cert = result
				pref.studentInfo = studentInfo
				
				if(first) {
					startActivity(Intent(this@FirstActivity, MainActivity::class.java).also {
						it.putExtra("doneFirst", true)
					})
					finish()
				}
			}
		}
		
		if(first) button_done.isEnabled = false
	}
}