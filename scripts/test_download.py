import os
os.environ["KAGGLEHUB_CACHE"] = "d:/k"

import kagglehub
from pathlib import Path

DEFAULT_DATASET = "nikhilgurav21/nail-disease-detection-dataset"

print("Downloading dataset to short path using KAGGLEHUB_CACHE...")
try:
    download_path = Path(kagglehub.dataset_download(DEFAULT_DATASET))
    print("Downloaded to:", download_path)
    if download_path.exists():
        contents = list(download_path.iterdir())
        print("Root contents:")
        for c in contents:
            print(" -", c.name, "(dir)" if c.is_dir() else "(file)")
            if c.is_dir():
                sub = list(c.iterdir())
                print("   Sub-items:", [s.name for s in sub[:10]])
except Exception as e:
    print("Download failed:", e)
