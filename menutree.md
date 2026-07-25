# NailNutri - Navigation Menu Tree Map

## 📑 Application Navigation & UX Route Structure

```markdown
NailNutri App Navigation Tree
├── 🚀 0. Onboarding [Onboarding]
│   └── OnboardingScreen
│
├── 🏛️ Main Bottom Navigation Bar
│   ├── 🏠 1. Home [Home]
│   │   ├── Vision AI Camera Scan Banner -> CameraScan
│   │   ├── Voice AI Symptom Diagnosis Banner -> VoiceScan
│   │   ├── Sensor Suite Banner -> SensorDashboard
│   │   ├── Scans Counter & History Quick Card -> History
│   │   ├── Latest Analysis Quick View -> AnalysisResult
│   │   └── Educational Nutrition Guides
│   │
│   ├── 🩺 2. Scan Suite [SensorDashboard]
│   │   ├── 📸 Category: Camera & Vision
│   │   │   ├── Anemia Conjunctiva Scan [AnemiaScan] -> AnalysisResult
│   │   │   ├── Nail AI Pixel Camera Scan [CameraScan] -> AnalysisResult
│   │   │   └── LFA Rapid Kit Reader Scan [LfaScan] -> AnalysisResult
│   │   ├── 🖐️ Category: Bio Sensor
│   │   │   └── PPG Heart Rate Waveform Scan [PpgScan] -> AnalysisResult
│   │   ├── 🎙️ Category: Voice & Audio
│   │   │   ├── Voice Symptom AI Diagnosis [VoiceScan] -> AnalysisResult
│   │   │   ├── 3-Sec Voice Fatigue Check [VoiceScan] -> AnalysisResult
│   │   │   └── Snoring Sleep Audio Scanner [SleepScan] -> AnalysisResult
│   │   └── 📊 Comprehensive Session Reports Entry [SessionListNavKey]
│   │
│   ├── 📊 3. History & Reports [History]
│   │   ├── 📈 Tab 0: Scan History
│   │   │   ├── Nutrient Deficiency Risk Trend Canvas Chart
│   │   │   ├── Scan History List Cards -> AnalysisResult [AnalysisResult]
│   │   │   └── Clear All History Action
│   │   └── 📄 Tab 1: Comprehensive Session Reports
│   │       ├── Session Report List Cards -> SessionReportScreen [SessionReportScreen]
│   │       └── Create New Session Action
│   │
│   └── ⚙️ 4. Settings [Settings]
│       ├── Profile & Mock Mode Toggle
│       ├── Vision AI & Gemma Model Path Settings
│       └── Notification Reminders
│
└── 🔍 Full Screen Diagnostics & Detail Views
    ├── CameraScanScreen [CameraScan]
    ├── AnemiaScanScreen [AnemiaScan]
    ├── PpgScanScreen [PpgScan]
    ├── LfaScanScreen [LfaScan]
    ├── SleepAudioScreen [SleepScan]
    ├── VoiceAnalysisScreen [VoiceScan]
    ├── AnalysisResultScreen [AnalysisResult]
    ├── SessionListScreen [SessionListNavKey]
    └── SessionReportScreen [SessionReportScreen]
```

## 🔗 Route Identifier Registry

| NavKey / Route Identifier | Screen Composable | Description |
|---|---|---|
| `Onboarding` | `OnboardingScreen` | Initial onboarding tutorial |
| `Home` | `HomeScreen` | Dashboard hero view with quick diagnostic triggers |
| `SensorDashboard` | `SensorDashboardScreen` | Category-filtered hardware sensor diagnostic suite |
| `History` | `HistoryScreen` | Dual-tab history trend chart and session reports |
| `Settings` | `SettingsScreen` | App options, Gemma model setup, mock mode toggle |
| `CameraScan` | `CameraScanScreen` | Nail pixel analysis camera scanner |
| `AnemiaScan` | `AnemiaScanScreen` | Eye conjunctiva luma ratio anemia scanner |
| `PpgScan` | `PpgScanScreen` | Finger pulse wave HRV stress scanner |
| `LfaScan` | `LfaScanScreen` | Urine LFA test kit reader scanner |
| `SleepScan` | `SleepAudioScreen` | Nighttime snoring sleep audio scanner |
| `VoiceScan` | `VoiceAnalysisScreen` | Voice symptom AI and 3-sec vocal jitter analyzer |
| `AnalysisResult` | `AnalysisResultScreen` | Detailed result view for single scan |
| `SessionListNavKey` | `SessionListScreen` | List of combined multi-sensor session reports |
| `SessionReportScreen` | `SessionReportScreen` | Comprehensive session report with score gauge and share option |
