## ADDED Requirements

### Requirement: Check CLI action
Feature: Interactive translator-proposal checking launched from the command line

The `check` action opens an interactive validation session using the same configuration conventions as the other actions: config file, corpus output directory, and translator selection are overridable with flags.

#### Scenario: Opening a validation session for one glyph image
- **GIVEN** translators are configured for the project
- **WHEN** the user starts the check action with a single glyph image
- **THEN** an interactive window opens presenting that glyph for review

#### Scenario: Restricting translators and output location
- **GIVEN** several translators are available and a dedicated corpus directory is wanted
- **WHEN** the user starts the check action selecting particular translators and an output corpus directory
- **THEN** the proposals shown come only from the selected translators
- **AND** validated signs are later filed under that corpus directory

### Requirement: Glyph input acceptance
Rule: A review session works on already-extracted glyph images, singly or as a directory batch.

#### Scenario: Loading a single glyph image
- **GIVEN** a glyph image file exists
- **WHEN** the user opens it in the checker
- **THEN** the glyph is converted into its sequence of signs ready for review

#### Scenario: Loading a directory of glyph images
- **GIVEN** a directory contains several glyph images
- **WHEN** the user opens the directory in the checker
- **THEN** the images form a review queue in name order
- **AND** reviewing advances through the queue one glyph at a time

### Requirement: Per-lsign translator proposal display
Feature: Seeing what every translator proposes before trusting any of them

For the current glyph, the checker shows the glyph image and one review row per sign. Each row shows the sign itself, every configured translator's candidate letters with their match probabilities, and the combined best-match proposal.

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
Rule: Nothing enters the corpus without the reviewer confirming the true letter for each sign.

#### Scenario: Correcting a wrong proposal
- **GIVEN** a review row pre-filled with a wrong letter proposal
- **WHEN** the reviewer selects or types the correct letter in that row
- **THEN** the row holds the corrected letter instead of the proposal

#### Scenario: Leaving a sign out of the corpus
- **GIVEN** a review row the reviewer does not want to store
- **WHEN** the reviewer empties that row's letter selector and saves
- **THEN** that sign produces no corpus entry

#### Scenario: Choosing a letter that has no corpus folder yet
- **GIVEN** the corpus has no folder for the letter "ţ"
- **WHEN** the reviewer types "ţ" in a row and saves
- **THEN** the confirmed sign is stored under a newly created "ţ" folder

### Requirement: Batch navigation
The checker supports moving back and forth inside the loaded review queue and loading a different input without leaving the application.

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
- **WHEN** the reviewer opens a different glyph image or directory from the menu
- **THEN** the review queue is replaced with the chosen input without restarting the application

### Requirement: Curated corpus saving
Rule: Saved entries are indistinguishable from corpus entries produced by automatic growth, except the letter is human-confirmed.

#### Scenario: Saving reviewed rows
- **GIVEN** a reviewed glyph whose rows carry confirmed letters
- **WHEN** the reviewer saves
- **THEN** every confirmed sign is written as a corpus entry under its confirmed letter's folder
- **AND** each entry carries exactly the standard corpus fields (ltype, lineseq, fileimage, row, column, pos) with no translator annotations
- **AND** each entry's file name follows the corpus naming convention for its source image and sign position

#### Scenario: Saving twice replaces rather than duplicates
- **GIVEN** a sign already saved for the current source image and position
- **WHEN** the reviewer corrects its letter and saves again
- **THEN** the existing corpus entry for that position is replaced rather than duplicated

### Requirement: Best-effort Emacs image display
Feature: Optional large-view side channel into Emacs

The checker can push glyph and sign images to a running Emacs server for closer inspection; Emacs availability never affects the checking workflow.

#### Scenario: Viewing an image in Emacs
- **GIVEN** an Emacs server is reachable
- **WHEN** the reviewer asks to view a glyph or sign image
- **THEN** the image appears in Emacs

#### Scenario: Working without Emacs
- **GIVEN** no Emacs server is running
- **WHEN** the reviewer asks to view an image
- **THEN** the request fails silently
- **AND** the checker remains fully usable
