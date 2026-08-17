#!/usr/bin/env bash
set -euo pipefail

output_dir="${1:-third-party-sources/native}"
mkdir -p "$output_dir"

download_and_verify() {
    local url="$1"
    local sha256="$2"
    local filename="$3"

    curl --fail --location --retry 3 --retry-all-errors \
        "$url" --output "$output_dir/$filename"
    printf '%s  %s\n' "$sha256" "$output_dir/$filename" | shasum -a 256 --check
}

download_and_verify \
    "https://github.com/termux/proot/archive/v5.1.107.91.zip" \
    "a7bc2fab34bf9a39073e8291f08a662e848c61a67494e59f5f84f5ca10690128" \
    "proot-5.1.107.91.zip"

download_and_verify \
    "https://github.com/termux/libandroid-shmem/archive/refs/tags/v0.7.tar.gz" \
    "1e5ff8459bc0a8c229dd8a94b27d119987e09ef3414331c2b5ebfff20b98e867" \
    "libandroid-shmem-0.7.tar.gz"

download_and_verify \
    "https://www.samba.org/ftp/talloc/talloc-2.4.3.tar.gz" \
    "dc46c40b9f46bb34dd97fe41f548b0e8b247b77a918576733c528e83abd854dd" \
    "talloc-2.4.3.tar.gz"

echo "Native corresponding sources downloaded to $output_dir"
