"""
Train a nail condition classifier from a Kaggle dataset and export to TFLite.
Usage: python scripts/train_nail_model.py [--force] [--dataset <kaggle-slug>] [--compare] [--arch <architecture>]
"""

import os
# Force kagglehub to use a short cache path to avoid Windows 260-character limit
os.environ["KAGGLEHUB_CACHE"] = "d:/k"

import argparse
import sys
import shutil
import time
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OUTPUT = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "nail_classifier.tflite"
DEFAULT_DATASET = "nikhilgurav21/nail-disease-detection-dataset"
CACHE_DIR = PROJECT_ROOT / ".cache" / "nail_dataset"


def check_dependencies():
    try:
        import tensorflow as tf
        import kagglehub
        import PIL
        return True
    except ImportError as e:
        print(f"Missing dependency: {e}")
        return False


def download_dataset(dataset_slug: str) -> Path:
    import kagglehub
    print(f"Downloading dataset {dataset_slug} from Kaggle (KAGGLEHUB_CACHE={os.environ['KAGGLEHUB_CACHE']})...")
    download_path = Path(kagglehub.dataset_download(dataset_slug))
    print(f"Dataset downloaded/extracted to {download_path}")
    return download_path


def prepare_dataset(data_dir: Path):
    import tensorflow as tf
    from tensorflow.keras.preprocessing import image_dataset_from_directory
    
    img_size = (224, 224)
    batch_size = 32
    seed = 42
    
    # Locate data source folder
    # In the downloaded Kaggle structure, we have <dataset_path>/data/train and <dataset_path>/data/validation
    data_source_train = data_dir / "data" / "train"
    data_source_val = data_dir / "data" / "validation"
    
    if not data_source_train.exists() or not data_source_val.exists():
        # Fallback if structure is different
        subdirs = [d for d in data_dir.iterdir() if d.is_dir()]
        if len(subdirs) == 1 and subdirs[0].name == "data":
            data_source_train = subdirs[0] / "train"
            data_source_val = subdirs[0] / "validation"
        else:
            print(f"Could not find train/validation directories in {data_dir}")
            sys.exit(1)
            
    print(f"Loading training data from {data_source_train}")
    train_ds = image_dataset_from_directory(
        data_source_train,
        image_size=img_size,
        batch_size=batch_size,
        label_mode="categorical",
        seed=seed,
    )
    
    print(f"Loading validation data from {data_source_val}")
    val_ds = image_dataset_from_directory(
        data_source_val,
        image_size=img_size,
        batch_size=batch_size,
        label_mode="categorical",
        seed=seed,
    )
    
    class_names = sorted(train_ds.class_names)
    print(f"Found {len(class_names)} classes: {class_names}")
    
    # Map both datasets to [-1, 1] range to match Android app's preprocessing
    def scale_inputs(image, label):
        return (image - 127.5) / 127.5, label

    train_ds = train_ds.map(scale_inputs, num_parallel_calls=tf.data.AUTOTUNE)
    val_ds = val_ds.map(scale_inputs, num_parallel_calls=tf.data.AUTOTUNE)
    
    AUTOTUNE = tf.data.AUTOTUNE
    train_ds = train_ds.prefetch(AUTOTUNE)
    val_ds = val_ds.prefetch(AUTOTUNE)
    
    return train_ds, val_ds, class_names


def build_model(num_classes: int, arch: str):
    import tensorflow as tf
    from tensorflow.keras import layers
    
    inputs = tf.keras.Input(shape=(224, 224, 3))
    
    # Data Augmentation layers prepended to the model
    # (these are only active during training, acting as identity/noop during evaluation/inference)
    x = layers.RandomFlip("horizontal_and_vertical")(inputs)
    x = layers.RandomRotation(0.2)(x)
    x = layers.RandomTranslation(0.1, 0.1)(x)
    x = layers.RandomZoom(0.1)(x)
    
    # Base model selection & rescaling compatibility
    if arch == "mobilenet_v2":
        # MobileNetV2 expects [-1, 1], which matches the scaled input. No rescaling needed.
        base_model = tf.keras.applications.MobileNetV2(
            input_shape=(224, 224, 3),
            include_top=False,
            weights="imagenet",
        )
    elif arch == "mobilenet_v3_large":
        # MobileNetV3 expects [0, 255]. Rescale [-1, 1] -> [0, 255]
        x = layers.Rescaling(scale=127.5, offset=127.5)(x)
        base_model = tf.keras.applications.MobileNetV3Large(
            input_shape=(224, 224, 3),
            include_top=False,
            weights="imagenet",
        )
    elif arch == "efficientnet_b0":
        # EfficientNetB0 expects [0, 255]. Rescale [-1, 1] -> [0, 255]
        x = layers.Rescaling(scale=127.5, offset=127.5)(x)
        base_model = tf.keras.applications.EfficientNetB0(
            input_shape=(224, 224, 3),
            include_top=False,
            weights="imagenet",
        )
    else:
        raise ValueError(f"Unsupported architecture: {arch}")
        
    base_model.trainable = False
    
    # Forward pass through base model
    x = base_model(x, training=False)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dense(128, activation="relu")(x)
    x = layers.Dropout(0.5)(x)
    outputs = layers.Dense(num_classes, activation="softmax")(x)
    
    model = tf.keras.Model(inputs, outputs)
    return model, base_model


