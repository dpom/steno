# Capability: checker-busy-feedback

## Purpose

User feedback and input handling for the translator checker while glyphs are
being converted and translated: the window signals activity, ignores replays
during processing, and tells the reviewer when navigation reaches a queue
boundary or an unreadable glyph is skipped.

## Requirements

### Requirement: Busy state blocks input during glyph processing
Feature: While the checker converts and translates the current glyph, the window SHALL indicate activity and SHALL ignore further user actions

#### Scenario: Processing activity is shown
- **GIVEN** the reviewer opens a glyph whose conversion and translation take noticeable time
- **WHEN** the checker starts processing it
- **THEN** the window shows an indeterminate progress bar
- **AND** the status label shows the glyph being processed and its position in the review queue

#### Scenario: No replay of a double-click during processing
- **GIVEN** the reviewer has pressed Next and the checker is still processing the glyph
- **WHEN** the reviewer presses Next, Previous, Delete, Save, or an Open command again
- **THEN** the repeated action has no effect for as long as processing is in progress

#### Scenario: The active review returns to the prior state
- **GIVEN** a glyph finished processing after a no-effect repeat
- **WHEN** the processing completes
- **THEN** the window shows only the glyph that was being processed
- **AND** the progress bar stops
- **AND** the status label reports readiness

### Requirement: Queue boundary notification
The checker SHALL tell the reviewer when navigation reaches either end of the review queue instead of silently doing nothing.

#### Scenario: Asking for the next glyph after the last one
- **GIVEN** the reviewer is reviewing the last glyph of the review queue
- **WHEN** the reviewer presses Next
- **THEN** the checker notifies the reviewer that no more glyphs remain in the queue

#### Scenario: Asking for the previous glyph before the first one
- **GIVEN** the reviewer is reviewing the first glyph of the review queue
- **WHEN** the reviewer presses Previous
- **THEN** the checker notifies the reviewer that this is the first glyph

### Requirement: Unreadable glyphs skip forward
The checker SHALL advance past a glyph that cannot be converted and SHALL say so, instead of silently showing an empty review.

#### Scenario: Skipping an unreadable glyph
- **GIVEN** the reviewer moves onto a glyph image that cannot be read or yields no sign
- **WHEN** the checker processes it
- **THEN** the checker moves on to the next loadable glyph in the queue
- **AND** the status label records the skipped glyph

#### Scenario: Skipping advances one glyph at a time
- **GIVEN** the reviewer is reviewing a queue where later glyphs are readable
- **WHEN** an unreadable glyph is skipped forward
- **THEN** each skipped skip consumes exactly one queue position
- **AND** the next readable glyph becomes current

### Requirement: Navigation in progress
The checker SHALL keep the review queue state and the screen consistent while a glyph is being loaded.

#### Scenario: Opening another input mid-processing is deferred
- **GIVEN** the checker is processing a glyph
- **WHEN** the reviewer issues an Open image or Open directory command
- **THEN** the command has no effect until the current processing finishes