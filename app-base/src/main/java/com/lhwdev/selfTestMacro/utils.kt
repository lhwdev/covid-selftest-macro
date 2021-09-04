@file:JvmName("AndroidUtils")

package com.lhwdev.selfTestMacro

import android.content.Context
import android.content.res.Resources
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume


inline fun <R> tryAtMost(maxTrial: Int, onError: (th: Throwable) -> Unit = {}, block: () -> R): R {
	var trialCount = 0
	while(true) {
		try {
			return block()
		} catch(th: Throwable) {
			trialCount++
			if(trialCount >= maxTrial) throw th
			onError(th)
		}
	}
}

fun <K, V> Map<K, V>.added(key: K, value: V): Map<K, V> {
	val newMap = toMutableMap()
	newMap[key] = value
	return newMap
}

fun <T> List<T>.replaced(from: T, to: T): List<T> = map { if(it == from) to else it }

fun Int.toPx() = (this * Resources.getSystem().displayMetrics.density).toInt()

suspend fun Context.showToastSuspendAsync(message: String, isLong: Boolean = false) =
	withContext(Dispatchers.Main) {
		showToast(message, isLong)
	}

fun Context.showToast(message: String, isLong: Boolean = false) {
	Toast.makeText(this, message, if(isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

suspend fun <R : Any> Context.promptDialog(
	block: AlertDialog.Builder.(onResult: (R) -> Unit) -> Unit
): R? = withContext(Dispatchers.Main) {
	suspendCancellableCoroutine { cont ->
		var invoked = false
		fun onResult(result: R?) {
			if(!invoked) {
				invoked = true
				cont.resume(result)
			}
		}
		
		var dialog: AlertDialog? = null
		dialog = AlertDialog.Builder(this@promptDialog).apply {
			block {
				onResult(it)
				dialog?.dismiss()
			}
			
			setOnDismissListener {
				onResult(null)
			}
			
			setOnCancelListener {
				onResult(null)
			}
			
		}.show()
		cont.invokeOnCancellation { dialog.dismiss() }
	}
}

