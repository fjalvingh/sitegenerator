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
- `-Dname=value` (optional, repeatable): defines a variable the documentation
  can use as `${name}`, overriding the site's own `variables.properties`.
  `-D name=value` works as well. See "Variables" below.

Everything else the build needs is in the site itself, so a plain
`-i <site-root>` is the whole command line for a normal build.

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
  variables.properties  what the ${name} variables stand for (optional, commit it)
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

### Variables

`${name}` is replaced by whatever the build says the name stands for, verbatim:

```
The application lives at ${demo}, and its [home page](${demo}HomePage.ui) looks
like this. This is version ${release}.
```

Values come from `variables.properties` in the site root, one `name=value` per
line:

```
# What ${name} stands for; '#' starts a comment line.
demo=https://demo.example.org/demo
release=2.1
```

The value is the rest of the line, trimmed, taken exactly as it is written -
there are no escapes and no continuation lines, and the file is read as UTF-8.
A name may be defined only once. The file is optional; a site using no
variables needs none.

What a variable stands for is a property of the site, not of whoever runs the
generator, which is why it is a committed file rather than an option every run
has to repeat. `-Dname=value` on the command line overrides the file for one
build - that is how the same site is built against another installation of the
application it documents. There is no other source of values: variables are a
build parameter, not page metadata.

One name has a meaning to the generator itself: `demo` is the base url the
`!demo()` tags resolve against, see "Embedded application pages" below. It has
to be an `http://` or `https://` url.

Variables work in the text of a page and in link and image urls (including
their titles, the `<...>` form and `[ref]: url` definitions). A url is expanded
before it is checked, so `[x](${demo}HomePage.ui)` is an external link that is
left alone, while a variable expanding to a path inside `content/` is checked
and rewritten like any other internal link.

A name may contain letters, digits, `.`, `-` and `_`; anything else is not a
variable and is left alone, as is a `${` that is never closed. `${name}` inside
a code span or a code block is left alone too, and `\${name}` is the escape for
a `${` that is meant literally in running text.

A variable that is not defined is an error naming the file and the line, and
the build stops - a page silently showing a bare `${name}`, or a hole where a
url should be, looks like the documentation is broken.

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

### Embedded application pages

Documentation for an application is a lot more convincing when the application
itself is on the page. A line that is nothing but a `!demo()` tag becomes an
iframe showing one page of the live application:

```
!demo(to.etc.domuidemo.pages.HomePage.ui)
!demo(to.etc.domuidemo.pages.HomePage.ui, 1024, 640)
!demo(to.etc.domuidemo.pages.HomePage.ui, 100%)
```

The path in the tag is appended to the `${demo}` base url, with exactly one
slash between the two, so the documentation does not hard-code which
installation it is shown against - a local build, the public demo and a
customer's acceptance server are all the same source with a different `demo`
value. A path that is a full `http(s)://` url is used as it is written.

After the path come the width and then the height, both optional: a bare number
is pixels, anything else is used as a css length (`100%`, `40em`). Leaving one
out keeps its default, and the default size is **1280 by 800**.

The tag renders as

```html
<div class="ui-demo"><iframe class="ui-demo-frame" src="..." style="width: 1280px; height: 800px;" loading="lazy" title="..."></iframe></div>
```

so the stylesheet decides what happens on a screen narrower than the frame -
the wrapping div is there to be given `overflow-x: auto`, which keeps the
application at the size it is being demonstrated at instead of squashing it.

A page that uses `!demo()` in a site that defines no `demo` variable is an
error, naming the file and the line: a silently missing application page looks
like the documentation is broken. A width or height that is not a length is reported the
same way.

### Diagrams

