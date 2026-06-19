## ADDED Requirements

### Requirement: Classification translator plugin registration

The system SHALL register the `"classification"` translator name in the multimethod dispatch.

#### Scenario: Registration exists

- **WHEN** the `steno.translators.classification` namespace is loaded
- **THEN** the `tra/prepare-translation`, `tra/translate`, and `tra/build-references` multimethods SHALL have a method for the `"classification"` dispatch value

### Requirement: Build classification references from corpus

The system SHALL train a classifier per letter from the corpus directory and serialize the trained models.

#### Scenario: Build references for type-1 letters

- **WHEN** `tra/build-references "classification"` is called
- **THEN** the system SHALL walk the corpus directory, read each letter's corpus data, train a k-NN classifier (k=3 by default) using frequency-map feature vectors, and return a map with `:type-0`, `:type-1`, and `:min-refseq` keys

#### Scenario: Build references for type-0 letters

- **WHEN** building type-0 references
- **THEN** the system SHALL follow the same process as type-1 but only for type-0 letter directories

### Requirement: Prepare translation context

The system SHALL load the serialized classification references into the translation context.

#### Scenario: Load from config path

- **WHEN** `tra/prepare-translation "classification"` is called with a config containing `"classification_references"`
- **THEN** the system SHALL load the classification references from the specified path and assoc them into the config under `:classification-references`

### Requirement: Translate numeric sequences using classifiers

The system SHALL classify numeric sequences into letters using the trained classification models.

#### Scenario: Translate type-0 glyph

- **WHEN** translating a type-0 glyph (single letter)
- **THEN** the system SHALL extract the frequency-map feature vector from the cnumseq, run inference with all type-0 classifiers, and return the letter with the highest confidence score

#### Scenario: Translate type-1 glyph sequence

- **WHEN** translating a type-1 glyph (multi-letter sequence)
- **THEN** the system SHALL slide a window across the cnumseq, classify each window with type-1 classifiers, select the highest-confidence letter, advance the window by the matched length, and repeat until the remaining sequence is shorter than `min-refseq`

#### Scenario: Unknown letter fallback

- **WHEN** no classifier produces a confidence above the minimum threshold
- **THEN** the system SHALL return `tra/unknown-letter` ("*") with confidence 0

### Requirement: Feature vector extraction

The system SHALL extract feature vectors from numeric sequences for classifier input.

#### Scenario: Frequency-map features

- **WHEN** extracting features from a cnumseq
- **THEN** the system SHALL compute a frequency map (cnum value → count) normalized to the sequence length, producing a vector of length 256 (one bin per possible cnum value)

### Requirement: Configurable classifier parameters

The system SHALL allow classifier hyperparameters to be configured via `config.yml`.

#### Scenario: k-NN neighbor count

- **WHEN** the config contains a `"classification_k"` setting
- **THEN** the system SHALL use that value as the number of neighbors for k-NN classification

#### Scenario: Default k value

- **WHEN** no `"classification_k"` is specified
- **THEN** the system SHALL default to k=3
