package com.mato.syai.imagegenerator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import java.net.NetworkInterface
import java.net.Inet4Address

@SpringBootApplication
@ConfigurationPropertiesScan
class ImageGeneratorApplication

fun main(args: Array<String>) {
    runApplication<ImageGeneratorApplication>(*args)
    printLocalIPs()
}

private fun printLocalIPs() {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return
        println("==========================================================================")
        println("  SYAI AI ORCHESTRATION SERVER ACTIVE - NETWORK INTERFACES:")
        var count = 0
        while (interfaces.hasMoreElements()) {
            val netInterface = interfaces.nextElement()
            if (netInterface.isLoopback || !netInterface.isUp) continue
            val addresses = netInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (addr is Inet4Address) {
                    val ip = addr.hostAddress
                    println("  -> Interface: ${netInterface.displayName} | URL: http://$ip:8088/")
                    count++
                }
            }
        }
        println()
        println("  DEVELOPER GUIDANCE FOR ANDROID CONNECTION:")
        println("  If your physical Android device is connected to the same local Wi-Fi,")
        println("  please ensure your Android project's 'local.properties' has:")
        println("  LOCAL_IMAGE_GENERATOR_BASE_URL = http://<YOUR_CHOSEN_INTERFACE_IP>:8088/")
        println("==========================================================================")
    } catch (e: Exception) {
        println("Failed to display network interfaces: ${e.message}")
    }
}
