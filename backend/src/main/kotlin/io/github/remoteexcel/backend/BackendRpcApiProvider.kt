@file:Suppress("UnstableApiUsage")

package io.github.remoteexcel.backend

import com.intellij.platform.rpc.backend.RemoteApiProvider
import fleet.rpc.remoteApiDescriptor
import io.github.remoteexcel.shared.ExcelBridgeRpcApi

internal class BackendRpcApiProvider : RemoteApiProvider {
    override fun RemoteApiProvider.Sink.remoteApis() {
        remoteApi(remoteApiDescriptor<ExcelBridgeRpcApi>()) {
            BackendExcelBridgeRpcApi()
        }
    }
}

