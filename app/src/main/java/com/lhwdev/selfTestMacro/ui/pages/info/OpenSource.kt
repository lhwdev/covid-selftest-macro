package com.lhwdev.selfTestMacro.ui.pages.info

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.pushRoute
import com.lhwdev.selfTestMacro.openWebsite
import com.lhwdev.selfTestMacro.ui.*
import com.lhwdev.selfTestMacro.ui.common.BackButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json


@Serializable
data class LicenseItem(
	val groupId: String,
	val artifactId: String,
	val version: String,
	val spdxLicenses: List<SpdxLicense>? = null,
	val unknownLicenses: List<UnknownLicense>? = null,
	val scm: ScmItem
)

sealed interface OpenSourceLicense {
	val name: String
	val url: String
}

@Serializable
data class SpdxLicense(
	val identifier: String,
	override val name: String,
	override val url: String
) : OpenSourceLicense

@Serializable
data class UnknownLicense(
	override val name: String,
	override val url: String
) : OpenSourceLicense

@Serializable
data class ScmItem(val url: String)


@Composable
fun OpenSources() {
	val context = LocalContext.current
	val items by lazyState(defaultValue = null) {
		val input = context.resources.openRawResource(R.raw.open_source_license)
		withContext(Dispatchers.IO) {
			val text = input.reader().readText()
			Json.decodeFromString(ListSerializer(LicenseItem.serializer()), text)
		}
	}
	
	AutoSystemUi(
		navigationBarMode = OnScreenSystemUiMode.Immersive(scrimColor = ScrimNavLightColor)
	) { scrims ->
		Scaffold(
			topBar = {
				TopAppBar(
					title = { Text("오픈소스 라이센스") },
					statusBarScrim = scrims.statusBar
				)
			}
		) {
			if(items != null) Box(Modifier.fillMaxSize()) {
				OpenSourcesContent(items!!, scrims.navigationBarSpacer)
				
				Box(Modifier.align(Alignment.BottomCenter)) {
					scrims.navigationBar()
				}
			} else Column {
				Text(
					"로딩 중...",
					style = MaterialTheme.typography.h4,
					modifier = Modifier.wrapContentSize(align = Alignment.Center)
				)
				
				Box(Modifier.wrapContentSize(align = Alignment.BottomCenter)) {
					scrims.navigationBar()
				}
			}
		}
	}
}


@Composable
fun OpenSourcesContent(
	itemList: List<LicenseItem>,
	navigationBarSpacer: @Composable () -> Unit
) {
	val navigator = LocalNavigator
	
	val mapped = remember(itemList) {
		itemList.groupBy { it.groupId }.map { it }
	}
	
	val groupStyle = SpanStyle(MaterialTheme.colors.onBackground)
	val artifactStyle = SpanStyle(MaterialTheme.colors.primaryActive)
	val versionStyle = SpanStyle(MaterialTheme.colors.onBackground.copy(alpha = .9f))
	val otherStyle = SpanStyle(MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium))
	
	LazyColumn {
		items(items = mapped, key = { it.key }) { (groupId, items) ->
			val text = buildAnnotatedString {
				withStyle(groupStyle) { append(groupId) }
				
				if(items.size == 1) {
					val item = items[0]
					
					withStyle(otherStyle) { append(":") }
					withStyle(artifactStyle) { append(item.artifactId) }
					withStyle(otherStyle) { append(":") }
					withStyle(versionStyle) { append(item.version) }
				} else {
					withStyle(otherStyle) { append(":…") }
				}
			}
			
			
			ListItem(
				modifier = Modifier.clickable {
					navigator.pushRoute {
						if(items.size == 1) OpenSourcesDetail(items[0])
						else OpenSourcesList(groupId, items)
					}
				}
			) {
				Text(text)
			}
		}
		
		item {
			navigationBarSpacer()
		}
	}
}


@Composable
fun OpenSourcesList(groupId: String, items: List<LicenseItem>) {
	AutoSystemUi { scrims ->
		Scaffold(
			topBar = {
				TopAppBar(
					title = { Text(groupId) },
					statusBarScrim = scrims.statusBar
				)
			}
		) {
			val navigator = LocalNavigator
			
			val artifactStyle = SpanStyle(MaterialTheme.colors.primaryActive)
			val versionStyle = SpanStyle(MaterialTheme.colors.onBackground.copy(alpha = .9f))
			val otherStyle = SpanStyle(MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium))
			
			Column {
				LazyColumn(modifier = Modifier.weight(1f)) {
					items(items = items) { item ->
						val text = buildAnnotatedString {
							withStyle(artifactStyle) { append(item.artifactId) }
							withStyle(otherStyle) { append(":") }
							withStyle(versionStyle) { append(item.version) }
						}
						
						ListItem(
							modifier = Modifier.clickable {
								navigator.pushRoute { OpenSourcesDetail(item) }
							}
						) { Text(text) }
					}
				}
				
				scrims.navigationBar()
			}
		}
	}
}


@Composable
fun OpenSourcesDetail(item: LicenseItem) {
	val context = LocalContext.current
	val navigator = LocalNavigator
	
	AutoSystemUi { scrims ->
		Scaffold(
			topBar = {
				TopAppBar(
					navigationIcon = { BackButton { navigator.popRoute() } },
					title = { Text("오픈소스 정보") },
					statusBarScrim = scrims.statusBar,
					backgroundColor = Color.Transparent,
					elevation = 0.dp
				)
			}
		) {
			Column(Modifier.padding(vertical = 48.dp, horizontal = 32.dp)) {
				Text("${item.groupId}:${item.artifactId}", style = MaterialTheme.typography.h4)
				
				Spacer(Modifier.height(48.dp))
				
				ProvideTextStyle(MaterialTheme.typography.body1) {
					Text("버전: ${item.version}")
					
					val licenses = mutableListOf<OpenSourceLicense>().apply {
						item.spdxLicenses?.let { addAll(it) }
						item.unknownLicenses?.let { addAll(it) }
					}
					
					when {
						licenses.isEmpty() -> Text("라이센스 없음")
						licenses.size == 1 -> {
							val license = licenses[0]
							Row {
								Text("라이센스: ")
								LicenseItemText(license)
							}
						}
						else -> {
							Text("라이센스")
							
							Column(Modifier.padding(8.dp)) {
								for(license in licenses) LicenseItemText(license)
							}
						}
					}
					
					Spacer(Modifier.height(16.dp))
					
					Text(
						"저장소 보기",
						style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
						modifier = Modifier.clickable {
							context.openWebsite(item.scm.url)
						}
					)
				}
			}
		}
	}
}

@Composable
private fun LicenseItemText(license: OpenSourceLicense) {
	val context = LocalContext.current
	
	Text(
		license.name,
		style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
		modifier = Modifier.clickable {
			context.openWebsite(license.url)
		}
	)
}
