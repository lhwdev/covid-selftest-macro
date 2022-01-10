package com.lhwdev.selfTestMacro.repository

import android.content.Context
import com.lhwdev.selfTestMacro.database.PreferenceHolder
import com.lhwdev.selfTestMacro.database.preferenceState
import java.io.File


class NotificationStatus(val logFile: File, val pref: PreferenceHolder) {
	constructor(context: Context) : this(
		logFile = logFile,
		pref = context.preferenceState.pref
	)
}
