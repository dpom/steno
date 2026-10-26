# ADR Review Manifest

- Status: completed
- Review date: 2026-10-25

## Review Summary

ADR review completed for this change. ADR-0001 selected tkinter as the checker's GUI toolkit; this change intentionally replaces that UI with a local browser page. ADR-0002 remains in force and is honored unchanged: the web checker still saves through `save-row!` and keeps corpus entries byte-compatible with automatic growth. One new durable ADR records the UI technology decision and supersedes ADR-0001.

## In-Force ADRs Reviewed

- [adr/0001-tkinter-as-gui-toolkit.md](../../../adr/0001-tkinter-as-gui-toolkit.md) - reviewed and intentionally superseded by ADR-0003.
- [adr/0002-corpus-format-preservation-contract.md](../../../adr/0002-corpus-format-preservation-contract.md) - reviewed and remains in force; no change to corpus writing.

## New Durable ADRs Created

- [adr/0003-web-based-checker-ui.md](../../../adr/0003-web-based-checker-ui.md) - the checker uses a loopback-only web UI served by the Python standard library, superseding ADR-0001.
