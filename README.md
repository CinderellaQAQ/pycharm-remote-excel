# Remote Excel Bridge for PyCharm

这个插件解决 PyCharm Remote Development / SSH 项目中的一个常见问题：Excel 文件位于远程服务器，IDE 后端无法调用用户电脑上的 Microsoft Excel、WPS 或 LibreOffice。

插件采用 JetBrains Split Mode 原生结构：

- `frontend` 运行在本机 JetBrains Client，保存临时副本并调用操作系统关联的 Office 软件。
- `backend` 运行在 SSH 服务器上的 PyCharm 后端，只读取远程文件。
- `shared` 定义前后端之间的 RPC 协议。

## 当前功能

- 在 Project 视图或编辑器标签页右键选择 **在本地 Office 中打开（不回传）**。
- 将远程工作簿下载到本机 IDE 缓存目录，并使用系统默认关联程序打开。
- 本地副本与远程文件完全独立；在 Office 中保存只修改本地副本，不会上传或覆盖远程文件。
- 每次执行打开操作都会从远程文件创建一份新的本地快照。
- 支持 `xls`、`xlsx`、`xlsm`、`xlsb`、`xlt`、`xltx`、`xltm` 和 `ods`。
- 默认限制单个文件为 2 GB。

## 构建

要求：

- JDK 21（推荐直接使用 PyCharm 自带的 JetBrains Runtime）
- PyCharm 2025.3.4 或同一产品线的更新版本
- 本机已安装 Excel、WPS、LibreOffice 等软件，并为工作簿扩展名设置了默认关联程序

```bash
./gradlew buildPlugin
```

如果本机已经安装了对应版本的 PyCharm，可以避免重新下载 IDE：

```bash
./gradlew buildPlugin -PlocalIdePath=/absolute/path/to/pycharm
```

插件 ZIP 位于 `build/distributions/`。在 PyCharm 的 **Settings | Plugins** 中选择齿轮菜单，再选择 **Install Plugin from Disk...** 安装 ZIP。JetBrains 当前只会为 Marketplace 插件自动同步前后端；用本地 ZIP 测试时，需要在 Remote Development 的 Client 和 Host 两侧都安装同一个 ZIP。

### PyCharm 2025.1.2 兼容包

`compat251/` 包含面向 `PY-251.26094.141` 的前端兼容实现。它不使用 2025.3 才提供的 RPC API；插件在本地 JetBrains Client 中通过远程 VFS 流式创建快照，因此不会把整个工作簿一次性载入内存。251 兼容包的模块 JAR 名称必须与模块描述符名称严格一致，否则 251 的插件加载器会把安装包判定为无效。

```bash
./gradlew -p compat251 buildPlugin
```

兼容包位于 `compat251/build/distributions/`，版本范围限定为 `251.26094.141` 到 `251.*`。本地 ZIP 无法通过 Marketplace 自动同步到另一侧，因此远程开发会话中需要在 JetBrains Client 和标有 **On Host** 的远程端分别安装同一个兼容包；右键动作只会在本机 Frontend 加载。

开发调试：

```bash
./gradlew runIde
```

构建配置启用了 Split Mode，并把插件安装目标设置为前后端两侧，因此调试环境会同时启动 backend 与 frontend。

## 本地副本行为

本机副本保存在 IDE system path 下的 `remote-excel-bridge/` 目录。插件不监听本地文件变更，也没有上传 RPC 或远程写入代码。每次打开都会创建新的快照目录，因此修改本地副本不会影响服务器上的原始文件。

## 已知限制

- JetBrains 的第三方 Split Mode / Modular Plugin API 目前仍标记为实验性，因此本项目固定针对 2025.3 API。
- 第一版使用 RPC 直接传输文件内容，适合普通工作簿，不适合数百 MiB 的超大文件。
- 系统默认关联程序决定实际打开 Excel、WPS 还是 LibreOffice；插件当前不提供单独的可执行文件路径设置。
