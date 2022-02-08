package com.lhwdev.github.repo

import com.lhwdev.fetch.FetchResult
import java.net.URL


interface ContentSource {
	fun contentOf(path: String): Content
}

interface Content {
	val url: URL
	val webUrl: URL
	
	suspend fun get(accept: GithubContentType = GithubContentType.raw): FetchResult
}


enum class GithubContentType(val contentType: String) {
	raw("application/vnd.github.VERSION.raw"),
	html("application/vnd.github.VERSION.html")
}
