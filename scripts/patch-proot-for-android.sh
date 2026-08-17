#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
    echo "Usage: $0 /path/to/official/proot /path/to/libproot.so" >&2
    exit 2
fi

input="$1"
output="$2"
official_sha256="cafe2f3957f0c5c92c2f0f545e259e09d1b580d2ea1264acdbb96e88223e19fd"
patched_sha256="c0cce6ea90191c4ea0c3e0dc8dcdf00f9df04d2f1eb2867bf10bf4de1c055c17"

sha256() {
    if command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$1" | awk '{print $1}'
    else
        sha256sum "$1" | awk '{print $1}'
    fi
}

if [[ "$(sha256 "$input")" != "$official_sha256" ]]; then
    echo "Input is not the audited Termux PRoot 5.1.107.91 ARM64 binary" >&2
    exit 1
fi

cp "$input" "$output"
LC_ALL=C perl -0pi -e 's/libtalloc\.so\.2\x00/libtalloc.so\x00\x00\x00/g' "$output"

if [[ "$(sha256 "$output")" != "$patched_sha256" ]]; then
    echo "Patched PRoot checksum does not match the audited result" >&2
    exit 1
fi

echo "Wrote Android-compatible PRoot to $output"
