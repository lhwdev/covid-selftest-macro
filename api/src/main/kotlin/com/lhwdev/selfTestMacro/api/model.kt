package com.lhwdev.selfTestMacro.api


public interface HcsModel


/*
 * Most models have a concept of 'persistent' and 'temporary'.
 * All models which is temporary should be only used for limited period, and cannot be serialized.
 * This ensures that one do not put temporary model into database.
 */

public interface HcsPersistentModel : HcsModel

public interface HcsTemporaryModel : HcsModel {
	public suspend fun update(): HcsTemporaryModel
}


public interface HcsLiveModel : HcsModel {
	public suspend fun refresh()
}
