package com.lhwdev.github

import java.net.URL


data class GithubInstance(val url: URL)


val sGithubInstanceDefault = GithubInstance(URL("https://api.github.com"))
