@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json


val jsonLooseConfig = Json { ignoreUnknownKeys = true }


fun <T> FetchResult.toJsonLoose(serializer: KSerializer<T>) = toJson(serializer, jsonLooseConfig)
