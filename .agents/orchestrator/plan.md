# Orchestration Plan - Nail Feature Extractor Optimization

## Goal
Improve the visual analysis heuristics in `NailFeatureExtractor.kt` to detect pale nail beds, white spots, vertical ridges, and dark edges. Create an automated test harness to verify correctness with 100% accuracy and ensure average extraction time is under 50ms.

## Architecture & Scope
- **Target File**: `app/src/main/java/com/example/nailnutri/analysis/GemmaAnalyzer.kt` (contains `NailFeatureExtractor`)
- **Key Modules**:
  - `GemmaAnalyzer.kt`: Holds `NailFeatureExtractor` internal object and `NailFeatures` data class.
  - `NailClassifier.kt`: Uses `NailFeatureExtractor` to classify features.
- **Verification Harness**: Create a Kotlin test file (e.g. `app/src/test/java/com/example/nailnutri/analysis/NailFeatureExtractorTest.kt` or `verify_extractor.py`) to run tests.
- **Build / Test Tooling**: `./gradlew compileDebugKotlin` or equivalent gradle test commands.

## Phased Execution Plan

### Phase 1: Exploration and Baseline Verification
- Spawn `teamwork_preview_explorer` to:
  1. Analyze the current implementation of `NailFeatureExtractor` inside `GemmaAnalyzer.kt`.
  2. Inspect existing tests and Gradle configuration.
  3. Determine the best way to execute builds/tests.
  4. Design a test case generation strategy (synthetic bitmaps representing Healthy, Pale, White Spots, Vertical Ridges, Dark Edges).

### Phase 2: Design and Implement the Test Harness
- Spawn `teamwork_preview_worker` to:
  1. Create the verification harness (`NailFeatureExtractorTest.kt` or a separate Kotlin/Java class or test class).
  2. Generate the 5 test cases matching conditions (Healthy, Pale, White Spots, Vertical Ridges, Dark Edges).
  3. Verify the baseline test harness compiles and runs, documenting baseline accuracy (which is likely less than 100%).

### Phase 3: Algorithm Optimization
- Spawn `teamwork_preview_explorer` (or reuse worker reports) to formulate optimization rules for RGB/HSV thresholds based on baseline results.
- Spawn `teamwork_preview_worker` to:
  1. Refine the RGB/HSV threshold heuristics in `NailFeatureExtractor.kt`.
  2. Run the test harness to verify new accuracy and average execution time.
  3. Keep modifying and testing until 100% accuracy and <50ms timing are achieved.

### Phase 4: Review and Auditing
- Spawn `teamwork_preview_reviewer` to review the modifications for correctness and styling.
- Spawn `teamwork_preview_auditor` to perform integrity forensics checks and verify no cheating (no hardcoded responses, genuine logic).

### Phase 5: Synthesis and Handoff
- Summarize results.
- Report completion/victory to the parent agent.
