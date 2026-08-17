package com.example.ubuntuonandroid

import android.content.Context
import java.io.File

object EnvironmentPaths {

    private const val ROOTFS_DIR = "ubuntu-fs"
    private const val START_SCRIPT = "start-ubuntu.sh"
    private const val TMP_DIR = "tmp"

    fun baseDir(context: Context): File = context.noBackupFilesDir.apply { mkdirs() }

    fun rootfsDir(context: Context): File = File(baseDir(context), ROOTFS_DIR)

    fun startScript(context: Context): File = File(baseDir(context), START_SCRIPT)

    fun tmpDir(context: Context): File = File(baseDir(context), TMP_DIR)

    @Synchronized
    fun migrateLegacyInstall(context: Context) {
        val destination = baseDir(context)
        migrateDirectory(
            File(context.filesDir, ROOTFS_DIR),
            File(destination, ROOTFS_DIR),
            failOnConflict = true,
        )
        migrateDirectory(
            File(context.filesDir, TMP_DIR),
            File(destination, TMP_DIR),
            failOnConflict = false,
        )

        // The script embeds absolute paths, so it is regenerated at the new location.
        File(context.filesDir, START_SCRIPT).delete()

        if (File(destination, ROOTFS_DIR).isDirectory) {
            File(context.filesDir, "ubuntu-rootfs.tar").delete()
            File(context.filesDir, "ubuntu-rootfs.tar.gz").delete()
        }
    }

    private fun migrateDirectory(source: File, destination: File, failOnConflict: Boolean) {
        if (!source.exists()) return
        check(!failOnConflict || !destination.exists()) {
            "Both legacy and no-backup Ubuntu environments exist; migration stopped to protect user data"
        }
        if (destination.exists()) return
        destination.parentFile?.mkdirs()
        check(source.renameTo(destination)) {
            "Unable to migrate ${source.absolutePath} to the no-backup directory"
        }
    }
}
