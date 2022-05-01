package com.lhwdev.selfTestMacro.ui.pages.setup

import androidx.activity.compose.BackHandler
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.navigation.Route
import com.lhwdev.selfTestMacro.ui.DefaultContentColor
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.common.SimpleIconButton
import com.lhwdev.selfTestMacro.ui.utils.RoundButton
import kotlinx.coroutines.launch


fun SetupRoute(parameters: SetupParameters): Route =
	Route("Setup") { Setup(parameters) }


@Composable
fun Setup(parameters: SetupParameters) {
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

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun SetupWizardView(model: SetupModel, parameters: SetupParameters) {
	val pagerState = rememberPagerState()
	val scope = rememberCoroutineScope()
	
	Scaffold(
		scaffoldState = model.scaffoldState,
		snackbarHost = {
			Box(Modifier.safeContentPadding().padding(bottom = 40.dp)) {
				SnackbarHost(it)
			}
		}
	) {
		HorizontalPager(count = sSetupPagesCount, state = pagerState, userScrollEnabled = false) { index ->
			val wizard = SetupWizard(index, pagerState.currentPage, sSetupPagesCount) {
				scope.launch { pagerState.animateScrollToPage(it) }
			}
			SetupWizardPage(model, parameters, wizard)
		}
	}
	
	BackHandler(enabled = pagerState.currentPage != 0) {
		scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
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
			if(wizard.index != 0) SimpleIconButton(
				icon = R.drawable.ic_arrow_left_24, contentDescription = "앞으로",
				onClick = onBefore
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
					pref.db.users.users.isEmpty() -> "완료"
					else -> "추가"
				}
				
				Text(text)
			}
		}
	}
}
