package com.lhwdev.selfTestMacro

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import java.io.File


abstract class BaseActivity : AppCompatActivity() {
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		
		if(requestCode == 1001) {
			println("$resultCode $data")
			println("deleting update apk")
			File(getExternalFilesDir(null), sUpdateApkExternalPath).delete()
		}
	}
}
