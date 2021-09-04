package com.lhwdev.selfTestMacro

import android.content.Context
import android.content.Intent
import android.net.Uri


fun Context.openWebsite(url: String) {
	val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
	if(intent.resolveActivity(packageManager) != null) {
		startActivity(intent)
	} else {
		showToast("설치된 웹 브라우저가 없어요.")
	}
}
