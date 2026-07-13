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
    css/, img/, ...     copied verbatim into the output root
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
image points at a file that doesn't exist in `content/`. Directories detected
as blog entries (see above) get additional blog-specific rendering (entry
listing, dates) via the blog extension.

## Templates

Templates are [jte](https://jte.gg/) files. `base.jte` is rendered once per
content page and receives a `PageModel` with the rendered page content,
breadcrumb path, page title, and site menu. Anything else under `templates/`
(CSS, images, JS) is copied verbatim into the generated site.
