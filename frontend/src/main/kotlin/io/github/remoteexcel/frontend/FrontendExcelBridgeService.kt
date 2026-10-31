@file:Suppress("UnstableApiUsage")

package io.github.remoteexcel.frontend

import com.intellij.ide.vfs.VirtualFileId
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import io.github.remoteexcel.shared.DownloadResult
import io.github.remoteexcel.shared.ExcelBridgeRpcApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

@Service(Service.Level.PROJECT)
internal class FrontendExcelBridgeService(
    private val project: Project,
    private val coroutineScope: CoroutineScope,
) {
    fun open(fileId: VirtualFileId) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val api = ExcelBridgeRpcApi.getInstance()
                val downloaded = api.download(fileId)
                if (!downloaded.ok) {
                    notify(downloaded.message, NotificationType.ERROR)
                    return@launch
                }

                val localPath = createLocalSnapshot(downloaded)
                openWithDesktop(localPath)
                notify(
                    "Opened a local copy of ${downloaded.fileName}. Local changes will not affect the remote file.",
                    NotificationType.INFORMATION,
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                notify(error.message ?: "Unable to open the remote workbook.", NotificationType.ERROR)
            }
        }
    }

    private fun createLocalSnapshot(downloaded: DownloadResult): Path {
        val workbookCacheRoot = Path.of(
            PathManager.getSystemPath(),
            "remote-excel-bridge",
            project.locationHash,
            sha256(downloaded.remoteKey).take(20),
        )
        Files.createDirectories(workbookCacheRoot)
        val snapshotDirectory = Files.createTempDirectory(workbookCacheRoot, "snapshot-")

        val safeName = downloaded.fileName
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .ifBlank { "workbook.xlsx" }
        val localPath = snapshotDirectory.resolve(safeName)
        Files.write(
            localPath,
            downloaded.content,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE,
        )

        return localPath
    }

    private fun openWithDesktop(path: Path) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            error("The local JetBrains Client cannot launch the operating system's default application.")
        }
        Desktop.getDesktop().open(path.toFile())
    }

    private fun notify(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(content, type)
            .notify(project)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val NOTIFICATION_GROUP_ID = "Remote Excel Bridge"
    }
}
