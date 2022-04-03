package com.lhwdev.selfTestMacro.ui.pages.info

import androidx.annotation.RawRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
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
import com.lhwdev.selfTestMacro.ui.common.SimpleIconButton
import com.lhwdev.selfTestMacro.ui.systemUi.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.systemUi.OnScreenSystemUiMode
import com.lhwdev.selfTestMacro.ui.systemUi.ScrimNavSurfaceColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream


@Serializable
class LicenseItem(
	val groupId: String,
	val artifactId: String,
	val version: String,
	val spdxLicenses: List<OpenSourceLicense.SpdxLicense>? = null,
	val unknownLicenses: List<OpenSourceLicense.UnknownLicense>? = null,
	val scm: OpenSourceLicense.ScmItem
)

sealed interface OpenSourceLicense {
	val name: String
	val url: String
	
	@Serializable
	class SpdxLicense(
		val identifier: String,
		override val name: String,
		override val url: String
	) : OpenSourceLicense
	
	@Serializable
	class UnknownLicense(
		override val name: String,
		override val url: String
	) : OpenSourceLicense
	
	@Serializable
	class ScmItem(val url: String)
}

@Serializable
class OpenSourceExtra(val comment: String)


@OptIn(ExperimentalSerializationApi::class)
@Composable
private fun <T> jsonRawResource(
	@RawRes id: Int,
	serializer: DeserializationStrategy<T>
): T? {
	val context = LocalContext.current
	return lazyState(defaultValue = null) {
		val input = context.resources.openRawResource(id)
		withContext(Dispatchers.IO) {
			input.use { Json.decodeFromStream(serializer, it) }
		}
	}.value
}


@Composable
fun OpenSources() {
	val context = LocalContext.current
	val navigator = LocalNavigator
	
	val items = jsonRawResource(
		R.raw.open_source_license,
		ListSerializer(LicenseItem.serializer())
	)
	val extras = jsonRawResource(
		R.raw.open_source_extra,
		MapSerializer(String.serializer(), OpenSourceExtra.serializer())
	)
	
	AutoSystemUi(
		navigationBars = OnScreenSystemUiMode.Immersive(ScrimNavSurfaceColor)
	) { scrims ->
		Scaffold(
			topBar = {
				com.lhwdev.selfTestMacro.ui.systemUi.TopAppBar(
					title = { Text("오픈소스 라이센스") },
					navigationIcon = {
						SimpleIconButton(
							icon = R.drawable.ic_arrow_back_24,
							contentDescription = "뒤로 가기",
							onClick = { navigator.popRoute() })
					},
					statusBarScrim = scrims.statusBars
				)
			}
		) {
			if(items != null && extras != null) Box(Modifier.fillMaxSize()) {
				OpenSourcesContent(items, extras, scrims.navigationBarsSpacer)
				
				Box(Modifier.align(Alignment.BottomCenter)) {
					scrims.navigationBars()
				}
			} else Column {
				ListItem { Text("로딩 중...", color = DisabledContentColor) }
				
				Box(Modifier.wrapContentSize(align = Alignment.BottomCenter)) {
					scrims.navigationBars()
				}
			}
		}
	}
}


@Composable
fun OpenSourcesContent(
	itemList: List<LicenseItem>,
	extras: Map<String, OpenSourceExtra>,
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
						else OpenSourcesList(groupId, items, extras[groupId])
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
fun OpenSourcesList(groupId: String, items: List<LicenseItem>, extra: OpenSourceExtra?) {
	val navigator = LocalNavigator
	
	AutoSystemUi { scrims ->
		Scaffold(
			topBar = {
				com.lhwdev.selfTestMacro.ui.systemUi.TopAppBar(
					title = { Text(groupId) },
					navigationIcon = {
						SimpleIconButton(
							icon = R.drawable.ic_arrow_back_24,
							contentDescription = "뒤로 가기",
							onClick = { navigator.popRoute() })
					},
					statusBarScrim = scrims.statusBars
				)
			}
		) {
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
					
					if(extra != null) item {
						Spacer(Modifier.height(30.dp))
						Text(extra.comment, modifier = Modifier.padding(16.dp))
					}
				}
				
				scrims.navigationBars()
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
				com.lhwdev.selfTestMacro.ui.systemUi.TopAppBar(
					navigationIcon = {
						SimpleIconButton(
							icon = R.drawable.ic_arrow_back_24, contentDescription = "뒤로 가기",
							onClick = { navigator.popRoute() }
						)
					},
					title = { Text("오픈소스 정보") },
					statusBarScrim = scrims.statusBars,
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
