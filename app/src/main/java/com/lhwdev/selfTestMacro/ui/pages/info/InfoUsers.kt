package com.lhwdev.selfTestMacro.ui.pages.info

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lhwdev.fetch.fetch
import com.lhwdev.io.runInterruptibleGracefully
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.debug.LocalDebugContext
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.ui.common.LinkedText
import com.lhwdev.selfTestMacro.ui.utils.AutoSizeText
import com.lhwdev.selfTestMacro.ui.utils.IconOnlyTopAppBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable


// Such as things in https://github.com/lhwdev/covid-selftest-macro/blob/meta/src/info
object InfoUserStructure {
	@Serializable
	data class Root(val titles: List<List<String>>, val details: Map<String, Detail>)
	
	@Serializable
	data class Detail(
		val name: String,
		val profile: String? = null,
		val profileBottomPadding: Float? = null,
		val quote: String? = null,
		val credit: String,
		val links: List<Link>
	)
	
	@Serializable
	data class Link(val text: String, val url: String)
}


@Composable
fun InfoUsersDetail(detail: InfoUserStructure.Detail) {
	val navigator = LocalNavigator
	val urlHandler = LocalUriHandler.current
	val debug = LocalDebugContext.current
	
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
	) {
		// profile
		Box {
			if(detail.profile != null) {
				val image by produceState<ImageBitmap?>(null) {
					withContext(Dispatchers.Default) {
						value =
							try { // I know, this would be insane, but putting Glide or Coil is overkill for only this
								val response = fetch(detail.profile).rawResponse
								runInterruptibleGracefully {
									response.use {
										BitmapFactory.decodeStream(it)
									}.asImageBitmap()
								}
							} catch(th: Throwable) {
								if(th is CancellationException) throw th
								debug.onError("${detail.name}의 프로필 사진이 로딩되지 못했어요.", th)
								null
							}
					}
				}
				
				if(image != null) {
					Image(image!!, contentDescription = null, modifier = Modifier.fillMaxWidth())
				} else Box(Modifier.height(72.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
					Text("프사 로딩 중....")
				}
			} else {
				Spacer(Modifier.height(72.dp))
			}
			
			IconOnlyTopAppBar(
				navigationIcon = painterResource(R.drawable.ic_clear_24),
				contentDescription = "닫기",
				onClick = { navigator.popRoute() }
			)
		}
		
		
		Spacer(Modifier.height((detail.profileBottomPadding ?: 32f).dp))
		AutoSizeText(detail.name, style = MaterialTheme.typography.h4)
		Spacer(Modifier.height(16.dp))
		
		Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
			if(detail.quote != null) {
				Text(
					detail.quote,
					style = MaterialTheme.typography.h6,
					fontFamily = FontFamily.Serif,
					fontStyle = FontStyle.Italic,
					textAlign = TextAlign.Center
				)
				Spacer(Modifier.height(42.dp))
			} else {
				Spacer(Modifier.height(24.dp))
			}
			
			Text(detail.credit)
			Spacer(Modifier.height(24.dp))
			
			for(link in detail.links) LinkedText(link.text, onClick = {
				urlHandler.openUri(link.url)
			})
		}
	}
}
