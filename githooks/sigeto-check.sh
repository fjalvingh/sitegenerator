#!/usr/bin/env bash
#
# Runs the site generator as a git hook, refusing the commit or push when the
# site does not generate cleanly - or when the generator had to change files,
# which is what it does when a document moved: it records the move in
# redirects.tsv and repairs the links to it in the markdown sources.
#
# Installed by install-hooks.sh; it is configured through git config:
#
#   sigeto.home        where the generator checkout lives (relative to the
#                      repository root, or absolute)
#   sigeto.siteroot    the site source directory, holding content/ and
#                      templates/ (relative to the repository root)
#   sigeto.output      optional output directory, default <siteroot>/_output
#
set -u

hook="${1:-pre-commit}"

say() {
	echo "sigeto[$hook]: $*"
}

fail() {
	echo >&2
	echo >&2 "sigeto[$hook]: $*"
	echo >&2
	exit 1
}

repoRoot=$(git rev-parse --show-toplevel 2>/dev/null) || fail "not inside a git repository"
cd "$repoRoot" || fail "cannot enter $repoRoot"

#-- Where is everything?
siteRoot=$(git config sigeto.siteroot || true)
sigetoHome=$(git config sigeto.home || true)
outputDir=$(git config sigeto.output || true)
[ -n "$siteRoot" ] || fail "git config sigeto.siteroot is not set - re-run the generator's install-hooks.sh"
[ -n "$sigetoHome" ] || fail "git config sigeto.home is not set - re-run the generator's install-hooks.sh"

absolute() {
	case "$1" in
		/*) echo "$1" ;;
		*) echo "$repoRoot/$1" ;;
	esac
}

siteDir=$(absolute "$siteRoot")
homeDir=$(absolute "$sigetoHome")
[ -d "$siteDir/content" ] || fail "$siteRoot does not look like a site source directory: no content/ in it"
[ -d "$homeDir" ] || fail "the generator is not at $sigetoHome - if it is a submodule, run: git submodule update --init"

#-- Nothing staged below the site? Then this commit cannot break it.
if [ "$hook" = "pre-commit" ]; then
	if git diff --cached --quiet -- "$siteRoot" 2>/dev/null; then
		say "no staged changes below $siteRoot, skipping the site check"
		exit 0
	fi
fi

#-- Build the generator when it is missing or older than its own sources
jar="$homeDir/target/sitegen.jar"
if [ ! -f "$jar" ] || [ -n "$(find "$homeDir/src" "$homeDir/pom.xml" -newer "$jar" -print -quit 2>/dev/null)" ]; then
	command -v mvn >/dev/null 2>&1 || fail "$jar is missing or out of date and maven (mvn) is not installed to build it"
	say "building the site generator in $sigetoHome ..."
	buildLog=$(mktemp)
	if ! (cd "$homeDir" && mvn -q --batch-mode -DskipTests package >"$buildLog" 2>&1); then
		cat >&2 "$buildLog"
		rm -f "$buildLog"
		fail "the site generator itself does not build"
	fi
	rm -f "$buildLog"
fi
[ -f "$jar" ] || fail "no generator jar at $jar after building"

#-- Warn about unstaged work, which is generated along with everything else
if [ "$hook" = "pre-commit" ] && ! git diff --quiet -- "$siteRoot" 2>/dev/null; then
	say "note: you have unstaged changes below $siteRoot; the site is generated from the working tree, so they are included in this check"
fi

#-- The paths whose git status changed while the site was generated
changedFiles() {
	diff <(echo "$before") <(echo "$after") | sed -n 's/^[<>] //p' | cut -c4- | sed 's/.* -> //' | sort -u
}

reportChanges() {
	echo >&2
	echo >&2 "The generator changed these files:"
	changedFiles | sed 's/^/    /' >&2
}

#-- What did the working tree look like before generating?
before=$(git status --porcelain -- "$siteRoot" 2>/dev/null)

say "generating the site from $siteRoot ..."
if [ -n "$outputDir" ]; then
	java -jar "$jar" -i "$siteDir" -o "$(absolute "$outputDir")"
else
	java -jar "$jar" -i "$siteDir"
fi
status=$?

after=$(git status --porcelain -- "$siteRoot" 2>/dev/null)

if [ $status -ne 0 ]; then
	if [ "$before" != "$after" ]; then
		reportChanges
	fi
	case "$hook" in
		pre-commit)
			fail "the site does not generate cleanly, so the commit was refused.
       Fix the errors above (links the generator repaired are already changed
       for you), then 'git add' the changed files and commit again.
       To commit anyway: git commit --no-verify" ;;
		*)
			fail "the site does not generate cleanly, so the push was refused.
       Fix the errors above and commit the result before pushing.
       To push anyway: git push --no-verify" ;;
	esac
fi

if [ "$before" != "$after" ]; then
	reportChanges
	case "$hook" in
		pre-commit)
			fail "the site generates cleanly, but the generator recorded document
       moves (redirects.tsv) or repaired links in the sources. Review the
       changes above, 'git add' them, and commit again.
       To commit anyway: git commit --no-verify" ;;
		*)
			fail "the site generates cleanly, but the generator recorded document
       moves (redirects.tsv) or repaired links in the sources, which are not
       in the commits being pushed. Review and commit them first.
       To push anyway: git push --no-verify" ;;
	esac
fi

say "site generates cleanly"
exit 0
