#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
manifest="$repo_root/sbom/rootfs-packages.tsv"
output_dir="${1:-$repo_root/third-party-sources/rootfs}"
source_list="$(mktemp)"
source_versions="$(mktemp)"
trap 'rm -f "$source_list" "$source_versions"' EXIT

cat > "$source_list" <<'EOF'
deb-src https://ports.ubuntu.com/ubuntu-ports/ jammy main restricted universe multiverse
deb-src https://ports.ubuntu.com/ubuntu-ports/ jammy-updates main restricted universe multiverse
deb-src https://ports.ubuntu.com/ubuntu-ports/ jammy-backports main restricted universe multiverse
deb-src https://ports.ubuntu.com/ubuntu-ports/ jammy-security main restricted universe multiverse
EOF

mkdir -p "$output_dir/apt-state/lists/partial" "$output_dir/archives"

tail -n +2 "$manifest" | cut -f4,5 | LC_ALL=C sort -u > "$source_versions"

apt-get \
    -o "Dir::Etc::sourcelist=$source_list" \
    -o "Dir::Etc::sourceparts=-" \
    -o "Dir::State=$output_dir/apt-state" \
    update

while IFS=$'\t' read -r source_package source_version; do
    (
        cd "$output_dir/archives"
        apt-get \
            -o "Dir::Etc::sourcelist=$source_list" \
            -o "Dir::Etc::sourceparts=-" \
            -o "Dir::State=$output_dir/apt-state" \
            source --download-only "$source_package=$source_version"
    )
done < "$source_versions"

echo "Ubuntu corresponding sources downloaded to $output_dir/archives"
