# Software Bill of Materials

- `native-artifacts.sha256`：仓库内预编译原生文件、rootfs 归档及其 Termux 来源软件包的 SHA-256。
- `rootfs-packages.tsv`：Ubuntu rootfs 中由 dpkg 记录的二进制包、版本、架构和对应源码包。

修改 rootfs 后运行 `./scripts/generate-rootfs-sbom.sh`，并同步更新 `native-artifacts.sha256`。修改原生组件时必须从官方软件包重新提取并更新文件与软件包校验值。
