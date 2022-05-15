package com.lhwdev.selfTestMacro.api


@RequiresOptIn(message = "This is internal hcs api and using this is discouraged as it can change along with hcs update.")
public annotation class InternalHcsApi

@RequiresOptIn(message = "This is unstable hcs api and using this is discouraged as it can change along with hcs update.")
public annotation class UnstableHcsApi
