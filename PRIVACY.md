# Privacy Notice

生效日期：2026-08-17

UbuntuOnAndroid 不提供账号系统，不集成广告、分析 SDK、崩溃上报或遥测服务，应用代码不会主动把终端内容、Ubuntu 文件、设备标识或使用记录上传给项目维护者。

## 本地数据

- Ubuntu rootfs、终端产生的文件和临时文件保存在 Android 应用沙盒的 `noBackupFilesDir` 中。
- 应用在 Manifest 中禁用了 Android Auto Backup 和设备到设备备份。
- 卸载应用或清除应用数据会删除这些本地数据。项目维护者无法远程读取或恢复它们。
- 从旧版本升级时，应用会将原 `files/ubuntu-fs` 目录迁移到不可备份目录；旧版本已经产生的云端备份不受本应用控制，用户应在 Android/Google 账号的备份设置中自行检查和删除。

## 网络连接

- `/etc/resolv.conf` 使用 Android 当前活动网络提供的 DNS 服务器。DNS 查询可能由网络运营商、Wi-Fi 提供方或 VPN 服务商处理。
- APT 默认配置为 Canonical 的 Ubuntu Ports HTTPS 服务 `https://ports.ubuntu.com/ubuntu-ports/`。只有在用户运行 `apt` 等联网命令时才会访问该服务。
- 用户可以主动运行 `ubuntuonandroid-set-mirror tencent` 切换到腾讯云镜像；该操作会把后续 APT 请求发送给腾讯云。运行 `ubuntuonandroid-set-mirror official` 可恢复官方镜像。
- 用户在 Ubuntu 终端中运行的 `ssh`、`curl`、浏览器、包管理器或其他程序可能连接相应第三方服务，其数据处理规则由用户选择的软件和服务决定。
- 内置 SSH 服务只接受到设备的连接；应用不会自动连接项目维护者的服务器。

## Android 权限

- `INTERNET`：允许 Ubuntu 命令和 SSH 服务访问网络。
- `ACCESS_NETWORK_STATE`：读取当前活动网络的 DNS 配置。
- `WAKE_LOCK`：支持终端会话运行。

## 联系方式

隐私或安全问题请通过 GitHub 仓库的 Security Advisory 私密报告；一般问题可使用 GitHub Issues。提交报告时不要附带密码、私钥、终端历史或其他敏感信息。
