# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

sigeto is a small Java static site generator: Markdown in `content/` +
[jte](https://jte.gg/) templates in `templates/` -> static HTML in an output
directory. See `README.md` for the user-facing docs (CLI usage, site layout,
supported Markdown extensions). Read that first for behavior; this file is
about working on the generator's own source.

## Build & run

```
mvn clean package
java -jar target/sitegen-jar-with-dependencies.jar -i testsite -o testsite/_output
```

- Compiles with the Eclipse compiler (ecj) via `maven-compiler-plugin`, JDK 21.
- `testsite/` is a real example site (checked into the repo) — use it to
  exercise changes end-to-end rather than inventing a throwaway fixture site.
  `testsite/_output` is generated output; don't hand-edit it.
- No test suite currently exists (no `src/test`). Verify changes by running
  the generator against `testsite/` and inspecting the produced HTML.

## Code layout

- `Main.java` — CLI entry point (args4j options), orchestrates the whole
  pipeline: scan content -> check markdown -> render each item -> copy
  template assets.
- `Content.java` / `ContentLevel.java` / `ContentItem.java` — the in-memory
  model of the site. `Content` scans `content/` recursively into a tree of
  `ContentLevel`s (directories) containing `ContentItem`s (files). Directories
  named `yyyymmdd` become blog entries (`ContentType.Blog`).
- `MarkdownChecker.java` — wraps the commonmark `Parser`/`HtmlRenderer`,
  splits YAML front matter from Markdown, validates internal links/images
  against the `Content` model (build fails on dangling links), and extracts
  the page title from the first `#` heading.
- `Menu.java` / `MenuItem.java` — builds the site navigation tree from
  `Content` after the scan/check phase (`Content.complete()`), one `MenuItem`
  per page. Which part of that tree a page shows is decided by `PageModel`
  (`mustShowItem`/`isOpenItem`/`isCurrentItem`), not by the tree itself, so
  both menu variants show exactly the same menu.
- `MenuJsonWriter.java` — writes `menu.json` (the whole menu tree, with site
  root relative links) at the output root, for templates that build their menu
  in the browser. It is always written: the generator offers both the tree and
  the json, and the templates decide which one they use. See the "The site
  menu" section of `README.md`.
- `PageModel.java` — the object handed to `base.jte`; exposes rendered HTML,
  breadcrumbs, menu, and `siteURL()` for computing relative links from any
  page depth.
- `LinkUpdater.java` — rewrites `.md` links in the AST to point at the
  generated `.html` targets.
- `MoveMap.java` / `GitMoveScanner.java` / `LinkFix.java` /
  `SourceLinkFixer.java` / `RedirectWriter.java` — document move tracking.
  `GitMoveScanner` asks git for the renames below `content/` (committed and
  staged), `MoveMap` merges the document ones into the checked-in
  `<siteRoot>/redirects.tsv` and collapses move chains, `RedirectWriter` emits a meta-refresh page at each old URL,
  and `SourceLinkFixer` repairs links to moved documents in the `.md` sources
  (which is reported as an error so the build stops and the change gets
  committed). See the "Moved documents" section of `README.md`.
- `MdImgRenderer.java` — custom image node renderer (resolves/copies image
  resources relative to the current page).
- Custom commonmark extensions, one per subpackage, each following the same
  `Parser.ParserExtension` + `HtmlRenderer.HtmlRendererExtension` shape:
  - `tocextension/` — `[TOC ...]` table-of-contents macro.
  - `notifications/` — `!`-prefixed callout/admonition blocks.
  - `emojis/` — `:shortcode:` emoji, data-driven from
    `src/main/resources/emoji/emoji.csv`.
  - `tables/` — GFM table rendering.
  - `blogextension/` — blog-entry-specific parsing/rendering.
  - `demos/` — `!demo(path)` iframe showing a page of the live application.
  - `variables/` — `${name}` variables, defined with `-D` on the command line.
  - `plantuml/` — ` ```plantuml ` fenced blocks, rendered to an svg/png file
    next to the page by the bundled PlantUML. This one hooks in through a
    `PostProcessor` rather than a block parser (commonmark already knows how to
    find a fenced block), and its renderer half is created per page because it
    writes files into that page's output directory; `PlantumlRenderCache` is
    shared between the check and the render phase so each distinct diagram is
    generated only once.
- `Util.java` — file IO helpers (copy, empty-dir, string IO).
- `install-hooks.sh` + `githooks/sigeto-check.sh` — installs and implements the
  `pre-commit` / `pre-push` hooks for a *site* repository (not this one), which
  generate the site and refuse the commit when it does not build or when the
  generator had to record a move or repair a link. Configured through the site
  repository's git config (`sigeto.home`, `sigeto.siteroot`, `sigeto.output`).

## Conventions to follow

- Member field naming uses the `m_` prefix (e.g. `m_itemMap`); this is
  existing style throughout `src/main/java`, keep new fields consistent.
- Nullability is annotated explicitly with `org.eclipse.jdt.annotation`
  (`@NonNull` / `@Nullable`) — add these on new public fields/params/returns
  the same way existing code does, don't rely on default-non-null.
- Errors that should abort the build with a user-facing message use
  `MessageException`; don't throw raw `RuntimeException` for expected
  validation failures (e.g. missing content root, dangling link).
- Non-fatal validation problems accumulate as `Message` objects in an
  `errorList` and are all reported together (see `MarkdownChecker.scanContent`
  and `Main.run`) rather than failing on the first error — follow this
  pattern when adding new checks.
- New commonmark features should follow the existing extension pattern (own
  subpackage, `*Extension` implementing `ParserExtension`/
  `HtmlRendererExtension`, registered in `MarkdownChecker`'s `m_extList`) not
  be bolted directly onto `MarkdownChecker`.
- Html tag attributes for `HtmlWriter.tag()` must be built with
  `Util.attributes(name, value, ...)`, not `Map.of(...)`: `Map.of` randomizes
  its iteration order per JVM run, which makes the generated html differ
  between builds of the same site.
- Don't hand-edit anything under `_output/` directories — it's generated.
