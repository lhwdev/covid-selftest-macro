package com.lhwdev.selfTestMacro.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import com.lhwdev.selfTestMacro.showToastSuspendAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


typealias ShowErrorInfo = (UiDebugContext, ErrorInfo, String) -> Unit


class UiDebugContext(
	manager: DebugManager,
	var context: Context,
	flags: DebugFlags,
	val uiScope: CoroutineScope,
	val showErrorInfo: ShowErrorInfo
) : DebugContext(flags = flags, manager = manager) {
	companion object {
		fun showErrorDialog(debugContext: UiDebugContext, errorInfo: ErrorInfo, description: String) {
			val context = debugContext.context
			AlertDialog.Builder(context).apply {
				setTitle("오류 발생")
				setMessage("* 복사된 오류정보는 기기의 정보 등 민감한 정보를 포함할 수 있어요.\n$description")
				
				setPositiveButton("오류정보 복사") { _, _ ->
					CoroutineScope(Dispatchers.Main).launch {
						context.getSystemService<ClipboardManager>()!!
							.setPrimaryClip(ClipData.newPlainText("오류정보", description))
						context.showToastSuspendAsync("복사 완료")
					}
				}
				setNegativeButton("취소", null)
			}.show()
		}
	}
	
	
	
	override var contextName: String = ""
	
	override suspend fun onShowErrorInfo(info: ErrorInfo, description: String) = withContext(uiScope.coroutineContext) {
		showErrorInfo(this@UiDebugContext, info, description)
	}
}
