package com.lhwdev.github

import java.net.URL


data class GithubInstance(val url: URL, val webUrl: URL)


val sGithubInstanceDefault = GithubInstance(
	url = URL("https://api.github.com"),
	webUrl = URL("https://github.com")
)
