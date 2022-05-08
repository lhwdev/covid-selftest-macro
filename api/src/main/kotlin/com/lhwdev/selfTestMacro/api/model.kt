package com.lhwdev.selfTestMacro.api


public interface HcsModel


public interface HcsPersistentModel : HcsModel

public interface HcsTemporaryModel : HcsModel {
	public suspend fun update(): HcsTemporaryModel
}
