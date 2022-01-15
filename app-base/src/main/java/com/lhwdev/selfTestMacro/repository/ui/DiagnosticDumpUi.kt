package com.lhwdev.selfTestMacro.repository.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.debug.*
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.ui.utils.AnimateHeight
import com.vanpra.composematerialdialogs.Title
import com.vanpra.composematerialdialogs.showDialogAsync


fun Navigator.showDiagnosticDialog(obj: DiagnosticObject) = showDialogAsync {
	val item = remember { obj.getDiagnosticInformation() }
	Title { Text(item.localizedName ?: item.name) }
	
	DiagnosticItemView(item, root = true)
}


@Composable
private fun DiagnosticItemView(item: DiagnosticItem, root: Boolean = false) {
	
	when(item) {
		is DiagnosticElement<*> -> DiagnosticElementView(item)
		is DiagnosticItemGroup -> DiagnosticItemGroupView(item, root = root)
	}
}

@Composable
private fun DiagnosticElementView(element: DiagnosticElement<*>) {
	ListItem {
		Text(buildAnnotatedString {
			withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
				append(element.name)
			}
			
			append(": ")
			append(element.localizedData())
		})
	}
}


@Composable
private fun DiagnosticItemGroupView(group: DiagnosticItemGroup, root: Boolean) {
	var expanded by remember { mutableStateOf(root) }
	
	Column {
		ListItem(
			text = { Text(group.localizedName ?: group.name) },
			modifier = Modifier.clickable { expanded = !expanded }
		)
		
		AnimateHeight(visible = expanded, modifier = if(root) Modifier else Modifier.padding(start = 8.dp)) {
			Column {
				for(child in group.children) DiagnosticItemView(child)
			}
		}
	}
}
