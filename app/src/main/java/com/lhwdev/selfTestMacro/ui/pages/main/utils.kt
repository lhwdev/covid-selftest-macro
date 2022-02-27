package com.lhwdev.selfTestMacro.ui.pages.main

import com.lhwdev.selfTestMacro.repository.SuspiciousKind


val SuspiciousKind?.displayText get() = this?.displayText ?: "정상"
