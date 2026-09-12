# ADR Review Manifest

- Status: completed
- Review date: 2026-09-12

## Review Summary

ADR review completed for this change. The in-force ADR set was reviewed: ADR-0001 (tkinter as GUI toolkit) and ADR-0002 (corpus format preservation contract). Neither is affected or superseded by this change. The decisions here — threaded glyph loading with Tk confined to the main thread, the `:busy?` guard, the status bar with indeterminate progress, boundary/skip notifications, and Quit-joins-worker — are tactical implementation details inside the existing checker container under the ADR-0001 thin-UI-over-pure-core pattern. Consistently with the parent change (gui-translator-checker), which recorded UI-layer iteration/threading choices as below the ADR bar, none of these new choices meet the durable-decision bar.

## In-Force ADRs Reviewed

- [adr/0001-tkinter-as-gui-toolkit.md](../../../adr/0001-tkinter-as-gui-toolkit.md) - tkinter stays the GUI toolkit; this change adds no new dependency and does not alter the toolkit choice.
- [adr/0002-corpus-format-preservation-contract.md](../../../adr/0002-corpus-format-preservation-contract.md) - corpus format untouched; the busy-feedback change only affects the check window's interaction layer.

## New Durable ADRs Created

- None - no new repository-level ADR files were created; no durable architectural decisions were introduced by this change.