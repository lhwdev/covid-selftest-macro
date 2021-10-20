package com.lhwdev.selfTestMacro.repository

import java.net.InetAddress


fun pingNetwork(): Boolean = try {
	InetAddress.getByName("8.8.8.8") // Google Public DNS
		.isReachable(1500)
} catch(th: Throwable) {
	false
}
