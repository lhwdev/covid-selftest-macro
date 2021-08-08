@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro

import com.lhwdev.selfTestMacro.api.JsonLoose
import kotlinx.serialization.KSerializer


public suspend fun <T> FetchResult.toJsonLoose(serializer: KSerializer<T>): T = toJson(serializer, JsonLoose)
