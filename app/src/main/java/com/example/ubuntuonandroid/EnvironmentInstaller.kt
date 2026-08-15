package com.example.ubuntuonandroid

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object EnvironmentInstaller {

    private const val TAG = "EnvInstaller"

    fun isInstalled(context: Context): Boolean {
        val script = File(context.filesDir, "start-ubuntu.sh")
        return script.exists()
    }

    fun install(context: Context, onProgress: (String) -> Unit) {
        try {
            val filesDir = context.filesDir
            val prootBinary = File(context.applicationInfo.nativeLibraryDir, "libproot.so").absolutePath

            // 1. Extract Rootfs
            val rootfsDir = File(filesDir, "ubuntu-fs")
            if (!rootfsDir.exists()) {
                rootfsDir.mkdirs()
            }
            
            onProgress("Copying Ubuntu rootfs tarball...")
            val tarballFile = File(filesDir, "ubuntu-rootfs.tar")
            copyAsset(context, "ubuntu-rootfs.tar", tarballFile)

            onProgress("Extracting Ubuntu rootfs (This may take a few minutes)...")
            // Use Android's native tar. It will fail on chown/hardlinks but extract successfully.
            val process = ProcessBuilder(
                "/system/bin/tar", "-xf", tarballFile.absolutePath, "-C", rootfsDir.absolutePath
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            // Since Android's tar cannot create hard links or chown, it will exit with an error code.
            // We verify success by checking if a core file was extracted.
            if (!File(rootfsDir, "bin/bash").exists()) {
                Log.e(TAG, "Tar extraction failed: $output")
                onProgress("Extraction failed! Check logs.")
                return
            }

            // Cleanup tarball
            tarballFile.delete()

            // 2. Create start script
            onProgress("Creating startup scripts...")
            createStartScript(context, prootBinary)
            
            // 3. Set up DNS
            val resolvConf = File(rootfsDir, "etc/resolv.conf")
            resolvConf.parentFile?.mkdirs()
            resolvConf.writeText("nameserver 8.8.8.8\nnameserver 1.1.1.1\n")

            onProgress("Installation Complete!")
        } catch (e: Exception) {
            Log.e(TAG, "Installation error", e)
            onProgress("Error: ${e.message}")
        }
    }

    private fun createStartScript(context: Context, prootBinary: String) {
        val script = File(context.filesDir, "start-ubuntu.sh")
        val rootfs = File(context.filesDir, "ubuntu-fs").absolutePath
        
        val content = """
            #!/system/bin/sh
            export PROOT_TMP_DIR=${context.filesDir.absolutePath}/tmp
            mkdir -p ${context.filesDir.absolutePath}/tmp
            
            export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
            export HOME=/root
            export TERM=xterm-256color
            
            exec $prootBinary --link2symlink -0 -r $rootfs -b /dev -b /proc -b /sys -w /root /bin/bash -l
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
