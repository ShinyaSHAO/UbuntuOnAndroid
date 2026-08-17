# Corresponding Source

本仓库分发预编译的 ARM64 原生程序和 Ubuntu rootfs。发布 APK 时，维护者应将本文件、`THIRD_PARTY_NOTICES.md`、`licenses/`、`sbom/` 以及对应源码归档与 APK 一同保留。仅提供项目自身 MIT 源码不足以覆盖 GPL/LGPL 组件的义务。

## Termux 原生组件

执行以下命令会下载固定版本的上游源码并校验发布方构建配方中声明的 SHA-256：

```bash
./scripts/fetch-native-sources.sh
```

来源基线：

| 组件 | 源码 | 源码 SHA-256 | Termux ARM64 软件包 SHA-256 |
| --- | --- | --- | --- |
| PRoot 5.1.107.91 | `https://github.com/termux/proot/archive/v5.1.107.91.zip` | `a7bc2fab34bf9a39073e8291f08a662e848c61a67494e59f5f84f5ca10690128` | `1ad09f7ddf65f297a7b59a398cd1f23a48748b1144b225f0c1e2d9f447ef3efc` |
| libandroid-shmem 0.7 | `https://github.com/termux/libandroid-shmem/archive/refs/tags/v0.7.tar.gz` | `1e5ff8459bc0a8c229dd8a94b27d119987e09ef3414331c2b5ebfff20b98e867` | `0da3a24d558b93c92bcf8d611e0826a99ff96e396b148e6cdf33b47c47c57ff6` |
| talloc 2.4.3 | `https://www.samba.org/ftp/talloc/talloc-2.4.3.tar.gz` | `dc46c40b9f46bb34dd97fe41f548b0e8b247b77a918576733c528e83abd854dd` | `ac81ad623d74c209718b9f3acb2dd702cc8a88c431e820d212229910b4db29da` |

Termux 构建系统及固定配方链接列在 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。官方 PRoot ARM64 文件的 SHA-256 为 `cafe2f3957f0c5c92c2f0f545e259e09d1b580d2ea1264acdbb96e88223e19fd`；APK 只能自动打包以 `.so` 结尾的 JNI 文件，因此仓库执行以下可复现变换，将 `DT_NEEDED` 的 `libtalloc.so.2` 改为 `libtalloc.so`：

```bash
./scripts/patch-proot-for-android.sh /path/to/official/proot /tmp/libproot.so
```

变换后的 SHA-256 为 `c0cce6ea90191c4ea0c3e0dc8dcdf00f9df04d2f1eb2867bf10bf4de1c055c17`。程序代码和动态链接 ABI 均未改变。若修改、替换或重新编译这些文件，必须同步更新版本、补丁、构建脚本和所有校验值。

## Ubuntu Rootfs

`sbom/rootfs-packages.tsv` 的 `source_package` 与 `source_version` 列记录 rootfs 中每个二进制包对应的 Ubuntu 源码包。可在已安装 `apt` 的 Ubuntu/Debian 环境运行：

```bash
./scripts/fetch-rootfs-sources.sh
```

脚本使用 Ubuntu Ports Jammy 的 `deb-src` 仓库下载清单中的精确源码版本。Ubuntu 仓库镜像会淘汰旧更新版本；正式发布者应在发布时运行该脚本，将成功下载的源码归档到与 APK 同等持久的位置，而不能依赖未来仍可从滚动镜像取得旧版本。

## Android 库

Android/Kotlin 依赖由 Gradle 解析。`terminal-view` 的固定源码标签为 `v0.118.0`：

```text
https://github.com/termux/termux-app/tree/v0.118.0/terminal-view
https://github.com/termux/termux-app/tree/v0.118.0/terminal-emulator
```

版本目录位于 `gradle/libs.versions.toml`，Gradle 锁定结果可通过 `releaseRuntimeClasspath` 依赖报告审计。
