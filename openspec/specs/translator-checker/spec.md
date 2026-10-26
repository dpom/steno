# Capability: translator-checker

## Purpose

Interactive validation of automatic translation proposals on already-extracted
glyph images: the reviewer sees what each translator proposes sign by sign,
corrects the proposals, and files confirmed signs into the curated corpus
(ADR-0001, ADR-0002).

## Requirements

### Requirement: Local web server lifecycle
Feature: The `check` action SHALL serve the translator checker from a local web server

The checker SHALL run in the existing `check` process and SHALL serve its page and API from a loopback-only HTTP server. The server SHALL default to port 8765, SHALL accept a `--port` override, SHALL open the default browser unless `--no-browser` is given, and SHALL fall back to an ephemeral port when the requested port is unavailable.

#### Scenario: Starting the checker
- **GIVEN** translators are configured for the project
- **WHEN** the user starts the check action with a glyph image or directory
- **THEN** the checker serves its page on the loopback interface
- **AND** the default browser opens the checker URL

#### Scenario: Choosing a different port
- **GIVEN** the reviewer wants the checker on a specific local port
- **WHEN** the user starts the check action with `--port <number>`
- **THEN** the checker serves its page on that port

#### Scenario: Requested port is already in use
- **GIVEN** another process is listening on the requested port
- **WHEN** the user starts the check action
- **THEN** the checker serves its page on an available ephemeral port
- **AND** the checker reports the actual URL

#### Scenario: Starting without opening a browser
- **GIVEN** the reviewer wants to open the checker page manually
- **WHEN** the user starts the check action with `--no-browser`
- **THEN** the checker reports its URL
- **AND** no browser is launched

#### Scenario: Local-only access
- **GIVEN** the checker server is running
- **WHEN** the server accepts connections
- **THEN** it accepts them only from the local machine

### Requirement: Check CLI action
Feature: The `check` action SHALL launch interactive translator-proposal checking from the command line

The `check` action SHALL open an interactive validation session in a browser using the same configuration conventions as the other actions: config file, corpus output directory, and translator selection are overridable with flags. The input SHALL be optional; without it, the page opens ready to accept a glyph image or directory path.

#### Scenario: Opening a validation session for one glyph image
- **GIVEN** translators are configured for the project
- **WHEN** the user starts the check action with a single glyph image
- **THEN** a browser page opens presenting that glyph for review

#### Scenario: Restricting translators and output location
- **GIVEN** several translators are available and a dedicated corpus directory is wanted
- **WHEN** the user starts the check action selecting particular translators and an output corpus directory
- **THEN** the proposals shown come only from the selected translators
- **AND** validated signs are later filed under that corpus directory

#### Scenario: Starting without an input
- **GIVEN** translators are configured for the project
- **WHEN** the user starts the check action without an input
- **THEN** the browser page opens with an empty review queue
- **AND** the page offers a path field for choosing a glyph image or directory

### Requirement: Glyph input acceptance
Rule: A review session works on already-extracted glyph images, singly or as a directory batch. The checker SHALL accept both forms of input without extraction.

#### Scenario: Loading a single glyph image
- **GIVEN** a glyph image file exists
- **WHEN** the reviewer enters its path in the page and opens it
- **THEN** the glyph is converted into its sequence of signs ready for review

#### Scenario: Loading a directory of glyph images
- **GIVEN** a directory contains several glyph images
- **WHEN** the reviewer enters its path in the page and opens it
- **THEN** the images form a review queue in name order
- **AND** reviewing advances through the queue one glyph at a time

### Requirement: GUI screen layout
Feature: The checker SHALL be a single browser page organised as three stacked panels

The checker page SHALL follow a fixed layout: a glyph display on top, the lsign review table in the middle, and controls and status at the bottom. It SHALL NOT require a native desktop toolkit.

#### Scenario: Top panel shows the glyph image
- **GIVEN** a glyph is loaded in the checker
- **WHEN** the page renders
- **THEN** the top panel shows a display frame labelled "gliph image"
- **AND** that frame displays the current glyph image

