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

# Site structure

The root of the site must contain index.md. This is the root page of the site content.
All images used by index.md must be in that same root folder.

The site has a hierarchical structure. An article mentioned in the root index.md must be
a directory inside that root directory. Inside that directory there must be an index.md
file which is the root article there. If that index.md has sub-articles these are directories
in that subdirectory, etc.

This keeps a page and its resources in a single directory, and every article has its own
subdirectory with resources.




