package io.github.remoteexcel.compat251;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.plugins.webDeployment.actions.WebDeploymentDataKeys;
import org.apache.commons.vfs2.FileObject;
import org.jetbrains.annotations.NotNull;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Set;

public final class RemoteHostOpenInOfficeAction extends DumbAwareAction {
    private static final long MAX_FILE_SIZE_BYTES = 2_000_000_000L;
    private static final int COPY_BUFFER_SIZE = 1024 * 1024;
    private static final String NOTIFICATION_GROUP_ID = "Remote Excel Bridge";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "xls", "xlsx", "xlsm", "xlsb", "xlt", "xltx", "xltm", "ods"
    );

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        FileObject file = getSelectedRemoteFile(event);
        boolean available = false;
        if (event.getProject() != null && file != null && supports(file.getName().getBaseName())) {
            try {
                available = file.exists() && file.getType().hasContent();
            } catch (IOException ignored) {
                available = false;
            }
        }
        event.getPresentation().setEnabledAndVisible(available);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        FileObject remoteFile = getSelectedRemoteFile(event);
        if (project == null || remoteFile == null) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "正在下载远程工作簿", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    Path localPath = createLocalSnapshot(project, remoteFile, indicator);
                    openWithDesktop(localPath);
                    showNotification(project,
                            "已打开 " + remoteFile.getName().getBaseName() + " 的本地副本；本地修改不会影响远程文件。",
                            NotificationType.INFORMATION);
                } catch (Exception error) {
                    showNotification(project,
                            error.getMessage() == null ? "无法打开远程工作簿。" : error.getMessage(),
                            NotificationType.ERROR);
                }
            }
        });
    }

    private static FileObject getSelectedRemoteFile(AnActionEvent event) {
        FileObject[] files = event.getData(WebDeploymentDataKeys.REMOTE_ITEMS);
        return files != null && files.length == 1 ? files[0] : null;
    }

    private static Path createLocalSnapshot(
            Project project,
            FileObject remoteFile,
            ProgressIndicator indicator
    ) throws IOException, NoSuchAlgorithmException {
        String fileName = remoteFile.getName().getBaseName();
        if (!remoteFile.exists() || !remoteFile.getType().hasContent()) {
            throw new IOException("所选项目不是可读取的远程文件。");
        }
        if (!supports(fileName)) {
            throw new IOException("所选文件不是受支持的工作簿。");
        }

        long expectedSize = remoteFile.getContent().getSize();
        if (expectedSize > MAX_FILE_SIZE_BYTES) {
            throw new IOException("工作簿超过 2 GB 上限。");
        }

        Path cacheRoot = Path.of(
                PathManager.getSystemPath(),
                "remote-excel-bridge",
                project.getLocationHash(),
                sha256(remoteFile.getName().getURI()).substring(0, 20)
        );
        Files.createDirectories(cacheRoot);
        Path snapshotDirectory = Files.createTempDirectory(cacheRoot, "snapshot-");
        Path localPath = snapshotDirectory.resolve(safeFileName(fileName));

        long copied = 0;
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        try (InputStream input = remoteFile.getContent().getInputStream();
             OutputStream output = Files.newOutputStream(
                     localPath,
                     StandardOpenOption.CREATE_NEW,
                     StandardOpenOption.WRITE)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                indicator.checkCanceled();
                copied += read;
                if (copied > MAX_FILE_SIZE_BYTES) {
                    throw new IOException("工作簿超过 2 GB 上限。");
                }
                output.write(buffer, 0, read);
                if (expectedSize > 0) {
                    indicator.setFraction(Math.min(1.0, (double) copied / expectedSize));
                }
            }
        } catch (Exception error) {
            Files.deleteIfExists(localPath);
            throw error;
        }

        return localPath;
    }

    private static void openWithDesktop(Path path) throws IOException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            throw new IOException("PyCharm 无法调用操作系统的默认应用程序。");
        }
        Desktop.getDesktop().open(path.toFile());
    }

    private static void showNotification(Project project, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(content, type)
                .notify(project);
    }

    private static boolean supports(String fileName) {
        int separator = fileName.lastIndexOf('.');
        return separator >= 0
                && separator < fileName.length() - 1
                && SUPPORTED_EXTENSIONS.contains(fileName.substring(separator + 1).toLowerCase(Locale.ROOT));
    }

    private static String safeFileName(String name) {
        String safeName = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
        return safeName.isBlank() ? "workbook.xlsx" : safeName;
    }

    private static String sha256(String value) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte item : digest) {
            result.append(String.format("%02x", item));
        }
        return result.toString();
    }
}