A fenced code block whose info string is `plantuml` is not shown as code: its
content is handed to [PlantUML](https://plantuml.com/), and the diagram that
comes out is written next to the page as an image file and embedded in its
place.

````
```plantuml
Alice -> Bob: Authentication request
Bob --> Alice: Authentication response
```
````

The `@startuml` and `@enduml` lines may be left out — the fence already says
where the diagram starts and ends, and they are added back before PlantUML sees
the source. Writing them yourself is fine too, and is what a diagram needs when
it opens with something else (`@startmindmap`, `@startsalt`).

After the word `plantuml` come the options, all of them optional:

````
```plantuml png title="The login handshake"
```plantuml format=png
````

- the **image format**, as a bare `svg` or `png`, or as `format=svg` /
  `format=png`. The default is **svg**: a diagram is line art, so it stays sharp
  at any zoom and its file is smaller than the bitmap would be.
- **`title="..."`** — what the diagram shows. It becomes the image's `alt` text
  and its tooltip. Without it the `alt` text is "PlantUML diagram".

The image is written into the same output directory as the page that uses it,
named after that page: the second diagram on `data-binding.md` becomes
`data-binding-uml2.svg`. The block renders as

```html
<div class="ui-uml"><img src="data-binding-uml2.svg" alt="..." width="640" height="480"></div>
```

with the diagram's own size in the attributes, scaled down proportionally when
it is wider than 900 pixels, so the page does not jump about while the images
load. The wrapping div is what the stylesheet gets to work with — give it
`overflow-x: auto` to keep a wide diagram scrollable rather than squashed.

A diagram PlantUML cannot parse is an error naming the file and **the line
inside the block** the complaint is about, and it stops the build: publishing a
page with PlantUML's error drawing on it would be worse. An option that is not
an option is reported the same way. A diagram PlantUML *can* parse but complains
about - a deprecated construct, say - stops the build too: PlantUML would draw
its complaint into the image as a banner over the diagram, and a warning nobody
sees until it is on the website is a warning that never gets fixed.

Diagrams are generated once per build no matter how often the same source
appears, but they are the slowest thing the generator does — a site with a
hundred diagrams takes noticeably longer than one without.

PlantUML is bundled (the LGPL build, which is what can be shipped inside the
shaded jar), so nothing needs installing. It uses [Graphviz](https://graphviz.org/)
when `dot` is on the machine and falls back to its own built-in layout engine
when it is not; the two lay class-like diagrams out slightly differently, but
both work.

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

### When nothing recorded the move

Git only reports a rename once it is committed or staged, and only when it
still recognises the file afterwards. A directory moved in a file manager and
not yet added, or a page rewritten in the same step it was moved, is a delete
plus an unrelated new file as far as git is concerned - and then nothing says
where the document went.

The link is repaired anyway when the site itself can answer the question. A
document is known by its *name*: the name of its directory for an article
living in one of its own (`qcriteria` for `data/qcriteria/index.md`), the file
name for anything else. If exactly one document of that name exists anywhere in
the site, that is where the link is pointed:

```
Error data/index.md(11)  Link link to moved document: qcriteria/index.md - the only 'qcriteria' in the site is at ../newsection/qcriteria/index.md (fixed in the source; check it and commit it)
```

That is a guess rather than a recorded fact, which is why the build stops on it
like on any other repair: check it went to the right place before committing.
When the name is not unique nothing is changed at all and the candidates are
listed instead, to be picked from by hand:

```
Error data/index.md(11)  Link link to unknown document: qcriteria/index.md - there is a 'qcriteria' at components/qcriteria/index.md, newsection/qcriteria/index.md, but that is ambiguous so nothing was changed
```

A link to a name the site does not have at all is simply reported as broken,
the way it always was.

### Which renames get a redirect

A site that is still being built has no urls worth keeping: pages get moved
around while its structure is worked out, and turning every one of those moves
into a permanent redirect only preserves urls nobody ever used. A `#moves` line
in `redirects.tsv` says which of the renames git knows about are recorded, and
so get a redirect page:

```
#moves off              ignore all of them, while the site is being restructured
#moves since <commit>   only the renames made after <commit>
#moves all              every rename in the history (the default when the line is absent)
```

The line lives in `redirects.tsv` itself, so there is nothing else to keep in
sync, and it survives the rewrites the generator does of that file.

It decides one thing only: which old **urls** are kept alive. The site's own
links are repaired from every rename git detects whatever it says - a link
pointing at a document's old location is wrong regardless of what was decided
about the outside world, and leaving it to be found at build time is exactly
the reason it is worth fixing while a site is being restructured. The moves
already listed in the file also keep producing their redirect pages whatever
it says.

While it is off every build prints the commit to start from once the structure
has settled down:

```
Move tracking is off ('#moves off' in redirects.tsv): renames repair the links in the sources but get no redirect page. To start recording them from here on, make that line: #moves since 1029404
```

Replacing the line with that `#moves since <commit>` arms the tracking: the
reshuffling before that commit stays ignored, every move after it is recorded
as usual. A commit the repository does not have, or an option that is not one
of the three, fails the build rather than silently collecting nothing.

To turn tracking off for a site that has no `redirects.tsv` yet, create one
containing just the `#moves off` line.

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

The hook needs no options of its own: everything the build needs is in the site
directory it is given. Extra generator options can still be passed in the
`SIGETO_ARGS` environment variable, put where the hook will see it (the
repository's `.git/hooks` environment, or your shell profile):

```
export SIGETO_ARGS='-Drelease=2.2-SNAPSHOT'
```

An existing hook that this script did not write is never replaced without
`--force`, which keeps it as `<hook>.bak` and puts it back on `--uninstall`.
As always with git hooks, `git commit --no-verify` and `git push --no-verify`
skip them.

## The site menu

After all content has been scanned and all page titles are known, the generator
builds a menu tree from the content tree: one `MenuItem` per page, nested the
way the content directories are nested. A page's front matter can adjust its
entry:

```
---
menu:
  title: Short menu title      # instead of the page's own title
  sort: 30                     # where the page sits among its siblings
  hidden: true                 # leave this page (and everything below it) out
---
```

### The order of the items in a level

Inside one level, **the pages that have a sort order come first**, in that
order, and **the pages without one come after them**, sorted by title. Numeric
sort orders are compared as numbers, so `100` sorts after `20`; a sort order
that is not a number is compared as text and sorts after the numeric ones.

A page gets its sort order in one of two ways:

- the **name of its directory starting with a number and a dash**, like
  `20-using-components`. The number is the sort order; it is not part of the
  page's url, which keeps the whole name, and not part of its title.
- the front matter's `menu.sort`, which **wins** over a number in the name.

So a level whose pages are ordered entirely by their directory names needs no
front matter at all, and a single page can still be moved somewhere else in
that order by giving it a `menu.sort`. Pages that have neither simply follow
in alphabetical order.

The menu shown on a page is always the same one, regardless of how it is
rendered: **all top level items**, plus **the children of every item on the
path from the top down to the page being shown** - so a page shows everything
above it, all of its siblings, and its own sub-pages. The rest of the tree
stays collapsed, which keeps the menu of a large site readable.

Both ways of getting that menu into the site are always available - the
generator hands the menu tree to every page *and* writes it to `menu.json` -
so which one is used is entirely up to the site's templates.

### Generated into the page

`base.jte` generates the menu into every page itself, using the tree in the
`PageModel`:

```
@template.menuitem(model, model.getMenuRoot())
```

with `menuitem.jte` walking the tree (see `testsite/templates/menuitem.jte`):

- `model.getMenuRoot()` - the invisible root; its children are the top level
  items, and its own page is the site's root page.
- `item.getSubItemList()`, `item.getTitle()`, `item.hasChildren()`,
  `item.getItemLevel()` - the tree itself.
- `model.mustShowItem(item)` - is this item part of the menu for this page?
- `model.isOpenItem(item)` - is it on the path to this page, i.e. do its
  children need to be shown?
- `model.isCurrentItem(item)` - is it the page being rendered?
- `model.menuHref(item)` - the link to the item's page, relative to the page
  being rendered.

This needs no javascript and works on any static host, at the cost of a copy of
the (visible part of the) menu inside every generated page.

### Built in the browser

The generator also writes the whole menu tree to `menu.json` in the output
root:

```json
{
  "items": [
    {"title": "Amplifiers", "href": "amplifiers/index.html"},
    {"title": "Digital (measurement) tools", "href": "digital-tools/index.html", "items": [
      {"title": "HP 1600A Logic Analyzer", "href": "digital-tools/hp-1600a-logic-analyzer/index.html"}
    ]}
  ]
}
```

The `href`s in it are relative to the site root. A template that wants this
variant emits an empty container plus the script that fills it, instead of the
generated menu:

```
<nav id="ui-menu" class="ui-menu" data-menu='${model.getMenuJsonHref()}'
     data-root='${model.getSiteRootHref()}' data-current='${model.getCurrentPagePath()}'></nav>
<script src='${model.siteURL("js/menu.js")}'></script>
```

- `model.getMenuJsonHref()` - the link to `menu.json` from this page.
- `model.getSiteRootHref()` - the prefix that gets you from this page back to
  the site root, to turn the paths in `menu.json` into links.
- `model.getCurrentPagePath()` - this page's own site root relative path, so
  the script can find it in the menu.

`testsite/templates/js/menu.js` is an example of such a script; it builds the
same menu the generated variant does. `testsite/templates/base.jte` shows how
a template can keep both and switch between them with a single flag. Note that
browsers do not allow
`fetch()` on `file://` urls, so this variant has to be tested through a web
server (`python3 -m http.server` in the output directory will do).

## Templates

Templates are [jte](https://jte.gg/) files. `base.jte` is rendered once per
content page and receives a `PageModel` with the rendered page content,
breadcrumb path, page title, and site menu (see "The site menu" above).
Anything else under `templates/` (CSS, images, JS) is copied verbatim into the
generated site.
