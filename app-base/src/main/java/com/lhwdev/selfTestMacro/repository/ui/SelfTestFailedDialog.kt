package com.lhwdev.selfTestMacro.repository.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.repository.SubmitResult
import com.lhwdev.selfTestMacro.ui.Color
import com.vanpra.composematerialdialogs.*


fun Navigator.showSelfTestFailedDialog(results: List<SubmitResult>, terminated: Boolean) = showDialogAsync {
	Title { Text("자가진단 실패") }
	
	ListContent {
		for(result in results) when(result) {
			is SubmitResult.Success -> ListItem {
				Text(
					"${result.target.name}: 성공",
					color = Color(
						onLight = Color(0xf4259644),
						onDark = Color(0xff99ffa0)
					)
				)
			}
			
			is SubmitResult.Failed -> ListItem(
				modifier = Modifier.clickable {
					showDialogAsync {
						Title { Text("오류 발생") }
						
						ListContent {
							ListItem { Text("대상: ${result.target.name} (${result.target.institute.name})") }
							ListItem { Text("오류 이유: ${result.causes.joinToString { it.description }}") }
							
							Spacer(Modifier.height(8.dp))
							
							for(cause in result.causes) ListItem(
								text = { Text(cause.description) },
								secondaryText = cause.detail?.let { { Text(it) } },
								singleLineSecondaryText = false
							)
							
							Spacer(Modifier.height(8.dp))
							
							ListItem(
								modifier = Modifier.clickable { showDiagnosticDialog(result.diagnostic) }
							) { Text("자세한 진단정보 보기") }
						}
						
						Buttons {
							PositiveButton(onClick = requestClose) { Text("닫기") }
						}
					}
				}
			) {
				Text(
					"${result.target.name}: 실패",
					color = Color(
						onLight = Color(0xffff1122),
						onDark = Color(0xffff9099)
					)
				)
			}
		}
		
		if(terminated) ListItem {
			Text("(자가진단 중단됨)")
		}
	}
	
	Buttons {
		PositiveButton(onClick = requestClose) { Text("확인") }
	}
}

