import os
os.environ["KAGGLEHUB_CACHE"] = "d:/k"

import kagglehub
import kagglehub.config
print("KAGGLEHUB_CACHE env:", os.environ.get("KAGGLEHUB_CACHE"))
print("kagglehub cache folder:", kagglehub.config.get_cache_folder())
