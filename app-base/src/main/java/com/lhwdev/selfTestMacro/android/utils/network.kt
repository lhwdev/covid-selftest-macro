package com.lhwdev.selfTestMacro.android.utils

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import androidx.annotation.RequiresApi


abstract class NetworkCommon {
	abstract val name: String?
	
	abstract val isAvailable: Boolean
	
	abstract val isVpn: Boolean
	
	abstract val extraInfo: String?
	
	fun copy(): NetworkCommonImpl = NetworkCommonImpl(
		name = name, isVpn = isVpn, isAvailable = isAvailable, extraInfo = extraInfo
	)
	
	override fun toString(): String =
		"NetworkCommon(name=$name, isVpn=$isVpn, isAvailable=$isAvailable, extraInfo=($extraInfo))"
}

class NetworkCommonImpl(
	override val name: String?,
	override val isVpn: Boolean,
	override val isAvailable: Boolean,
	override val extraInfo: String?
) : NetworkCommon()


@RequiresApi(21)
private class NetworkCommonApi21(val manager: ConnectivityManager, val network: Network) : NetworkCommon() {
	private val capability = manager.getNetworkCapabilities(network)
	
	override val name: String?
		get() = manager.getLinkProperties(network)?.interfaceName
	
	override val isAvailable: Boolean
		get() = capability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
	
	override val isVpn: Boolean
		get() = capability?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
	
	override val extraInfo: String?
		get() = manager.getLinkProperties(network)?.run {
			"domains=$domains, httpProxy=($httpProxy), isPrivateDnsActive=${if(Build.VERSION.SDK_INT >= 28) isPrivateDnsActive else "(api < 28)"}"
		}
}

// Our minSdkVersion is 21 so don't need this, but see activeNetworkCommon. activeNetwork requires api >= 23.
@Suppress("DEPRECATION")
private class NetworkCommonOlder(val info: NetworkInfo) : NetworkCommon() {
	override val name: String?
		get() = null
	
	override val isAvailable: Boolean
		get() = info.isConnected
	
	override val isVpn: Boolean
		get() = info.type == ConnectivityManager.TYPE_VPN
	
	override val extraInfo: String
		get() = "(extraInfo=${info.extraInfo}, reason=${info.reason} detailedState=${info.detailedState})"
}


val ConnectivityManager.activeNetworkCommon: NetworkCommon?
	get() = if(Build.VERSION.SDK_INT >= 23) {
		activeNetwork?.let { NetworkCommonApi21(this, it) }
	} else @Suppress("DEPRECATION") {
		activeNetworkInfo?.let { NetworkCommonOlder(it) }
	}
