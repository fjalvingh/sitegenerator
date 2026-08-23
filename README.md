# sigeto

A very simple static site generator that turns a tree of Markdown files into a
static HTML website. Written in Java, built on [commonmark-java](https://github.com/commonmark/commonmark-java)
for Markdown parsing and [jte](https://jte.gg/) for HTML page templates.

sigeto was written to generate the author's personal/hobby website (electronics
repair and retrocomputing articles), including a simple blog. It has no plugin
system, no config-file driven theming, and no live-reload server — you run it,
it produces a directory of `.html` files, and you publish that directory (e.g.
to GitHub Pages).

## Building

```
mvn clean package
```

This produces `target/sitegen-jar-with-dependencies.jar`, a self-contained
shaded jar (see the `maven-shade-plugin` configuration in `pom.xml`).

## Running

```
java -jar target/sitegen-jar-with-dependencies.jar -i <site-root> [-o <output-dir>]
```

- `-i` / `-input` (required): the root directory of the site source (see
  "Site layout" below).
- `-o` / `-output` (optional): where to write the generated site. Defaults to
  `<site-root>/_output`. The output directory is emptied before each run.

The `testsite/` directory in this repository is a full example site (the
author's real site content) and can be used to try the generator out, e.g.:

```
mvn clean package
java -jar target/sitegen-jar-with-dependencies.jar -i testsite -o testsite/_output
```

## Site layout

A site source directory (the `-i` argument) must contain two subdirectories:

```
<site-root>/
  content/            all markdown source files and their resources
    index.md          the root page of the site
    some-article/
      index.md         an article, one directory per article
      photo.png         a resource used by that article
      sub-article/
        index.md         a nested article (a directory inside its parent)
  templates/          jte templates and static theme assets (css, img, ...)
    base.jte           the main page template (required, invoked for every page)
    redirect.jte       optional template for moved-document redirect pages
    css/, img/, ...     copied verbatim into the output root
  redirects.tsv       the record of moved documents (generated, commit it)
```

Rules:
- The content root must contain an `index.md`; this is the site's root page.
- Every article is a directory containing its own `index.md`. Sub-articles are
  subdirectories of that article directory, each with their own `index.md`.
  This keeps an article and all of the resources (images, PDFs, ...) it uses
  together in one directory.
- Any non-`.jte` file under `templates/` (CSS, images, fonts, ...) is copied
  as-is into the output, preserving its relative path.
- A directory whose name is exactly 8 digits in `yyyymmdd` form (year
  2024-2099) is treated as a **blog entry** rather than a regular article.

Markdown files (`.md`, `.mdown`) are rendered to `.html` files with the same
base name; every other file under `content/` is copied to the output
unchanged.

## Markdown features

### Front matter

Pages can start with YAML front matter:

```
---
title: This is a title
menulocation: 10.5
---
# Page title
...
```

A `tags: foo, bar` (or YAML list) entry in the front matter tags the page,
and `menu: { hidden: true }` excludes a page from the generated site menu.

### Page titles

A page's title is taken from the first `# heading` (h1) found in the
document. Don't use more than one h1 per page.

### Table of contents

```
[TOC hierarchy]
```

generates a table of contents for the page; see the Flexmark TOC macro
documentation for the available options. Note that h1 headers (the page
title) are intentionally excluded from the generated TOC.

### Notifications / callout blocks

Lines starting with `!`, followed by an optional type flag, render as a
styled callout box:

| Prefix | Type    |
|--------|---------|
| `!i `  | info (default) |
| `!v `  | success |
| `!w` / `!! ` | warning |
| `!x` / `!e ` | error   |

### Emoji

GitHub-style `:shortcode:` emoji are supported, backed by
`src/main/resources/emoji/emoji.csv`.

### GFM tables and strikethrough

Standard GitHub-flavored-Markdown tables (`MyTablesExtension`) and
`~~strikethrough~~` are supported, along with automatically generated heading
anchors.

### Links, images and blogs

Internal links and images (`[text](other-page.md)`, `![alt](photo.png)`) are
checked at build time — the generator refuses to produce output if a link or
image points at a file that doesn't exist in `content/`. If the file was moved
rather than deleted the link is repaired in the source, see
[Moved documents](#moved-documents). Directories detected
as blog entries (see above) get additional blog-specific rendering (entry
listing, dates) via the blog extension.

## Moved documents

Renaming or moving an article changes its URL, which breaks every link to it
that lives outside the site and cannot be fixed - search results, other
people's pages, forum posts. The generator keeps those links working.

At the start of every build it asks git which files it has seen renamed below
`content/`, and records each move in `redirects.tsv` in the site root:

```
# sigeto redirect map: <old path><TAB><new path>, relative to content/.
old-name/index.md	new-place/old-name/index.md
```

Only documents are recorded. An image or a pdf has no url of its own that a
redirect page could be served at, and it moves along with the article directory
it belongs to, so the ordinary link check covers it.

**Commit that file.** It is what keeps old URLs alive once the move scrolls out
of the history git can see (or when the content is copied into another
repository). It can also be edited by hand, which is the way to record moves
made before the site used a generator - or moves made without git. Moves that
chain (a page moved twice) are collapsed automatically, so every old location
points straight at the current one.

Two things then happen:

- **Old URLs keep working.** For every old location a small page is generated
  in the output that redirects to the document's current location, using a
  `meta refresh` plus a `canonical` link. This needs no server configuration,
  so it works on plain static hosting like Github Pages. If a location is
  filled with new content later on, that real page wins and no redirect is
  generated for it.
- **Stale links in your own sources are repaired.** A link or image in a
  markdown file that points at a moved document is rewritten in the source
  file to point at the new location. The build then reports each one and
  stops, so you can review the changes and commit them - after which the next
  build runs through cleanly:

```
Fixed 1 stale link(s) in 1 file(s) - please review and commit them
Error index.md(28)  Link link to moved document: hp-7470a-plotter/index.md is now at digital-tools/hp-7470a-plotter/index.md (fixed in the source; review and commit it)
```

If the content is not in a git repository nothing breaks; `redirects.tsv` is
then the only source of moves, and a site that has never had one gets no file
at all.

To style the redirect pages, add a `redirect.jte` to `templates/`. It receives
a `RedirectModel` with `getTargetHref()` (the link to the new location,
relative to the old one) and `getTargetTitle()`. Without it a plain built-in
page is used.

## Checking the site before every commit

Because a move is only handled properly when the generator gets to see it, it
is worth running the generator before a commit is accepted rather than finding
out when the site is published. `install-hooks.sh` installs git hooks in a site
repository that do exactly that. Run it from the root of the site repository:

```
sitegenerator/install-hooks.sh
```

It finds the site source (the directory holding `content/` and `templates/`),
records where it and the generator live in the repository's git config, and
installs a `pre-commit` and a `pre-push` hook. From then on:

- A commit that touches the site generates the site first, and is refused when
  the site does not build - a dangling link, a broken template.
- When the generator records a move in `redirects.tsv` or repairs a link in the
  sources, the commit is refused too, listing what it changed, so you review it
  and `git add` it before committing again. A move is therefore always
  committed together with its redirect record and its repaired links.
- Pushing runs the same check over the whole site, catching anything that got
  in with `--no-verify`.
- A commit that does not touch the site is not checked at all.
- The generator jar is built with maven if it is missing or older than its own
  sources, so a freshly cloned or updated submodule needs no separate build.

Moves that are only staged are picked up as well: `git mv` an article and the
very next commit already knows the document moved, rather than reporting every
link to it as broken.

Options:

```
sitegenerator/install-hooks.sh [--repo <dir>] [--site-root <dir>]
                              [--output <dir>] [--force] [--uninstall]
```

An existing hook that this script did not write is never replaced without
`--force`, which keeps it as `<hook>.bak` and puts it back on `--uninstall`.
As always with git hooks, `git commit --no-verify` and `git push --no-verify`
skip them.

## Templates

Templates are [jte](https://jte.gg/) files. `base.jte` is rendered once per
content page and receives a `PageModel` with the rendered page content,
breadcrumb path, page title, and site menu. Anything else under `templates/`
(CSS, images, JS) is copied verbatim into the generated site.
