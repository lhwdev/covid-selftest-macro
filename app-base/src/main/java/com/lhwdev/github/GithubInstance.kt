package com.lhwdev.github

import com.lhwdev.fetch.URLSerializer
import kotlinx.serialization.Serializable
import java.net.URL


@Serializable
data class GithubInstance(
	@Serializable(with = URLSerializer::class)
	val url: URL,
	@Serializable(with = URLSerializer::class)
	val webUrl: URL
)


val sGithubInstanceDefault = GithubInstance(
	url = URL("https://api.github.com"),
	webUrl = URL("https://github.com")
)
