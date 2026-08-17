#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
rootfs="$repo_root/app/src/main/assets/ubuntu-rootfs.tar.gz"
output="$repo_root/sbom/rootfs-packages.tsv"
status_file="$(mktemp)"
rows_file="$(mktemp)"
trap 'rm -f "$status_file" "$rows_file"' EXIT

tar -xOzf "$rootfs" ./var/lib/dpkg/status > "$status_file"

awk 'BEGIN { RS=""; FS="\n"; OFS="\t" }
{
    package=""; version=""; architecture=""; source=""
    for (i = 1; i <= NF; i++) {
        if ($i ~ /^Package: /) package=substr($i, 10)
        else if ($i ~ /^Version: /) version=substr($i, 10)
        else if ($i ~ /^Architecture: /) architecture=substr($i, 15)
        else if ($i ~ /^Source: /) source=substr($i, 9)
    }
    if (package == "" || version == "") next
    source_package=source
    source_version=version
    if (source_package == "") source_package=package
    if (match(source_package, / \([^)]*\)$/)) {
        source_version=substr(source_package, RSTART + 2, RLENGTH - 3)
        source_package=substr(source_package, 1, RSTART - 1)
    }
    print package, version, architecture, source_package, source_version
}' "$status_file" | LC_ALL=C sort > "$rows_file"

{
    printf 'package\tversion\tarchitecture\tsource_package\tsource_version\n'
    cat "$rows_file"
} > "$output"

echo "Wrote $output"
