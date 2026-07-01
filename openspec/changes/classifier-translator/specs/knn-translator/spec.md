## ADDED Requirements

### Requirement: Build reference data from corpus samples

Feature: knn-translator
Rule: The knn translator stores individual sample sequences per letter, preserving multi-modal writing style variation.

#### Scenario: Build references collects one sample per corpus entry
- **GIVEN** a corpus containing entries for letter "t" with cnum sequence [136 136 136 136 8]
- **WHEN** build-references is invoked for the knn translator
- **THEN** the reference data includes a sample for letter "t" with sequence [136 136 136 136 8]

#### Scenario: Build references collects multiple samples per letter
- **GIVEN** a corpus containing two entries for letter "t":
  - Entry 1 with sequence [136 136 136 8]
  - Entry 2 with sequence [128 128 128 8]
- **WHEN** build-references is invoked for the knn translator
- **THEN** the reference data includes both samples for letter "t"

#### Scenario: Build references computes min-len and max-len per letter
- **GIVEN** a corpus containing samples for letter "t" with lengths 4, 5, and 7
- **WHEN** build-references is invoked for the knn translator
- **THEN** the reference data for "t" includes min-len 4 and max-len 7

#### Scenario: Build references filters unknown-letter entries
- **GIVEN** a corpus entry mapped to the unknown-letter placeholder "@"
- **WHEN** build-references is invoked for the knn translator
- **THEN** the entry is excluded from the reference data

### Requirement: Translate cnum sequences using k-NN classification

Feature: knn-translator
Rule: The translator classifies a glyph's cnum sequence by finding the k nearest neighbors from reference samples using Hamming distance with sliding window, then applies distance-weighted voting.

#### Scenario: Translation with exact match returns the correct letter
- **GIVEN** reference data where letter "t" has sample [136 136 136 136 8]
- **WHEN** translating the sequence [136 136 136 136 8]
- **THEN** the result is "t"

#### Scenario: Translation uses sliding window for variable-length sequences
- **GIVEN** reference data where letter "t" has a sample [136 136 136 136 8]
- **WHEN** translating a longer input sequence that contains the sample pattern
- **THEN** the translator slides the shorter sequence across the longer one to find the best match position

#### Scenario: Distance-weighted voting selects the letter with highest total match score
- **GIVEN** reference data:
  - Letter "t" with sample [136 136 136 8] (match score 0.9)
  - Letter "d" with sample [128 128 128 8] (match score 0.8)
  - Letter "t" with sample [136 136 128 8] (match score 0.7)
- **WHEN** k=3 and the input is [136 136 132 8]
- **THEN** "t" wins with total weight 1.6 (0.9 + 0.7) vs "d" at 0.8

#### Scenario: Translation with fewer samples than k degrades gracefully
- **GIVEN** reference data with only 2 samples total
- **WHEN** k=3 and translating any sequence
- **THEN** the translator uses all available samples (effectively 2-NN)

### Requirement: Support configurable k value

Feature: knn-translator
Rule: The k parameter controls how many nearest neighbors are considered during voting.

#### Scenario: Default k value is 3
- **GIVEN** no knn_k is specified in the configuration
- **WHEN** the knn translator is initialized
- **THEN** the effective k value is 3

#### Scenario: Configurable k value from context
- **GIVEN** knn_k is set to 5 in the context
- **WHEN** the knn translator translates a sequence
- **THEN** it considers 5 nearest neighbors for voting
