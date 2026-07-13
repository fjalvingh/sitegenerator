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
  `Content` after the scan/check phase (`Content.complete()`).
- `PageModel.java` — the object handed to `base.jte`; exposes rendered HTML,
  breadcrumbs, menu, and `siteURL()` for computing relative links from any
  page depth.
- `LinkUpdater.java` — rewrites `.md` links in the AST to point at the
  generated `.html` targets.
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
- `Util.java` — file IO helpers (copy, empty-dir, string IO).

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
- Don't hand-edit anything under `_output/` directories — it's generated.
