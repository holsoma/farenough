#!/usr/bin/env bash
# Create or refresh a draft. This script never publishes a release.
set -euo pipefail

package_dir=${1:-.release}
mode=${2:-apply}
tag=${GITHUB_REF_NAME:?Set GITHUB_REF_NAME to the version tag.}
repo=${GITHUB_REPOSITORY:?Set GITHUB_REPOSITORY to the owner/repository.}
[[ "$tag" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo 'Expected a stable version tag.' >&2; exit 1; }
[[ "$repo" =~ ^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$ ]] || { echo 'Invalid repository name.' >&2; exit 1; }
[[ "$mode" == apply || "$mode" == --dry-run ]] || { echo 'Unknown mode.' >&2; exit 1; }

for name in "farenough-$tag.apk" SHA256SUMS.txt signing-certificate.txt release-notes.md; do
  test -s "$package_dir/$name" || { echo "Missing release file: $name" >&2; exit 1; }
done
checksum=$(cat "$package_dir/SHA256SUMS.txt")
actual_checksum=$(cd "$package_dir" && sha256sum --binary "farenough-$tag.apk")
actual_checksum="${actual_checksum%% *}  farenough-$tag.apk"
[[ "$checksum" == "$actual_checksum" ]] || { echo 'The checksum must describe exactly this APK and use LF line endings.' >&2; exit 1; }
echo "farenough-$tag.apk: checksum verified"
expected=$(tr -d '\r' < SIGNING.md | sed -n '/^[0-9a-f]\{64\}$/p')
actual=$(tr -d '\r' < "$package_dir/signing-certificate.txt" | sed -n 's/^Signer #[0-9][0-9]* certificate SHA-256 digest: //p')
[[ "$expected" =~ ^[0-9a-f]{64}$ && "$actual" == "$expected" ]] || {
  echo 'The APK certificate does not match SIGNING.md. Restore the recorded signing key.' >&2
  exit 1
}

# A draft without a Git tag can be absent from GET /releases/tags/{tag}.
# A successful list request distinguishes absence from an API/authentication failure.
state=$(gh api "repos/$repo/releases" --paginate --jq ".[] | select(.tag_name == \"$tag\") | .draft")
case "$state" in
  false) echo 'Release already published. Existing assets are retained.'; exit 0 ;;
  true|'') ;;
  *) echo 'More than one matching release or an unexpected release state.' >&2; exit 1 ;;
esac
if [[ "$mode" == --dry-run ]]; then
  if [[ "$state" == true ]]; then echo "Would refresh the existing $tag draft."; else echo "Would create a $tag draft."; fi
  exit 0
fi
assets=("$package_dir/farenough-$tag.apk" "$package_dir/SHA256SUMS.txt" "$package_dir/signing-certificate.txt")
if [[ "$state" == true ]]; then
  gh release upload "$tag" --repo "$repo" --clobber "${assets[@]}"
  gh release edit "$tag" --repo "$repo" --title "farenough $tag" --notes-file "$package_dir/release-notes.md"
else
  gh release create "$tag" --repo "$repo" --verify-tag --draft --title "farenough $tag" --notes-file "$package_dir/release-notes.md" "${assets[@]}"
fi
