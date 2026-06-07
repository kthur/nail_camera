## 2026-06-07T02:44:47Z
Role: Implementation Worker
Working Directory: d:/project/nail_camera/.agents/teamwork_preview_worker_implementation

Objective: Create the programmatic verification harness (using a JUnit test with custom mocks for Bitmap and Color), run baseline tests, optimize NailFeatureExtractor inside GemmaAnalyzer.kt to pass all test cases with 100% accuracy, and verify average execution time is under 50ms and the project compiles.

Scope Boundaries:
- Modify only the required files: app/src/main/java/com/example/nailnutri/analysis/GemmaAnalyzer.kt (specifically the NailFeatureExtractor class), and create files in app/src/test/java/.
- DO NOT change public signatures of GemmaAnalyzer or NailFeatureExtractor.

Input Information:
- Core codebase: d:/project/nail_camera/
- Baseline Explorer findings: d:/project/nail_camera/.agents/teamwork_preview_explorer_baseline/handoff.md

Instructions:
1. Create Java mock classes for android.graphics.Bitmap and android.graphics.Color inside app/src/test/java/android/graphics/ so that Android dependencies can be resolved locally on JVM without Robolectric.
   - Bitmap.java should implement width, height, getPixel, setPixel, createBitmap, and Bitmap.Config.
   - Color.java should implement red, green, blue, rgb, and RGBToHSV. (Translate RGB to HSV in Color.java accurately).
2. Create the test class NailFeatureExtractorTest.kt at app/src/test/java/com/example/nailnutri/analysis/NailFeatureExtractorTest.kt with the 5 test cases (Healthy, Pale, White Spots, Vertical Ridges, Dark Edges) representing these nail conditions using 100x100 synthetic bitmaps.
3. Run the unit tests via Gradle: `./gradlew testDebugUnitTest` to verify the test harness is working and document baseline failure/results.
4. Optimize the algorithm in NailFeatureExtractor inside GemmaAnalyzer.kt:
   - Fix array bounds / loop mismatch (calculate totalSamples based on step count: `val xSteps = (width - 1) / sampleStep + 1`, `val ySteps = (height - 1) / sampleStep + 1`, `val totalSamples = xSteps * ySteps`).
   - Fix brightnessStdDev check: multiply brightnessStdDev by 255.0 (or compare to 22.0/255.0) because HSV value component is normalized [0, 1].
   - Fix isLowRedness check to avoid excluding extreme cases (e.g. check avgR < 130 and ensure it doesn't trigger false positives by adding `&& !isDarkEdges` and `&& (avgR > avgB * 0.95 || avgR < 100.0)` or similar logic).
   - Optimize isDarkEdges threshold to be robust against background noise (e.g., set threshold to > 0.30).
5. Verify that all 5 tests pass with 100% accuracy.
6. Measure average execution time per image (using system timers in your test runner/test case over multiple iterations) and assert it is under 50ms on average.
7. Run `./gradlew compileDebugKotlin` to verify the entire project compiles successfully.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT
hardcode test results, create dummy/facade implementations, or
circumvent the intended task. A Forensic Auditor will independently
verify your work. Integrity violations WILL be detected and your
work WILL be rejected.
