package com.lhwdev.selfTestMacro.ui.pages.common

import androidx.annotation.DrawableRes
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.api.InstituteType
import com.lhwdev.selfTestMacro.database.AppDatabase
import com.lhwdev.selfTestMacro.database.DbTestTarget
import com.lhwdev.selfTestMacro.database.DbUser


@DrawableRes
fun AppDatabase.iconFor(group: DbTestTarget): Int = when(group) {
	is DbTestTarget.Group -> R.drawable.ic_group_24
	is DbTestTarget.Single -> iconFor(group.user)
}

@DrawableRes
fun iconFor(user: DbUser): Int = when(user.institute.type) {
	InstituteType.school -> R.drawable.ic_school_24
	InstituteType.university -> TODO()
	InstituteType.academy -> TODO()
	InstituteType.office -> TODO()
}
