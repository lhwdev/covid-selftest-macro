package com.lhwdev.selfTestMacro.api

import kotlinx.serialization.Serializable


@Serializable
public class InstituteData(
	public val identifier: String,
	public val name: String,
	public val address: String
) : HcsPersistentModel
