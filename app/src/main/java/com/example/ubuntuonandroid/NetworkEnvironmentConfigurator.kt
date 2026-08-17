package com.example.ubuntuonandroid

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import java.io.File

object NetworkEnvironmentConfigurator {

    private const val TAG = "NetworkEnvConfig"
    private const val OFFICIAL_MIRROR = "https://ports.ubuntu.com/ubuntu-ports/"
    private const val TENCENT_MIRROR = "https://mirrors.tencent.com/ubuntu-ports/"
    private const val CONFIG_VERSION = "3"

    fun installDefaults(context: Context) {
        val rootfs = EnvironmentPaths.rootfsDir(context)
        writeAptSources(rootfs, TENCENT_MIRROR)
        writeMirrorHelper(rootfs)
        writeVersionMarker(rootfs)
        updateDns(context)
    }

    fun migrateLegacyConfiguration(context: Context) {
        val rootfs = EnvironmentPaths.rootfsDir(context)
        if (!File(rootfs, "bin/bash").isFile) return

        writeMirrorHelper(rootfs)
        val marker = File(rootfs, "etc/ubuntuonandroid-network-version")
        if (marker.readTextOrNull()?.trim() != CONFIG_VERSION) {
            val sources = File(rootfs, "etc/apt/sources.list")
            val current = sources.readTextOrNull().orEmpty()
            if (shouldUseDefaultTencentMirror(current)) {
                writeAptSources(rootfs, TENCENT_MIRROR)
            }
            writeVersionMarker(rootfs)
        }
        updateDns(context)
    }

    fun updateDns(context: Context): Boolean {
        val rootfs = EnvironmentPaths.rootfsDir(context)
        if (!File(rootfs, "bin/bash").isFile) return false

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val dnsServers = connectivityManager.activeNetwork
            ?.let(connectivityManager::getLinkProperties)
            ?.dnsServers
            .orEmpty()
            .mapNotNull { it.hostAddress }
            .distinct()

        return try {
            val resolvConf = File(rootfs, "etc/resolv.conf")
            resolvConf.parentFile?.mkdirs()
            resolvConf.writeText(renderResolvConf(dnsServers))
            dnsServers.isNotEmpty()
        } catch (error: Exception) {
            Log.w(TAG, "Unable to update resolv.conf", error)
            false
        }
    }

    internal fun renderResolvConf(dnsServers: List<String>): String = buildString {
        appendLine("# Managed by UbuntuOnAndroid from Android's active network.")
        if (dnsServers.isEmpty()) {
            appendLine("# No DNS server is currently available.")
        } else {
            dnsServers.forEach { appendLine("nameserver $it") }
        }
    }

    internal fun shouldUseDefaultTencentMirror(currentSources: String): Boolean =
        currentSources.isBlank() || currentSources.contains("ports.ubuntu.com/ubuntu-ports")

    private fun writeAptSources(rootfs: File, mirror: String) {
        val sources = File(rootfs, "etc/apt/sources.list")
        sources.parentFile?.mkdirs()
        sources.writeText(
            listOf(
                "deb $mirror jammy main restricted universe multiverse",
                "deb $mirror jammy-updates main restricted universe multiverse",
                "deb $mirror jammy-backports main restricted universe multiverse",
                "deb $mirror jammy-security main restricted universe multiverse",
            ).joinToString(separator = "\n", postfix = "\n")
        )
    }

    private fun writeMirrorHelper(rootfs: File) {
        val helper = File(rootfs, "usr/local/bin/ubuntuonandroid-set-mirror")
        helper.parentFile?.mkdirs()
        helper.writeText(
            """
            #!/bin/bash
            set -eu

            case "${'$'}{1:-}" in
                official) mirror="$OFFICIAL_MIRROR" ;;
                tencent) mirror="$TENCENT_MIRROR" ;;
                *)
                    echo "Usage: ubuntuonandroid-set-mirror {official|tencent}" >&2
                    exit 2
                    ;;
            esac

            cat > /etc/apt/sources.list <<EOF
            deb ${'$'}mirror jammy main restricted universe multiverse
            deb ${'$'}mirror jammy-updates main restricted universe multiverse
            deb ${'$'}mirror jammy-backports main restricted universe multiverse
            deb ${'$'}mirror jammy-security main restricted universe multiverse
            EOF

            echo "APT mirror changed to ${'$'}mirror"
            echo "Run: apt update"
            """.trimIndent() + "\n"
        )
        helper.setExecutable(true, false)
    }

    private fun writeVersionMarker(rootfs: File) {
        val marker = File(rootfs, "etc/ubuntuonandroid-network-version")
        marker.parentFile?.mkdirs()
        marker.writeText("$CONFIG_VERSION\n")
    }

    private fun File.readTextOrNull(): String? =
        try {
            if (isFile) readText() else null
        } catch (_: Exception) {
            null
        }
}
