package com.example.ubuntuonandroid

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object EnvironmentInstaller {

    private const val TAG = "EnvInstaller"

    fun isInstalled(context: Context): Boolean {
        return try {
            EnvironmentPaths.migrateLegacyInstall(context)
            val script = EnvironmentPaths.startScript(context)
            val bash = File(EnvironmentPaths.rootfsDir(context), "bin/bash")
            if (bash.isFile && !script.isFile) {
                ensureStartScript(context)
            }
            script.exists() && bash.exists()
        } catch (error: Exception) {
            Log.e(TAG, "Unable to inspect the installed environment", error)
            false
        }
    }

    fun install(context: Context, onProgress: (String) -> Unit) {
        try {
            EnvironmentPaths.migrateLegacyInstall(context)
            val baseDir = EnvironmentPaths.baseDir(context)
            val prootBinary = File(context.applicationInfo.nativeLibraryDir, "libproot.so").absolutePath

            // 1. Extract Rootfs
            val rootfsDir = EnvironmentPaths.rootfsDir(context)
            if (!rootfsDir.exists()) {
                rootfsDir.mkdirs()
            }
            
            val assetNames = context.assets.list("") ?: emptyArray()
            val assetName = if (assetNames.contains("ubuntu-rootfs.tar.gz")) {
                "ubuntu-rootfs.tar.gz"
            } else if (assetNames.contains("ubuntu-rootfs.tar")) {
                "ubuntu-rootfs.tar"
            } else {
                "ubuntu-rootfs.tar.gz"
            }

            onProgress("Copying Ubuntu rootfs tarball ($assetName)...")
            val tarballFile = File(baseDir, assetName)
            copyAsset(context, assetName, tarballFile)

            val tarFile = if (assetName.endsWith(".gz")) {
                onProgress("Decompressing gzip archive...")
                val decompressed = File(baseDir, "ubuntu-rootfs.tar")
                java.util.zip.GZIPInputStream(tarballFile.inputStream().buffered()).use { gzIn ->
                    decompressed.outputStream().buffered().use { tarOut ->
                        gzIn.copyTo(tarOut)
                    }
                }
                tarballFile.delete()
                decompressed
            } else {
                tarballFile
            }

            onProgress("Extracting Ubuntu rootfs (This may take a few minutes)...")
            val process = ProcessBuilder(
                "/system/bin/tar", "-xf", tarFile.absolutePath, "-C", rootfsDir.absolutePath
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            tarFile.delete()

            // Since Android's tar cannot create hard links or chown, it will exit with an error code.
            // We verify success by checking if a core file was extracted.
            if (!File(rootfsDir, "bin/bash").exists()) {
                Log.e(TAG, "Tar extraction failed: $output")
                onProgress("Extraction failed! Check logs.")
                return
            }

            // 2. Create start script
            onProgress("Creating startup scripts...")
            createStartScript(context, prootBinary)

            // 3. Set up network defaults without embedding a fixed DNS provider.
            NetworkEnvironmentConfigurator.installDefaults(context)

            onProgress("Installation Complete!")
        } catch (e: Exception) {
            Log.e(TAG, "Installation error", e)
            onProgress("Error: ${e.message}")
        }
    }

    fun ensureStartScript(context: Context) {
        EnvironmentPaths.migrateLegacyInstall(context)
        val prootBinary = File(context.applicationInfo.nativeLibraryDir, "libproot.so").absolutePath
        createStartScript(context, prootBinary)
        NetworkEnvironmentConfigurator.migrateLegacyConfiguration(context)
    }

    fun createStartScript(context: Context, prootBinary: String) {
        val script = EnvironmentPaths.startScript(context)
        val rootfs = EnvironmentPaths.rootfsDir(context).absolutePath
        val tmpDir = EnvironmentPaths.tmpDir(context).absolutePath
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val prootLoader = File(nativeLibDir, "libproot_loader.so").absolutePath
        
        val content = """
            #!/system/bin/sh
            export PATH=/system/bin:/system/xbin
            export HOME=/root
            export TERM=xterm-256color
            export PROOT_TMP_DIR=$tmpDir
            export PROOT_LOADER=$prootLoader
            export LD_LIBRARY_PATH="$nativeLibDir"
            
            mkdir -p "$tmpDir"
            mkdir -p "$rootfs/tmp"
            mkdir -p "$rootfs/run/sshd"
            chmod 755 "$rootfs/run/sshd" 2>/dev/null || true
            
            PROOT_BIN="$prootBinary"
            if [ ! -f "${'$'}PROOT_BIN" ]; then
                if [ -f "$nativeLibDir/libproot.so" ]; then
                    PROOT_BIN="$nativeLibDir/libproot.so"
                fi
            fi
            
            if [ ${'$'}# -gt 0 ]; then
                exec "${'$'}PROOT_BIN" --link2symlink -0 -r "$rootfs" -b /dev -b /proc -b /sys -w /root /bin/bash "${'$'}@"
            else
                exec "${'$'}PROOT_BIN" --link2symlink -0 -r "$rootfs" -b /dev -b /proc -b /sys -w /root /bin/bash -l
            fi
        """.trimIndent()
        
        script.writeText(content)
        script.setExecutable(true)
    }

    private fun copyAsset(context: Context, assetName: String, outFile: File) {
        context.assets.open(assetName).use { input ->
            FileOutputStream(outFile).use { output ->
                val buffer = ByteArray(1024 * 64)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
            }
        }
    }
}
