import sys
try:
    import tensorflow as tf
    print("TensorFlow successfully imported.")
    print("Version:", tf.__version__)
    gpus = tf.config.list_physical_devices('GPU')
    cpus = tf.config.list_physical_devices('CPU')
    print("Available GPUs:", gpus)
    print("Available CPUs:", cpus)
except Exception as e:
    print("Failed to import TensorFlow:", e)
    sys.exit(1)
