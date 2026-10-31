@file:Suppress("UnstableApiUsage")

package io.github.remoteexcel.frontend

import com.intellij.ide.vfs.rpcId
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

class OpenInOfficeAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible =
            event.project != null && file != null && !file.isDirectory && ExcelFiles.supports(file.extension)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        project.service<FrontendExcelBridgeService>().open(file.rpcId())
    }
}

internal object ExcelFiles {
    private val extensions = setOf("xls", "xlsx", "xlsm", "xlsb", "xlt", "xltx", "xltm", "ods")

    fun supports(extension: String?): Boolean = extension?.lowercase() in extensions
}
