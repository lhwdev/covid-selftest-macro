package com.lhwdev.selfTestMacro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider


class WizardIndexPreviewProvider : PreviewParameterProvider<Int> {
	override val values: Sequence<Int>
		get() = (0 until sSetupPagesCount).asSequence()
	override val count: Int get() = values.count()
}


@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "setup wizard")
@Composable
fun SetupPreview(@PreviewParameter(WizardIndexPreviewProvider::class) index: Int) {
	PreviewBase(statusBar = true) {
		val model = remember {
			SetupModel().apply {
				instituteInfo = InstitutionInfoModel.School()
			}
		}
		val parameters = SetupParameters.Default
		
		SetupWizardPage(model, parameters, SetupWizard(index, index, sSetupPagesCount) {})
	}
}

