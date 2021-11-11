package com.lhwdev.selfTestMacro.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import com.lhwdev.selfTestMacro.showToastSuspendAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


val LocalDebugManager: ProvidableCompositionLocal<DebugManager> =
	compositionLocalOf { error("Not provided") }

val LocalDebugContext: ProvidableCompositionLocal<ComposeDebugContext> =
	compositionLocalOf { error("Not provided") }


@Composable
fun rememberDebugContext(
	flags: DebugContext.DebugFlags,
	manager: DebugManager = LocalDebugManager.current,
	showErrorInfo: ShowErrorInfo
): ComposeDebugContext {
	val androidContext = LocalContext.current
	val uiScope = rememberCoroutineScope()
	val workScope = remember { CoroutineScope(Dispatchers.Default) }
	
	val context = remember {
		ComposeDebugContext(
			context = androidContext,
			flags = flags,
			uiScope = uiScope,
			manager = manager,
			showErrorInfo = showErrorInfo
		)
	}
	context.context = androidContext
	return context
}


typealias ShowErrorInfo = (ComposeDebugContext, ErrorInfo, String) -> Unit


class ComposeDebugContext(
	manager: DebugManager,
	var context: Context,
	flags: DebugFlags,
	override val uiScope: CoroutineScope,
	val showErrorInfo: ShowErrorInfo
) : DebugContext(flags = flags, manager = manager) {
	companion object {
		fun ShowErrorDialog(debugContext: ComposeDebugContext, errorInfo: ErrorInfo, description: String) {
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
	
	override suspend fun onShowErrorInfo(info: ErrorInfo, description: String) {
		showErrorInfo(this, info, description)
	}
}
