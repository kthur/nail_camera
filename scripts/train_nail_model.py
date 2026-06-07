"""
Train a nail condition classifier from a Kaggle dataset and export to TFLite.
Usage: python scripts/train_nail_model.py [--force] [--dataset <kaggle-slug>]

If the model already exists at app/src/main/assets/nail_classifier.tflite,
training is skipped unless --force is passed.
"""

import argparse
import os
import sys
import shutil
import tempfile
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OUTPUT = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "nail_classifier.tflite"
DEFAULT_DATASET = "nikhilgurav21/nail-disease-detection-dataset"
CACHE_DIR = PROJECT_ROOT / ".cache" / "nail_dataset"
CLASS_MAPPING = {
    "Healthy_Nail": "healthy",
    "Onychomycosis": "white_spots",
    "Nail_Psoriasis": "vertical_ridges",
    "Pitting": "vertical_ridges",
    "Onychogryphosis": "spoon_nails",
    "ALM": "brittle",
    "Blue_finger": "brittle",
    "Clubbing": "brittle",
}


def check_dependencies():
    try:
        import tensorflow as tf
        import kagglehub
        import PIL
        return True
    except ImportError as e:
        print(f"Missing dependency: {e}")
        print("Install with: pip install -r scripts/requirements.txt")
        return False


def download_dataset(dataset_slug: str) -> Path:
    import kagglehub
    cache_marker = CACHE_DIR / ".downloaded"
    if cache_marker.exists():
        stored = cache_marker.read_text().strip()
        if stored == dataset_slug:
            print(f"Dataset already cached at {CACHE_DIR}")
            return CACHE_DIR / "data"
    print(f"Downloading dataset {dataset_slug} from Kaggle...")
    download_path = Path(kagglehub.dataset_download(dataset_slug))
    if CACHE_DIR.exists():
        shutil.rmtree(CACHE_DIR)
    shutil.copytree(download_path, CACHE_DIR / "data")
    cache_marker.parent.mkdir(parents=True, exist_ok=True)
    cache_marker.write_text(dataset_slug)
    print(f"Dataset cached at {CACHE_DIR}")
    return CACHE_DIR / "data"


def prepare_dataset(data_dir: Path):
    import tensorflow as tf
    from tensorflow.keras.preprocessing import image_dataset_from_directory
    img_size = (224, 224)
    batch_size = 32
    seed = 42
    subdirs = [d for d in data_dir.iterdir() if d.is_dir()]
    if not subdirs:
        subdirs = list(data_dir.rglob("*"))
        subdirs = [s for s in subdirs if s.is_dir() and list(s.iterdir())]
    if subdirs:
        data_source = data_dir
    else:
        zip_files = list(data_dir.glob("*.zip"))
        if zip_files:
            import zipfile
            extract_dir = data_dir / "extracted"
            if not extract_dir.exists():
                with zipfile.ZipFile(zip_files[0], "r") as zf:
                    zf.extractall(extract_dir)
            data_source = extract_dir
        else:
            print(f"No class subdirectories or zip files found in {data_dir}")
            print(f"Contents: {list(data_dir.iterdir())}")
            sys.exit(1)
    train_ds = image_dataset_from_directory(
        data_source,
        validation_split=0.2,
        subset="training",
        seed=seed,
        image_size=img_size,
        batch_size=batch_size,
        label_mode="categorical",
    )
    val_ds = image_dataset_from_directory(
        data_source,
        validation_split=0.2,
        subset="validation",
        seed=seed,
        image_size=img_size,
        batch_size=batch_size,
        label_mode="categorical",
    )
    class_names = train_ds.class_names
    print(f"Found {len(class_names)} classes: {class_names}")
    AUTOTUNE = tf.data.AUTOTUNE
    train_ds = train_ds.prefetch(AUTOTUNE)
    val_ds = val_ds.prefetch(AUTOTUNE)
    return train_ds, val_ds, class_names


def build_model(num_classes: int):
    import tensorflow as tf
    base = tf.keras.applications.MobileNetV2(
        input_shape=(224, 224, 3),
        include_top=False,
        weights="imagenet",
    )
    base.trainable = False
    inputs = tf.keras.Input(shape=(224, 224, 3))
    x = tf.keras.applications.mobilenet_v2.preprocess_input(inputs)
    x = base(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dense(128, activation="relu")(x)
    x = tf.keras.layers.Dropout(0.5)(x)
    outputs = tf.keras.layers.Dense(num_classes, activation="softmax")(x)
    model = tf.keras.Model(inputs, outputs)
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
        loss="categorical_crossentropy",
        metrics=["accuracy"],
    )
    return model


def train_model(model, train_ds, val_ds):
    import tensorflow as tf
    print("Training top layers (epochs 10)...")
    history = model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=10,
        verbose=1,
    )
    best_acc = max(history.history["val_accuracy"])
    print(f"Best validation accuracy: {best_acc:.4f}")
    return model


def convert_to_tflite(model, output_path: Path, class_names: list):
    import tensorflow as tf
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    tflite_model = converter.convert()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_bytes(tflite_model)
    labels_path = output_path.with_suffix(".txt")
    labels_path.write_text("\n".join(class_names))
    model_size = len(tflite_model) / (1024 * 1024)
    print(f"TFLite model saved: {output_path} ({model_size:.2f} MB)")
    print(f"Labels saved: {labels_path}")
    print(f"Classes: {class_names}")


def main():
    parser = argparse.ArgumentParser(description="Train nail classifier from Kaggle dataset")
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT), help="Output .tflite path")
    parser.add_argument("--dataset", default=DEFAULT_DATASET, help="Kaggle dataset slug")
    parser.add_argument("--force", action="store_true", help="Retrain even if model exists")
    parser.add_argument("--skip-download", action="store_true", help="Skip dataset download (use cached)")
    args = parser.parse_args()
    output_path = Path(args.output)
    if output_path.exists() and not args.force:
        print(f"Model already exists at {output_path}. Use --force to retrain.")
        print(f"Labels file: {output_path.with_suffix('.txt')}")
        return
    if not check_dependencies():
        sys.exit(1)
    os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"
    import tensorflow as tf
    gpus = tf.config.list_physical_devices("GPU")
    print(f"TensorFlow {tf.__version__} | GPUs: {len(gpus)}")
    if not args.skip_download:
        data_dir = download_dataset(args.dataset)
    else:
        data_dir = CACHE_DIR / "data"
        if not data_dir.exists():
            print(f"Cached data not found at {data_dir}. Run without --skip-download first.")
            sys.exit(1)
    train_ds, val_ds, class_names = prepare_dataset(data_dir)
    num_classes = len(class_names)
    model = build_model(num_classes)
    model.summary()
    model = train_model(model, train_ds, val_ds)
    convert_to_tflite(model, output_path, class_names)


if __name__ == "__main__":
    main()
