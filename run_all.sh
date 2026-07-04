#!/usr/bin/env bash

# run_all.sh – End‑to‑end pipeline for nail‑disease model
# -----------------------------------------------------
# 1) Install Python dependencies (once)
# 2) Download public datasets
# 3) Preprocess images
# 4) Train TensorFlow Lite model
# 5) Build Android app
# -----------------------------------------------------

set -e

# ----- Helpers ---------------------------------------------------
function echo_step() {
    echo -e "\n=== $1 ==="
}

# Ensure we are in the project root (where this script lives)
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
cd "$SCRIPT_DIR"

# ----- 1) Install Python dependencies -----------------------------
echo_step "Installing Python dependencies"
python3 -m pip install --upgrade pip
python3 -m pip install tensorflow pillow requests python-dotenv huggingface_hub

# ----- 2) Download datasets ---------------------------------------
echo_step "Downloading datasets"
python3 scripts/download_nail_datasets.py --dest datasets

# ----- 3) Preprocess images --------------------------------------
echo_step "Pre‑processing images"
python3 scripts/preprocess_nail_data.py --datasets datasets --label_map app/src/main/assets/label_map.json

# ----- 4) Train the model ----------------------------------------
echo_step "Training TensorFlow Lite model"
python3 model_training/train_nail_classifier.py \
    --data_csv datasets/all_nail_data.csv \
    --output_dir app/src/main/assets \
    --model_name model_v2.tflite \
    --epochs 20 \
    --batch_size 32

# ----- 5) Build Android app --------------------------------------
echo_step "Building Android app (Debug APK)"
# Use Gradle wrapper if available, otherwise fall back to system gradle
if [[ -f ./gradlew ]]; then
    ./gradlew clean assembleDebug
else
    gradle clean assembleDebug
fi

echo_step "Pipeline completed successfully!"

echo "You can find the APK at ./app/build/outputs/apk/debug/"
