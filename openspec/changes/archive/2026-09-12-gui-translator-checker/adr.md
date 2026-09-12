# ADR Review Manifest

- Status: completed
- Review date: 2026-08-22

## Review Summary

ADR review completed for this change. No prior ADRs existed (`adr/` was absent), so the supersession graph is empty and the sequence starts at 0001. Two decisions from design.md met the durable bar (long-term commitment affecting changes beyond this one) and are recorded as repository-level ADRs; remaining design decisions (single-namespace pure-core layout, pipeline reuse without extraction, synchronous batch iteration, best-effort Emacs bridge) are tactical implementation details below the ADR bar.

## In-Force ADRs Reviewed

- None - `adr/` has no in-force ADRs (directory did not exist before this change).

## New Durable ADRs Created

- [adr/0001-tkinter-as-gui-toolkit.md](../../../adr/0001-tkinter-as-gui-toolkit.md) - tkinter chosen as steno's GUI toolkit (zero-dependency constraint, Tk 8.6 verified).
- [adr/0002-corpus-format-preservation-contract.md](../../../adr/0002-corpus-format-preservation-contract.md) - all corpus writers must produce byte-compatible entries; checker strips translator annotations, pins row/column to 0, treats same-position save as replacement.
