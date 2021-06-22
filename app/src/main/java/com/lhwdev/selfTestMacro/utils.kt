@file:JvmName("AndroidUtils")

package com.lhwdev.selfTestMacro

import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.util.Base64
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnPreDraw
import androidx.core.view.setPadding
import com.google.android.material.snackbar.Snackbar
import com.lhwdev.selfTestMacro.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume


val sDummyForInitialization: Unit = run {
	// NO_WRAP: this is where I was confused for a few days
	encodeBase64 = { Base64.encodeToString(it, Base64.NO_WRAP) }
}


fun <K, V> Map<K, V>.added(key: K, value: V): Map<K, V> {
	val newMap = toMutableMap()
	newMap[key] = value
	return newMap
}

fun EditText.isEmpty() = text == null || text.isEmpty()

fun Int.toPx() = (this * Resources.getSystem().displayMetrics.density).toInt()

@Suppress("NOTHING_TO_INLINE")
inline fun Context.runOnUiThread(noinline action: () -> Unit) {
	Handler(mainLooper).post(action)
}

suspend fun Context.showToastSuspendAsync(message: String, isLong: Boolean = false) =
	withContext(Dispatchers.Main) {
		showToast(message, isLong)
	}

fun Context.showToast(message: String, isLong: Boolean = false) {
	Toast.makeText(this, message, if(isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun View.showSnackBar(message: String, duration: Int = 3000) {
	Snackbar.make(this, message, duration).show()
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

suspend fun Context.promptInput(block: AlertDialog.Builder.(edit: EditText, okay: () -> Unit) -> Unit): String? =
	promptDialog { onResult ->
		val view = EditText(context)
		val okay = { onResult(view.text.toString()) }
		view.setPadding(16.toPx())
		view.imeOptions = EditorInfo.IME_ACTION_DONE
		view.setOnEditorActionListener { _, _, _ ->
			okay()
			true
		}
		view.doOnPreDraw { view.requestFocus() }
		setView(view)
		setPositiveButton("확인") { _, _ ->
			okay()
		}
		setNegativeButton("취소", null)
		block(view, okay)
	}
