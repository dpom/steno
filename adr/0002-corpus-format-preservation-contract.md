# 0002 - Preserve corpus entry format across all writers

## Status

Accepted

## Date

2026-08-22

## Context and Problem Statement

Corpus entries under `resources/corpus/<letter>/` are consumed by every translator's reference builder (`build-references`). Today the only writer is automatic growth (`action-make-corpus`), which stores each lsign with exactly the keys `{:ltype :lineseq :fileimage :row :column :pos}` and file name `<image>-<row>-<col>-<pos>.edn`. The gui-translator-checker change introduces a second writer whose saved entries must remain indistinguishable to downstream readers, or references degrade depending on provenance.

## Considered Options

- Contract: checker writes byte-compatible corpus entries (same keys, same naming convention); translator annotations are stripped on save.
- Divergent format: checker adds provenance/quality metadata (e.g., confirmed-by, match scores) for curation value.
- Separate curated store: keep human-validated signs in a parallel directory until some merge step.

## Decision Outcome

Chosen option: "Contract: checker writes byte-compatible corpus entries", because readers stay agnostic to who wrote an entry, avoiding migration of existing references and keeping the corpus uniform. Since checker inputs are pre-extracted glyph images without page context, `row`/`column` pin to 0 and `pos` is the wsign index; re-saving the same source position replaces the existing entry (deliberate idempotent curation, not duplication).

### Consequences

- Good, because freq/diff/knn reference building works unchanged over mixed-provenance corpora
- Good, because rollback of the checker cannot corrupt or fork the corpus
- Bad, because checker-sourced entries lose positional meaning (`row`/`column` are always 0)
- Bad, because silent overwrite of an existing entry is possible and accepted as intended replacement
