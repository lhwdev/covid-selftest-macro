@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro

import com.lhwdev.selfTestMacro.api.JsonLoose
import kotlinx.serialization.KSerializer


fun <T> FetchResult.toJsonLoose(serializer: KSerializer<T>) = toJson(serializer, JsonLoose)
