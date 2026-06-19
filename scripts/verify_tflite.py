import os
import sys
import numpy as np
from PIL import Image
from pathlib import Path

# Force CPU for TFLite to avoid GPU library issues in python scripts
os.environ["CUDA_VISIBLE_DEVICES"] = "-1"

PROJECT_ROOT = Path(__file__).resolve().parent.parent
MODEL_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "nail_classifier.tflite"
LABELS_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "nail_classifier.txt"
VAL_DIR = Path("d:/k/datasets/nikhilgurav21/nail-disease-detection-dataset/versions/1/data/validation")

def load_labels(path):
    with open(path, "r", encoding="utf-8") as f:
        return [line.strip() for line in f if line.strip()]

def preprocess_image(img_path):
    img = Image.open(img_path).convert("RGB")
    img = img.resize((224, 224))
    arr = np.array(img, dtype=np.float32)
    # Scale to [-1, 1]
    arr = (arr - 127.5) / 127.5
    # Add batch dimension
    arr = np.expand_dims(arr, axis=0)
    return arr

def verify_tflite():
    try:
        import tensorflow as tf
    except ImportError:
        print("TensorFlow not installed. Please install it to verify TFLite model.")
        return

    if not MODEL_PATH.exists():
        print(f"TFLite model not found at {MODEL_PATH}")
        return
    if not LABELS_PATH.exists():
        print(f"Labels file not found at {LABELS_PATH}")
        return
    if not VAL_DIR.exists():
        print(f"Validation directory not found at {VAL_DIR}")
        return

    labels = load_labels(LABELS_PATH)
    print(f"Loaded {len(labels)} classes: {labels}")

    # Initialize TFLite Interpreter
    interpreter = tf.lite.Interpreter(model_path=str(MODEL_PATH))
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    print(f"Model Input Details: {input_details}")
    print(f"Model Output Details: {output_details}")

    categories = sorted([d.name for d in VAL_DIR.iterdir() if d.is_dir()])
    
    total_images = 0
    correct_predictions = 0
    
    confusion_matrix = np.zeros((len(labels), len(labels)), dtype=int)
    class_to_idx = {label: idx for idx, label in enumerate(labels)}

    print("\nRunning TFLite model inference on Kaggle validation set...")
    for cat in categories:
        cat_dir = VAL_DIR / cat
        images = list(cat_dir.glob("*.jpg")) + list(cat_dir.glob("*.jpeg")) + list(cat_dir.glob("*.png"))
        
        # In Kaggle dataset, the directory names might match labels or be mapped
        # Let's see if the category is in our labels
        actual_label = cat
        if actual_label not in class_to_idx:
            # Try matching case-insensitively
            matched = False
            for label in labels:
                if label.lower() == actual_label.lower():
                    actual_label = label
                    matched = True
                    break
            if not matched:
                print(f"Warning: category {cat} not found in model labels {labels}. Skipping metric count.")
                continue

        actual_idx = class_to_idx[actual_label]
        cat_correct = 0
        cat_total = 0

        for img_path in images:
            input_data = preprocess_image(img_path)
            interpreter.set_tensor(input_details[0]['index'], input_data)
            interpreter.invoke()
            
            output_data = interpreter.get_tensor(output_details[0]['index'])[0]
            pred_idx = np.argmax(output_data)
            pred_label = labels[pred_idx]
            pred_conf = output_data[pred_idx]

            confusion_matrix[actual_idx, pred_idx] += 1
            cat_total += 1
            total_images += 1
            
            if pred_idx == actual_idx:
                cat_correct += 1
                correct_predictions += 1

        if cat_total > 0:
            print(f"Category: {cat:30s} | Accuracy: {cat_correct}/{cat_total} ({cat_correct/cat_total*100:.1f}%)")

    overall_accuracy = correct_predictions / total_images if total_images > 0 else 0.0
    print(f"\n==========================================")
    print(f"Overall TFLite Validation Accuracy: {correct_predictions}/{total_images} ({overall_accuracy*100:.2f}%)")
    print(f"==========================================")
    
    # Print Confusion Matrix
    print("\nConfusion Matrix:")
    print("Actual \\ Predicted")
    header = " " * 30 + "".join(f"{label[:8]:>10}" for label in labels)
    print(header)
    for idx, label in enumerate(labels):
        row = f"{label[:28]:30s}" + "".join(f"{confusion_matrix[idx, p_idx]:10d}" for p_idx in range(len(labels)))
        print(row)

if __name__ == "__main__":
    verify_tflite()
