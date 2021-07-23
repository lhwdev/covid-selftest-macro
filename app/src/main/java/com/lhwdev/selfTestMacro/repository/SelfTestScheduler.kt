package com.lhwdev.selfTestMacro.repository

import com.lhwdev.selfTestMacro.DatabaseManager


interface SelfTestScheduler {
    fun onScheduleUpdated(database: DatabaseManager)
}
