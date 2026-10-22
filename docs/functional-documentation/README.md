# Functional Documentation

Source of the functional documentation of the Keyple Demo Ticketing
([`index.adoc`](index.adoc)), written in AsciiDoc with the graphic charter of
the CNA documents. Its annexes are a technical reference for the developers:

- annex A, "Card data model": supported card application, structures of the records, data types, and layout of the
  cards, implemented by the common library (`src/common`);
- annex B, "Card procedures": procedures executed on the card, step by step, the step numbers being referenced by the
  comments of the code.

A change of the data model or of a procedure is made both in the annex and in the code.

## Publication

At each release of the repository, the CI (`.github/workflows/release-tag.yml`) generates the document and attaches it
to the release:

- `kdt-functional-documentation-YY.MM.DD.pdf`
- `kdt-functional-documentation-YY.MM.DD.html`, standalone (fonts, images, and diagrams embedded; only the icons are
  loaded from the Font Awesome CDN)

As the applications of the demo, the version of the document is the release tag (YY.MM.DD), its date being the date
of this tag; outside the releases, the version is `99.99.99` and the date is the build date. The document reference
(`261021-FD-KeypleDemoTicketing`, from the date of its first publication) is fixed: it identifies the document,
whatever its version. The CI of the pull requests and of the main branch (`.github/workflows/ci-main-and-pr.yml`)
generates the same files, available as artifacts of the workflow run for review.

## Local build

With [Asciidoctor](https://asciidoctor.org/), Asciidoctor PDF, and Asciidoctor Diagram (PlantUML, Java required), or
the `asciidoctor/docker-asciidoctor` image used by the CI, from this directory:

```bash
asciidoctor -r asciidoctor-diagram -D generated index.adoc
asciidoctor-pdf -r asciidoctor-diagram -D generated index.adoc
```

All the generated files (HTML, PDF, diagrams, cache) are placed in the `generated` directory, ignored by Git.

## Structure

```
docs/functional-documentation/
├── index.adoc                      # Document
├── diagrams/                       # PlantUML sources of the diagrams, generated at rendering
└── assets/                         # CNA graphic charter
    ├── pdf-theme.yml               # Theme of the PDF output
    ├── docinfo.html                # Styles of the HTML output
    ├── title-page-background.svg   # Background of the PDF title page
    ├── fonts/                      # Montserrat and JetBrains Mono (TTF for the PDF, base64 WOFF2 for the HTML)
    ├── layout/                     # Colophon (link, contact), favicon and icons
    ├── legal/                      # License of the document (BSD 3-Clause, license of the repository)
    └── logos/                      # Calypso logo (title page, HTML header, PDF page header)
```

The `pdf-theme.yml`, `docinfo.html`, `title-page-background.svg`, `fonts`, and `layout` files are copies of the shared
resources of the CNA documents (CNA documentation toolchain): keep them in sync with their source. Two adaptations of
`pdf-theme.yml` are specific to this document: the `green` and `olive` roles (icons of the terminal table), and the
license of the document in the page footer, instead of the reproduction prohibition of the CNA specifications. The
`logos` and `legal` partials are specific to this document.

## Preview in the IDE

The IDE previews (for example, the AsciiDoc plugin of IntelliJ IDEA) do not load the fonts embedded in the HTML output:
they use the fonts installed on the system. To preview the document with the fonts of the CNA charter, install the
Montserrat and JetBrains Mono fonts of the `assets/fonts` directory. The generated HTML and PDF files are not affected.