def train_model(model, base_model, train_ds, val_ds, arch: str):
    import tensorflow as tf
    import time
    
    # Phase 1: Feature Extraction
    print(f"\n[{arch}] Phase 1: Training top layers (5 epochs)...")
    base_model.trainable = False
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=1e-3),
        loss="categorical_crossentropy",
        metrics=["accuracy"],
    )
    
    start_time = time.time()
    history_p1 = model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=5,
        verbose=1,
    )
    p1_time = time.time() - start_time
    
    # Phase 2: Fine-Tuning
    print(f"\n[{arch}] Phase 2: Fine-tuning base model (5 epochs)...")
    base_model.trainable = True
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=1e-5),
        loss="categorical_crossentropy",
        metrics=["accuracy"],
    )
    
    early_stopping = tf.keras.callbacks.EarlyStopping(
        monitor='val_loss',
        patience=2,
        restore_best_weights=True,
        verbose=1
    )
    lr_scheduler = tf.keras.callbacks.ReduceLROnPlateau(
        monitor='val_loss',
        factor=0.2,
        patience=1,
        min_lr=1e-6,
        verbose=1
    )
    
    start_time = time.time()
    history_p2 = model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=5,
        callbacks=[early_stopping, lr_scheduler],
        verbose=1,
    )
    p2_time = time.time() - start_time
    total_time = p1_time + p2_time
    
    val_acc_history = history_p2.history.get("val_accuracy", [])
    if not val_acc_history:
        val_acc_history = history_p1.history.get("val_accuracy", [0.0])
    best_acc = max(val_acc_history)
    
    print(f"\n[{arch}] Training completed in {total_time:.2f}s (P1: {p1_time:.2f}s, P2: {p2_time:.2f}s)")
    print(f"[{arch}] Best Validation Accuracy: {best_acc:.4f}")
    
    return model, best_acc


def convert_to_tflite(model, output_path: Path, class_names: list):
    import tensorflow as tf
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    tflite_model = converter.convert()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_bytes(tflite_model)
    labels_path = output_path.with_suffix(".txt")
    # Sort class names alphabetically
    sorted_classes = sorted(class_names)
    labels_path.write_text("\n".join(sorted_classes))
    model_size = len(tflite_model) / (1024 * 1024)
    print(f"TFLite model saved: {output_path} ({model_size:.2f} MB)")
    print(f"Labels saved: {labels_path}")
    print(f"Classes (alphabetical): {sorted_classes}")
def main():
    parser = argparse.ArgumentParser(description="Train nail classifier from Kaggle dataset")
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT), help="Output .tflite path")
    parser.add_argument("--dataset", default=DEFAULT_DATASET, help="Kaggle dataset slug")
    parser.add_argument("--force", action="store_true", help="Retrain even if model exists")
    parser.add_argument("--skip-download", action="store_true", help="Skip dataset download (use cached)")
    parser.add_argument("--arch", default="mobilenet_v2", choices=["mobilenet_v2", "mobilenet_v3_large", "efficientnet_b0"], help="Model architecture")
    parser.add_argument("--compare", action="store_true", help="Compare multiple architectures and select the best one")
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

    if args.compare:
        architectures = ["mobilenet_v2", "mobilenet_v3_large"]
        best_acc = -1.0
        best_model = None
        best_arch = None
        results = {}
        for arch in architectures:
            print(f"\n==================================================")
            print(f" Training architecture: {arch}")
            print(f"==================================================")
            try:
                model, base_model = build_model(num_classes, arch)
                model, acc = train_model(model, base_model, train_ds, val_ds, arch)
                results[arch] = acc
                if acc > best_acc:
                    best_acc = acc
                    best_model = model
                    best_arch = arch
            except Exception as e:
                print(f"Failed to train {arch}: {e}")
        
        if best_model is None:
            print("No models trained successfully.")
            sys.exit(1)
            
        print("\n=== Comparison Results ===")
        for arch, acc in results.items():
            print(f" - {arch}: Val Accuracy = {acc:.4f}")
            
        print(f"\n==================================================")
        print(f" Best architecture: {best_arch} with accuracy {best_acc:.4f}")
        print(f"==================================================")
        convert_to_tflite(best_model, output_path, class_names)
    else:
        model, base_model = build_model(num_classes, args.arch)
        model.summary()
        model, acc = train_model(model, base_model, train_ds, val_ds, args.arch)
        convert_to_tflite(model, output_path, class_names)


if __name__ == "__main__":
    main()
