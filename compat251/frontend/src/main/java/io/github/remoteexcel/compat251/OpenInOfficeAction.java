package io.github.remoteexcel.compat251;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
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

public final class OpenInOfficeAction extends DumbAwareAction {
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
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        event.getPresentation().setEnabledAndVisible(
                event.getProject() != null && file != null && !file.isDirectory() && supports(file.getExtension())
        );
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || file == null) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "正在下载远程工作簿", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    Path localPath = createLocalSnapshot(project, file, indicator);
                    openWithDesktop(localPath);
                    showNotification(project,
                            "已打开 " + file.getName() + " 的本地副本；本地修改不会影响远程文件。",
                            NotificationType.INFORMATION);
                } catch (Exception error) {
                    showNotification(project,
                            error.getMessage() == null ? "无法打开远程工作簿。" : error.getMessage(),
                            NotificationType.ERROR);
                }
            }
        });
    }

    private static Path createLocalSnapshot(
            Project project,
            VirtualFile file,
            ProgressIndicator indicator
    ) throws IOException, NoSuchAlgorithmException {
        if (!file.isValid() || file.isDirectory()) {
            throw new IOException("所选项目不是可读取的文件。");
        }
        if (!supports(file.getExtension())) {
            throw new IOException("所选文件不是受支持的工作簿。");
        }
        if (file.getLength() > MAX_FILE_SIZE_BYTES) {
            throw new IOException("工作簿超过 2 GB 上限。");
        }

        Path cacheRoot = Path.of(
                PathManager.getSystemPath(),
                "remote-excel-bridge",
                project.getLocationHash(),
                sha256(file.getUrl()).substring(0, 20)
        );
        Files.createDirectories(cacheRoot);
        Path snapshotDirectory = Files.createTempDirectory(cacheRoot, "snapshot-");
        Path localPath = snapshotDirectory.resolve(safeFileName(file.getName()));

        long copied = 0;
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        try (InputStream input = file.getInputStream();
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
                if (file.getLength() > 0) {
                    indicator.setFraction(Math.min(1.0, (double) copied / file.getLength()));
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
            throw new IOException("本地 JetBrains Client 无法调用操作系统的默认应用程序。");
        }
        Desktop.getDesktop().open(path.toFile());
    }

    private static void showNotification(Project project, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(content, type)
                .notify(project);
    }

    private static boolean supports(String extension) {
        return extension != null && SUPPORTED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
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
