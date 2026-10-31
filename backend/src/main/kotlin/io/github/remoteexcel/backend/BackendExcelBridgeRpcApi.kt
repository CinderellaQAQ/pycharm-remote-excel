@file:Suppress("UnstableApiUsage")

package io.github.remoteexcel.backend

import com.intellij.ide.vfs.VirtualFileId
import com.intellij.ide.vfs.virtualFile
import io.github.remoteexcel.shared.DownloadResult
import io.github.remoteexcel.shared.ExcelBridgeRpcApi

internal class BackendExcelBridgeRpcApi : ExcelBridgeRpcApi {
    override suspend fun download(fileId: VirtualFileId): DownloadResult {
        val file = fileId.virtualFile()
            ?: return DownloadResult(ok = false, message = "The remote file is no longer available.")

        if (!file.isValid || file.isDirectory) {
            return DownloadResult(ok = false, message = "The selected item is not a readable file.")
        }
        if (!ExcelFiles.supports(file.extension)) {
            return DownloadResult(ok = false, message = "The selected file is not a supported workbook.")
        }
        if (file.length > MAX_FILE_SIZE_BYTES) {
            return DownloadResult(
                ok = false,
                message = "The workbook is larger than the 2 GB safety limit.",
            )
        }

        return try {
            DownloadResult(
                ok = true,
                remoteKey = file.url,
                fileName = file.name,
                content = file.contentsToByteArray(),
            )
        } catch (error: Exception) {
            DownloadResult(ok = false, message = error.message ?: "Unable to read the remote workbook.")
        }
    }

    private companion object {
        const val MAX_FILE_SIZE_BYTES = 2_000_000_000L
    }
}

private object ExcelFiles {
    private val extensions = setOf("xls", "xlsx", "xlsm", "xlsb", "xlt", "xltx", "xltm", "ods")

    fun supports(extension: String?): Boolean = extension?.lowercase() in extensions
}
