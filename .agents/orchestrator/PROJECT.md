# Project: NailNutri NailFeatureExtractor Optimization

## Architecture
The application runs on Android and uses Gemma for on-device analysis. The image analysis components are:
- `NailFeatureExtractor`: Extracted pixels from fingernail images to identify RGB and HSV features, computing averages and standard deviations to flag:
  - Pale nail beds (Low redness)
  - White spots (Leukonychia)
  - Uneven texture (Vertical ridges)
  - Dark edges (Spoon nail indicator)
- `GemmaAnalyzer`: Uses `NailFeatureExtractor` to run analysis and formats a JSON prompt for Gemma model.
- `NailClassifier`: Another component that utilizes `NailFeatureExtractor`.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | Baseline Exploration | Analyze the current implementation, code layout, compilation status, and baseline behavior. | None | DONE |
| 2 | Verification Harness | Implement test cases and a programmatic test runner verifying all 5 nail conditions. | Milestone 1 | DONE |
| 3 | Extractor Optimization | Refine RGB/HSV thresholds and heuristics in `NailFeatureExtractor` to achieve 100% accuracy and keep average inference time < 50ms. | Milestone 2 | DONE |
| 4 | Verification & Audit | Verify correctness, run reviews, and run forensic integrity audit. | Milestone 3 | IN_PROGRESS (Conv: 10c88003-53f7-445f-a41a-781e85ace8c5, 373100a4-efe9-4771-8f04-631a24160fe1) |

## Code Layout
- Extractor: `app/src/main/java/com/example/nailnutri/analysis/GemmaAnalyzer.kt` (lines 298-419)
- Analyzer: `app/src/main/java/com/example/nailnutri/analysis/GemmaAnalyzer.kt`
- Classifier: `app/src/main/java/com/example/nailnutri/analysis/NailClassifier.kt`
- Test Harness: To be placed at `app/src/test/java/com/example/nailnutri/analysis/NailFeatureExtractorTest.kt` (or similar)
