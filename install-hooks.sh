#!/usr/bin/env bash
#
# Installs git hooks in a site repository which generate the site before a
# commit or a push is accepted, so that a broken site - a dangling link, a
# document that moved without its links being updated - cannot enter the
# history.
#
# Run it from the root of the site repository, for instance when the generator
# is a submodule at sitegenerator/:
#
#    sitegenerator/install-hooks.sh
#
# See README.md, "Checking the site before every commit".
#
set -eu

HOOKS="pre-commit pre-push"
MARKER="# sigeto-generated-hook - re-run install-hooks.sh to update, or delete this file to remove"

sigetoHome=$(cd "$(dirname "$0")" && pwd)
repoDir=""
siteRoot=""
outputDir=""
force="false"
uninstall="false"

usage() {
	cat <<EOF
Usage: $0 [options]

  --repo <dir>         the site repository to install into (default: the git
                       repository containing the current directory)
  --site-root <dir>    the site source directory holding content/ and
                       templates/ (default: found automatically)
  --output <dir>       where the hook generates the site (default: the
                       generator's own default, <site-root>/_output)
  --force              replace hooks that are already there, keeping a .bak
  --uninstall          remove the hooks this script installed
  -h, --help           this text
EOF
}

while [ $# -gt 0 ]; do
	case "$1" in
		--repo) repoDir="$2"; shift 2 ;;
		--site-root) siteRoot="$2"; shift 2 ;;
		--output) outputDir="$2"; shift 2 ;;
		--force) force="true"; shift ;;
		--uninstall) uninstall="true"; shift ;;
		-h|--help) usage; exit 0 ;;
		*) echo >&2 "Unknown option: $1"; usage >&2; exit 2 ;;
	esac
done

die() {
	echo >&2 "Error: $*"
	exit 1
}

#-- Which repository are we installing into?
if [ -n "$repoDir" ]; then
	[ -d "$repoDir" ] || die "no such directory: $repoDir"
	repoRoot=$(cd "$repoDir" && git rev-parse --show-toplevel 2>/dev/null) || die "$repoDir is not inside a git repository"
else
	repoRoot=$(git rev-parse --show-toplevel 2>/dev/null) || die "not inside a git repository - use --repo <dir>"
fi

if [ "$repoRoot" = "$sigetoHome" ]; then
	die "this is the generator's own repository, not a site repository.
       Run it from the site repository instead, e.g. sitegenerator/install-hooks.sh"
fi

hooksDir=$(cd "$repoRoot" && git rev-parse --git-path hooks)
case "$hooksDir" in
	/*) ;;
	*) hooksDir="$repoRoot/$hooksDir" ;;
esac

#-- Uninstalling needs none of the rest
if [ "$uninstall" = "true" ]; then
	for hook in $HOOKS; do
		target="$hooksDir/$hook"
		if [ -f "$target" ] && grep -qF "sigeto-generated-hook" "$target"; then
			rm -f "$target"
			echo "Removed $target"
			if [ -f "$target.bak" ]; then
				mv "$target.bak" "$target"
				echo "Restored the hook that was there before, from $target.bak"
			fi
		fi
	done
	(cd "$repoRoot" && git config --unset sigeto.home || true)
	(cd "$repoRoot" && git config --unset sigeto.siteroot || true)
	(cd "$repoRoot" && git config --unset sigeto.output || true)
	echo "Done."
	exit 0
fi

#-- Find the site source: a directory with both content/ and templates/
isSite() {
	[ -d "$1/content" ] && [ -d "$1/templates" ]
}

if [ -n "$siteRoot" ]; then
	siteDir=$(cd "$siteRoot" && pwd) || die "no such directory: $siteRoot"
	isSite "$siteDir" || die "$siteRoot has no content/ and templates/ in it"
elif isSite "$repoRoot"; then
	siteDir="$repoRoot"
else
	found=""
	for dir in "$repoRoot"/*/; do
		[ -d "$dir" ] || continue
		dir="${dir%/}"
		if isSite "$dir"; then
			found="$found $dir"
		fi
	done
	set -- $found
	case $# in
		0) die "no site source found in $repoRoot - use --site-root <dir>.
       A site source is a directory containing both content/ and templates/." ;;
		1) siteDir="$1" ;;
		*) die "several possible site sources found ($*) - use --site-root <dir>" ;;
	esac
fi

#-- Store paths relative to the repository root when they are inside it, so
#-- that the hooks survive the checkout being moved
relativeTo() {
	case "$1" in
		"$2"/*) echo "${1#"$2"/}" ;;
		*) echo "$1" ;;
	esac
}

siteConfig=$(relativeTo "$siteDir" "$repoRoot")
homeConfig=$(relativeTo "$sigetoHome" "$repoRoot")

check="$sigetoHome/githooks/sigeto-check.sh"
[ -f "$check" ] || die "the generator checkout at $sigetoHome has no githooks/sigeto-check.sh"
chmod +x "$check" 2>/dev/null || true

#-- Install
mkdir -p "$hooksDir"
for hook in $HOOKS; do
	target="$hooksDir/$hook"
	if [ -f "$target" ] && ! grep -qF "sigeto-generated-hook" "$target"; then
		if [ "$force" != "true" ]; then
			die "there already is a $hook hook at $target.
       Use --force to replace it (the old one is kept as $hook.bak)."
		fi
		mv "$target" "$target.bak"
		echo "Kept the existing $hook hook as $target.bak"
	fi
	cat > "$target" <<EOF
#!/bin/sh
$MARKER
home=\$(git config sigeto.home) || exit 0
case "\$home" in
	/*) ;;
	*) home="\$(git rev-parse --show-toplevel)/\$home" ;;
esac
exec "\$home/githooks/sigeto-check.sh" $hook
EOF
	chmod +x "$target"
	echo "Installed $target"
done

(cd "$repoRoot" && git config sigeto.home "$homeConfig")
(cd "$repoRoot" && git config sigeto.siteroot "$siteConfig")
if [ -n "$outputDir" ]; then
	(cd "$repoRoot" && git config sigeto.output "$outputDir")
else
	(cd "$repoRoot" && git config --unset sigeto.output || true)
fi

cat <<EOF

Done. In $repoRoot:
  generator   $homeConfig
  site source $siteConfig

Every commit that touches $siteConfig now generates the site first, and is
refused if the site does not build - or if the generator had to record a
document move or repair a link, so you get to review and stage that first.
Pushes are checked the same way.

Remove them again with: $0 --uninstall
EOF
