## 2026-06-06T17:40:26Z

The goal of this project is to improve the accuracy of fingernail health status and nutritional deficiency diagnosis in the NailNutri application by optimizing the pixel-level visual analysis algorithm in `NailFeatureExtractor.kt` (used by Gemma and the offline fallback analyzer). The agent team will refine the heuristics for detecting pale nail beds, white spots, vertical ridges, and dark edges, and generate their own set of test images to objectively verify and measure the accuracy improvements.

Working directory: d:/project/nail_camera
Integrity mode: development

## Requirements

### R1. NailFeatureExtractor Optimization
Optimize the RGB and HSV thresholding/analysis logic in `NailFeatureExtractor.kt` to improve the detection accuracy of:
- Pale nail bed / Low redness
- White spots (Leukonychia)
- Uneven texture (Vertical ridges)
- Dark edges (Spoon nail indicator)

### R2. Programmatic Verification Harness
Create an automated test suite or verification script that:
- Feeds a set of synthetic or sample fingernail images with known diagnostic labels to `NailFeatureExtractor`.
- Measures precision, recall, or accuracy metrics.
- Validates that the optimized extractor matches the expected symptoms with higher fidelity.

## Acceptance Criteria

### Execution & Integration
- [ ] Code compiles successfully without errors (`./gradlew compileDebugKotlin` passes).
- [ ] The modified `NailFeatureExtractor.kt` integrates seamlessly with `GemmaAnalyzer.kt` without changing its public signature.

### Accuracy & Validation
- [ ] An automated test harness (`NailFeatureExtractorTest.kt` or `verify_extractor.py`) is created and runnable.
- [ ] The test harness includes at least 5 test cases representing different nail conditions (healthy, pale, white spots, vertical ridges, dark edges).
- [ ] The optimized `NailFeatureExtractor` achieves 100% classification accuracy on the test cases within the test harness.
- [ ] Inference/extraction time per image is measured and remains under 50ms on average to maintain real-time performance.
