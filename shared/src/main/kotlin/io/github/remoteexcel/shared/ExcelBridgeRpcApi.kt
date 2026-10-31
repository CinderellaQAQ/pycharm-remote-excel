@file:Suppress("UnstableApiUsage")

package io.github.remoteexcel.shared

import com.intellij.ide.vfs.VirtualFileId
import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import kotlinx.serialization.Serializable

@Serializable
data class DownloadResult(
    val ok: Boolean,
    val message: String = "",
    val remoteKey: String = "",
    val fileName: String = "",
    val content: ByteArray = byteArrayOf(),
)

@Rpc
interface ExcelBridgeRpcApi : RemoteApi<Unit> {
    companion object {
        suspend fun getInstance(): ExcelBridgeRpcApi =
            RemoteApiProviderService.resolve(remoteApiDescriptor<ExcelBridgeRpcApi>())
    }

    suspend fun download(fileId: VirtualFileId): DownloadResult
}
