import tensorflow as tf
print("Importing models...")
try:
    m2 = tf.keras.applications.MobileNetV2(input_shape=(224,224,3), include_top=False, weights="imagenet")
    print("MobileNetV2 imported successfully.")
    m3 = tf.keras.applications.MobileNetV3Large(input_shape=(224,224,3), include_top=False, weights="imagenet")
    print("MobileNetV3Large imported successfully.")
    eff = tf.keras.applications.EfficientNetB0(input_shape=(224,224,3), include_top=False, weights="imagenet")
    print("EfficientNetB0 imported successfully.")
except Exception as e:
    print("Import failed:", e)
