package com.lhwdev.selfTestMacro.api.impl

import com.lhwdev.selfTestMacro.api.UserData
import com.lhwdev.selfTestMacro.api.UserModel


public class User(public val data: UserData) : UserModel by data
