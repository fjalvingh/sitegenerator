# sigeto Markdown for VS Code

The VS Code Markdown preview renders plain GitHub-flavored Markdown, so it
shows sigeto's `~subscript~` and `^superscript^` as the literal tildes and
carets you typed. This extension teaches the preview those two, so what you see
while writing is what the generated site will show.

It is a handful of lines of plain JavaScript with no dependencies and no build
step: the preview's own markdown-it instance is extended through
`extendMarkdownIt`, and `subsup.js` implements the rule against markdown-it's
`scanDelims()` — markdown-it's implementation of the same flanking rules
commonmark-java applies to the generator's delimiter processors. Both sides
therefore agree on the cases that matter:

| written                    | preview and site      |
|----------------------------|-----------------------|
| `H~2~O`                    | H<sub>2</sub>O        |
| `2^16^`                    | 2<sup>16</sup>        |
| `170005~oct~`              | 170005<sub>oct</sub>  |
| `~~struck~~`               | struck through        |
| `under ~/opt ... ~/opt/x`  | left exactly as typed |
| `press ^C or ^D`           | left exactly as typed |
| `~two words~`              | left exactly as typed |

Two differences remain, both harmless: the preview renders strikethrough as
`<s>` where the generator uses `<del>` (markdown-it and commonmark-java have
always differed there), and a pathological run of markers like `2^16^^2^` is
grouped differently. Neither shows up in real text.

## Installing

VS Code only loads extensions from its own extensions directory, so link this
one into it:

```bash
ln -s "$(pwd)/vscode-sigeto" ~/.vscode/extensions/sigeto-markdown
```

Run that from the generator checkout, then restart VS Code (or run *Developer:
Reload Window*). "sigeto Markdown" then shows up under the installed
extensions, and the preview picks up the new syntax.

To remove it again, delete the link and reload:

```bash
rm ~/.vscode/extensions/sigeto-markdown
```

## What it does not do

Only the *preview* learns the syntax; the editor's own syntax highlighting
still colors `~x~` as ordinary text. That would need a grammar injection, which
is a lot more machinery for a bit of color.

The other sigeto extensions (notifications, figures, `[TOC]`, `!demo()`,
```plantuml, `${variables}`, `:emoji:`) are not implemented here either — the
preview shows their source. Only sub- and superscript are silent enough about
being unsupported to be worth teaching it.
