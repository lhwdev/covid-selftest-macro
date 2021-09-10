package com.lhwdev.github.repo

import com.lhwdev.fetch.FetchResult
import com.lhwdev.fetch.fetch
import com.lhwdev.fetch.get


suspend fun Repository.getRawContent(path: String, branch: String?): FetchResult {
	val realPath = "contents/$path"
	return fetch(
		url = if(branch == null) url[realPath] else url[realPath, "ref" to branch],
		headers = mapOf("Accept" to "application/vnd.github.VERSION.raw")
	)
}
