package com.lhwdev.selfTestMacro.repository

import android.text.format.DateUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.database.PreferenceHolder
import com.lhwdev.selfTestMacro.database.preferenceLong
import com.lhwdev.selfTestMacro.database.preferenceSerialized
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer


class NotificationStatus(holder: PreferenceHolder) {
	private var today by holder.preferenceSerialized(
		"today",
		serializer = SetSerializer(String.serializer()),
		defaultValue = emptySet()
	)
	
	private var dateEpoch by holder.preferenceLong("dateEpoch", defaultValue = 0L)
	
	
	private fun idFor(user: DbUser) = user.userCode
	
	fun submit(user: DbUser) {
		val id = idFor(user)
		
		if(DateUtils.isToday(dateEpoch)) {
			if(id !in today) {
				today = today + id
			}
		} else {
			today = setOf(id)
		}
	}
}
