# Third-Party Notices

本项目自身源代码采用 [MIT License](LICENSE)。以下组件随源码仓库、Gradle 构建或 APK 一同分发，并继续受各自许可证约束；MIT License 不替代这些条款。

## 原生运行组件

| 组件 | 版本 | 许可证 | 对应源码与构建配方 |
| --- | --- | --- | --- |
| Termux PRoot / loader | 5.1.107.91 | GPL-2.0 | [源码标签](https://github.com/termux/proot/tree/v5.1.107.91)，[Termux 配方](https://github.com/termux/termux-packages/blob/8ec1d2071728ab5f79fd509d089892cddb295f36/packages/proot/build.sh) |
| libandroid-shmem | 0.7 | BSD-3-Clause | [源码标签](https://github.com/termux/libandroid-shmem/tree/v0.7)，[Termux 配方](https://github.com/termux/termux-packages/blob/b25e257208da6d2e8b558b8a2b51762158a2e806/packages/libandroid-shmem/build.sh) |
| talloc | 2.4.3 | LGPL-3.0-or-later | [源码包](https://www.samba.org/ftp/talloc/talloc-2.4.3.tar.gz)，[Termux 配方](https://github.com/termux/termux-packages/blob/fbc049451e7fc59cdf510732aad49bd45590b0bb/packages/libtalloc/build.sh) |

仓库中的 ARM64 文件来自 Termux 官方软件包，软件包和文件 SHA-256 记录在 [sbom/native-artifacts.sha256](sbom/native-artifacts.sha256)。为符合 Android APK 的 `.so` 文件命名规则，仓库中的 PRoot 将 `DT_NEEDED` 从 `libtalloc.so.2` 改为字节内容相同的 `libtalloc.so`；可复现变换见 `scripts/patch-proot-for-android.sh`。对应源码的固定 URL、校验值和下载命令见 [SOURCE_CODE.md](SOURCE_CODE.md)。

## Android 依赖

- Termux `terminal-view` / `terminal-emulator` v0.118.0。Termux v0.118.0 的许可证说明明确将这两个库所用的 Android Terminal Emulator 代码列为 Apache-2.0 例外；完整说明见 [Termux-v0.118.0-LICENSE.md](licenses/Termux-v0.118.0-LICENSE.md)。
- AndroidX、Jetpack Compose 与 Material 组件采用 Apache-2.0。构建产物保留依赖所带的 `META-INF` 许可证资源；项目不再排除 `AL2.0` 或 `LGPL2.1` 文件。
- JVM 和 Android 测试依赖不进入 release APK，其版本仍可在 `gradle/libs.versions.toml` 中审计。

完整 Gradle 依赖树可通过以下命令生成：

```bash
./gradlew app:dependencies --configuration releaseRuntimeClasspath
```

## Ubuntu Rootfs

`app/src/main/assets/ubuntu-rootfs.tar.gz` 是 Ubuntu 22.04.5 LTS ARM64 文件系统，包含多个独立软件包。每个已安装包的版权和许可证文本位于归档内的 `/usr/share/doc/<package>/copyright`，常见许可证全文位于 `/usr/share/common-licenses/`。

- 已安装二进制包、版本、架构和对应源码包见 [sbom/rootfs-packages.tsv](sbom/rootfs-packages.tsv)。
- 归档 SHA-256 见 [sbom/native-artifacts.sha256](sbom/native-artifacts.sha256)。
- 获取精确 Ubuntu 源码包的方法见 [SOURCE_CODE.md](SOURCE_CODE.md) 和 `scripts/fetch-rootfs-sources.sh`。

Ubuntu 名称和标识受 Canonical 的知识产权政策约束；本项目不是 Canonical 官方产品，也未获得 Canonical 背书。
