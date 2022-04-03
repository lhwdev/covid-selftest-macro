package com.lhwdev.selfTestMacro.ui.pages.setup

import androidx.activity.compose.BackHandler
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.navigation.Route
import com.lhwdev.selfTestMacro.ui.DefaultContentColor
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.common.SimpleIconButton
import com.lhwdev.selfTestMacro.ui.utils.RoundButton
import kotlinx.coroutines.launch
import kotlin.math.max


fun SetupRoute(parameters: SetupParameters = SetupParameters.Default): Route =
	Route("Setup") { Setup(parameters) }


@Composable
fun Setup(parameters: SetupParameters = SetupParameters.Default) {
	Surface(color = MaterialTheme.colors.surface) {
		val model = remember { SetupModel() }
		SetupWizardView(model, parameters)
	}
}

@Immutable
internal data class SetupWizard(
	val index: Int,
	val currentIndex: Int,
	val count: Int,
	val scrollTo: (index: Int) -> Unit
)

internal val SetupWizard.isCurrent get() = currentIndex == index

internal const val sSetupPagesCount = 4

@Composable
private fun SetupWizardView(model: SetupModel, parameters: SetupParameters) {
	var pageIndex by remember { mutableStateOf(0) }
	
	Scaffold(
		scaffoldState = model.scaffoldState,
		snackbarHost = {
			Box(Modifier.safeContentPadding().padding(bottom = 40.dp)) {
				SnackbarHost(it)
			}
		}
	) {
		WizardPager(pageIndex = pageIndex) { index ->
			val wizard = SetupWizard(index, pageIndex, sSetupPagesCount) {
				pageIndex = it
			}
			SetupWizardPage(model, parameters, wizard)
		}
	}
	
	BackHandler(enabled = pageIndex != 0) {
		pageIndex--
	}
}

@Composable
@VisibleForTesting
internal fun SetupWizardPage(model: SetupModel, parameters: SetupParameters, wizard: SetupWizard) {
	when(wizard.index) {
		0 -> WizardSelectType(model, parameters, wizard)
		1 -> WizardInstituteInfo(model.instituteInfo ?: return, model, parameters, wizard)
		2 -> WizardUserInfo(model, parameters, wizard)
		3 -> WizardSelectUsers(model, parameters, wizard)
		else -> error("unknown page")
	}
}


private const val sPreloadPages = 0

@Composable
private fun WizardPager(
	pageIndex: Int,
	content: @Composable (index: Int) -> Unit
) {
	var maxLoads by remember { mutableStateOf(1) }
	
	BoxWithConstraints(Modifier.clipToBounds()) {
		val scope = rememberCoroutineScope()
		val width = maxWidth
		val widthPx = with(LocalDensity.current) { width.roundToPx() }
		
		var targetPage by remember { mutableStateOf(pageIndex) }
		val scrollState = remember { ScrollState(pageIndex) }
		
		fun scrollTo(target: Int) {
			if(target !in 0 until sSetupPagesCount) return
			targetPage = target
			scope.launch {
				scrollState.animateScrollTo(target * widthPx)
			}
		}
		
		if(pageIndex != targetPage) {
			maxLoads = max(maxLoads, pageIndex + 1)
			scrollTo(pageIndex)
		}
		
		Row(
			modifier = Modifier.horizontalScroll(
				scrollState,
				enabled = false
			)
		) {
			for(index in 0 until sSetupPagesCount) {
				Box(Modifier.requiredWidth(width)) {
					if(index < maxLoads +
						if(scrollState.isScrollInProgress) sPreloadPages else 0
					) content(index)
				}
			}
		}
	}
}


internal fun SetupWizard.before() {
	scrollTo(index - 1)
}

internal fun SetupWizard.next() {
	scrollTo(index + 1)
}


@Composable
internal fun WizardCommon(
	wizard: SetupWizard,
	wizardFulfilled: Boolean,
	showNotFulfilledWarning: () -> Unit,
	modifier: Modifier = Modifier,
	extra: @Composable (() -> Unit)? = null,
	onNext: () -> Unit = { wizard.next() },
	onBefore: () -> Unit = { wizard.before() },
	showNext: Boolean = true,
	content: @Composable () -> Unit
) {
	Column(modifier) {
		Box(Modifier.weight(1f, fill = true)) {
			content()
		}
		
		Row(verticalAlignment = Alignment.CenterVertically) {
			SimpleIconButton(
				icon = R.drawable.ic_arrow_left_24, contentDescription = "앞으로",
				onClick = onBefore,
				modifier = if(wizard.index == 0) Modifier.alpha(0f) else Modifier
			)
			
			if(extra != null) {
				Spacer(Modifier.width(8.dp))
				extra()
			}
			
			Spacer(Modifier.weight(100f))
			
			
			if(showNext) RoundButton(
				onClick = {
					if(wizardFulfilled) onNext() else showNotFulfilledWarning()
				},
				trailingIcon = {
					val contentColor = DefaultContentColor
					Icon(
						painterResource(
							id = if(wizard.index != wizard.count - 1) R.drawable.ic_arrow_right_24
							else R.drawable.ic_check_24
						),
						contentDescription = null,
						tint = if(wizardFulfilled) contentColor else contentColor.copy(alpha = 0.8f)
					)
				}
			) {
				val pref = LocalPreference.current
				
				val text = when {
					wizard.index != wizard.count - 1 -> "다음"
					pref.isFirstTime -> "완료"
					else -> "추가"
				}
				
				Text(text)
			}
		}
	}
}
