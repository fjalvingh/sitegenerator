# Simple site generator

TBD

# Markdown notes

## Front matter
Pages can have fron matter in YAML format, using the usual format:
```aiignore
---
title: This is a title
menulocation: 10.5
---
# Page title
....
```

## Headers inside documents

Each markdown page gets its page title by default from the first h1 heading 
in the document. This means that you should _not_ have more than one such
header in your document.
If you use the [TOC] macro then it is important to realise that h1 headers are
not shown by that macro, to prevent linking to the page title.

## The toc macro
You can generate a TOC by adding the TOC macro, as follows:
```aiignore
[TOC hierarchy]
```
For options that can be used see the documentation of Flexmark.

