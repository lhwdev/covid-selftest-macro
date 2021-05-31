package com.lhwdev.selfTestMacro

import android.util.Log
import androidx.compose.material.SnackbarHostState


suspend fun onError(snackbarHostState: SnackbarHostState, message: String, throwable: Throwable) {
	Log.d("SelfTest-Macro", message, throwable)
	snackbarHostState.showSnackbar("오류: $message", "확인")
}
