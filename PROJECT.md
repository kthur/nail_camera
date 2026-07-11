# Project: nail_camera Production Enhancement

## Architecture
- **UI Module (`ui/`)**: Android Jetpack Compose views for history, home, result, and sensor scans.
- **Analysis Module (`analysis/`)**: Algorithms and models for conjunctiva luma check, PPG scan, and Kit reader.
- **CI/CD (`.github/workflows/`)**: GitHub actions workflow that builds, signs, and distributes the Android app.
- **Data Module (`data/`)**: SharedPreferences database repository storing `NailAnalysisResult`.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | E2E/Verification Test Suite | Create JUnit unit/integration tests for R1, R2, R3 | none | DONE |
| 2 | R1: History UX Completion | Date-sorted cards, badges, navigation, and Canvas trend chart | M1 | DONE |
| 3 | R2: Sensor Quality Guards | Luma checks, button state, kit reader SD threshold, error alerts | M1 | DONE |
| 4 | R3: CI/CD Release Signing | android.yml keystore setup, build.gradle.kts signingConfigs fallback | M1 | DONE |
| 5 | Verification & Forensic Audit | Run verification tests, build app, run forensic integrity audit | M2, M3, M4 | DONE |

## Interface Contracts
### `ConjunctivaAnalyzer.analyze()`
- Input: `imagePath: String`, `context: Context` (or bitmap depending on method signature)
- Returns: `NailAnalysisResult`. If average luma is outside [40, 220], `symptoms` must contain `"조도 부적합"`.

### `KitReader.readKit()`
- Input: `bitmap: Bitmap` (or similar)
- Returns: `NailAnalysisResult` (or reading status). If standard deviation in T/C region is below threshold, `symptoms` must contain `"키트 미감지"`.

### `PPGAnalyzer` and `PpgScanScreen`
- Input: `isFingerDetected` state flow.
- UI Contract: If `isFingerDetected == false`, measurement button must have `enabled = false`. Real-time detection status text color must distinguish green (true) and grey (false).

## Code Layout
- `app/src/main/java/com/example/nailnutri/ui/history/HistoryScreen.kt` - History view & Compose Canvas chart.
- `app/src/main/java/com/example/nailnutri/analysis/ConjunctivaAnalyzer.kt` - Conjunctiva luma calculation.
- `app/src/main/java/com/example/nailnutri/analysis/KitReader.kt` - Kit reading and T/C line standard deviation.
- `app/src/main/java/com/example/nailnutri/ui/sensor/PpgScanScreen.kt` - PPG measurement button and finger check.
- `app/build.gradle.kts` - signingConfigs release block.
- `.github/workflows/android.yml` - Keystore setup and workflow run logic.
