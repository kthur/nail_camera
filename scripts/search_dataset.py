import os
from pathlib import Path

data_dir = Path("d:/k/datasets/nikhilgurav21/nail-disease-detection-dataset/versions/1")
print("Searching for files/folders containing Psoriasis or Onychomycosis...")
matches = []
for root, dirs, files in os.walk(data_dir):
    for d in dirs:
        if "psoriasis" in d.lower() or "onychomycosis" in d.lower():
            matches.append(os.path.join(root, d))
    for f in files:
        if "psoriasis" in f.lower() or "onychomycosis" in f.lower():
            matches.append(os.path.join(root, f))

if matches:
    print("Found matches:")
    for m in matches[:20]:
        print(" -", m)
else:
    print("No matches found.")
