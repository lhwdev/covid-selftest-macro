package com.lhwdev.github.repo

import com.lhwdev.selfTestMacro.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL


private val Repository.releaseUrl get() = url["releases"]

@Serializable
data class Release(
	val id: Long,
	val name: String, @SerialName("tag_name") val tagName: String,
	val body: String,
	val assets: List<Asset>
)

@Serializable
data class Asset(
	val id: Long,
	val name: String, val label: String,
	@SerialName("browser_download_url")
	@Serializable(URLSerializer::class) val browserDownloadUrl: URL
)

suspend fun Repository.getReleaseLatest() = ioTask {
	fetch(releaseUrl["latest"]).toJsonLoose(Release.serializer())
}
