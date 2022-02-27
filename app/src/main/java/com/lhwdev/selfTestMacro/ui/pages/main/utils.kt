package com.lhwdev.selfTestMacro.ui.pages.main

import com.lhwdev.selfTestMacro.repository.SuspiciousKind


val SuspiciousKind?.displayName get() = this?.displayName ?: "정상"
