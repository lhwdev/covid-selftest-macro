package com.lhwdev.github.repo

import com.lhwdev.fetch.FetchResult


interface ContentSource {
	suspend fun getContent(
		path: String,
		accept: GithubContentType = GithubContentType.raw
	): FetchResult
}


enum class GithubContentType(val contentType: String) {
	raw("application/vnd.github.VERSION.raw"),
	html("application/vnd.github.VERSION.html")
}
