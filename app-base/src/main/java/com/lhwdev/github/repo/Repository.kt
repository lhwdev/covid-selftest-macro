package com.lhwdev.github.repo

import com.lhwdev.fetch.get
import com.lhwdev.github.GithubInstance
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.net.URL


@Serializable
data class Repository(val instance: GithubInstance, val owner: String, val name: String) {
	@Transient
	val url = instance.url["repos/$owner/$name"]
	val webUrl: URL get() = instance.webUrl["$owner/$name"]
}
