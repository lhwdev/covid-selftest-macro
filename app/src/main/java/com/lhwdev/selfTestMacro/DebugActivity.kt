package com.lhwdev.selfTestMacro

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.getSystemService


class DebugActivity : Activity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		val clipboard = getSystemService<ClipboardManager>()!!
		val error = intent.getStringExtra("error")
		AlertDialog.Builder(this).setTitle("오류 발생").setMessage(error).setPositiveButton("오류 내용 복사") { _, _ ->
			clipboard.setPrimaryClip(ClipData.newPlainText("오류 로그", error))
			Toast.makeText(this, "오류 로그를 복사했습니다.", Toast.LENGTH_LONG).show()
		}.show()
	}
}