#### Scenario: Middle panel holds the lsigns table
- **GIVEN** a processed glyph produced one or more lsigns
- **WHEN** the page renders
- **THEN** the middle panel contains a table named "lsigns"
- **AND** the table has exactly 7 columns: Lsign image (image thumbnails), start, freq (char + float score), diff (char + float score), knn (char + float score), select (input char), and save (checkbox)
- **AND** each lsign of the current glyph occupies one table row

#### Scenario: Bottom panel holds the controls
- **WHEN** the page renders
- **THEN** the bottom panel offers SAVE, PREV, NEXT, and DELETE controls
- **AND** the bottom panel shows a status line for processing, boundary, and skip messages

### Requirement: Per-lsign translator proposal display
Feature: Reviewers SHALL see what every translator proposes before trusting any of them

For the current glyph, the checker SHALL show the glyph image and one review row per sign. Each row shows the sign itself, every configured translator's candidate letters with their match probabilities, and the combined best-match proposal.

#### Scenario: Reviewing proposals for a multi-sign word
- **GIVEN** a glyph image containing several signs and at least two translators configured
- **WHEN** the checker processes the glyph
- **THEN** one review row appears per sign
- **AND** each row shows that sign's image
- **AND** each row shows every translator's candidate letters together with their match probabilities

#### Scenario: Best match pre-selected for correction
- **GIVEN** a processed glyph whose first sign was jointly proposed as the letter "t"
- **WHEN** the reviewer inspects that sign's row
- **THEN** the row's letter selector is pre-filled with the best-match proposal

### Requirement: Human correction of proposals
Rule: Nothing enters the corpus without the reviewer confirming the true letter for each sign; every saved row MUST carry such a confirmed letter.

#### Scenario: Correcting a wrong proposal
- **GIVEN** a review row pre-filled with a wrong letter proposal
- **WHEN** the reviewer selects or types the correct letter in that row
- **THEN** the row holds the corrected letter instead of the proposal

#### Scenario: Leaving a sign out of the corpus
- **GIVEN** a review row the reviewer does not want to store
- **WHEN** the reviewer leaves that row's save checkbox unchecked and saves
- **THEN** that sign produces no corpus entry

#### Scenario: Choosing a letter that has no corpus folder yet
- **GIVEN** the corpus has no folder for the letter "ţ"
- **WHEN** the reviewer types "ţ" in a row and saves
- **THEN** the confirmed sign is stored under a newly created "ţ" folder

### Requirement: Batch navigation
The checker SHALL support moving back and forth inside the loaded review queue and loading a different input without leaving the application.

#### Scenario: Moving to the next glyph in a directory batch
- **GIVEN** a directory batch is open and the current glyph has been reviewed
- **WHEN** the reviewer moves forward
- **THEN** the next queued glyph becomes current with its rows displayed

#### Scenario: Returning to the previous glyph
- **GIVEN** the reviewer has moved forward in the queue
- **WHEN** the reviewer moves backward
- **THEN** the previous glyph becomes current again

#### Scenario: Opening another input mid-session
- **GIVEN** a validation session is running
- **WHEN** the reviewer enters a different glyph image or directory path in the page and opens it
- **THEN** the review queue is replaced with the chosen input without restarting the application

### Requirement: Curated corpus saving
Rule: Saved entries SHALL be indistinguishable from corpus entries produced by automatic growth, except the letter is human-confirmed.

#### Scenario: Saving reviewed rows
- **GIVEN** a reviewed glyph whose rows carry confirmed letters and checked save boxes
- **WHEN** the reviewer presses SAVE
- **THEN** every confirmed sign whose save box is checked is written as a corpus entry under its confirmed letter's folder
- **AND** each entry carries exactly the standard corpus fields (ltype, lineseq, fileimage, row, column, pos) with no translator annotations
- **AND** each entry's file name follows the corpus naming convention for its source image and sign position

#### Scenario: Saving twice replaces rather than duplicates
- **GIVEN** a sign already saved for the current source image and position
- **WHEN** the reviewer corrects its letter and saves again
- **THEN** the existing corpus entry for that position is replaced rather than duplicated
