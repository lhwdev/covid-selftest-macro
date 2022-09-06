package com.lhwdev.build

import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.ComposePlugin


val ComposePlugin.Dependencies.foundationLayout
	get() = "org.jetbrains.compose.foundation:foundation:${ComposeBuildConfig.composeVersion}"
