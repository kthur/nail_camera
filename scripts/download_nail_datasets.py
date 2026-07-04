# scripts/download_nail_datasets.py
"""Download public nail disease image datasets.
Supported sources:
1. DermNet NZ – Nail Images
2. ISIC Archive – Nail Images
3. Roboflow – Nail Disease Detection
4. Hugging Face – onychomycosis
The script saves each dataset under `../datasets/<source_name>/images/`.
API keys (if required) should be placed in a `.env` file:
    ISIC_API_KEY=your_key
    ROBOFLOW_API_KEY=your_key
"""
import os, sys, argparse, requests, shutil
from pathlib import Path

def ensure_dir(path: Path):
    path.mkdir(parents=True, exist_ok=True)

def download_dermnet(dest: Path):
    # DermNet does not provide bulk API; we'll scrape the public gallery.
    # For simplicity, we download a pre‑compiled zip if available.
    zip_url = "https://dermnetnz.org/site_media/images/dermnet_nail_images.zip"
    r = requests.get(zip_url, stream=True)
    r.raise_for_status()
    zip_path = dest / "dermnet_nail_images.zip"
    with open(zip_path, "wb") as f:
        for chunk in r.iter_content(chunk_size=8192):
            f.write(chunk)
    shutil.unpack_archive(str(zip_path), str(dest / "images"))
    os.remove(zip_path)
    print("DermNet NZ downloaded.")

def download_isic(dest: Path, api_key: str):
    # ISIC Archive provides a REST endpoint.
    headers = {"Authorization": f"Bearer {api_key}"}
    # Get a list of image IDs for nail entries (search term "nail").
    search_url = "https://isic-archive.com/api/v1/image/search"
    params = {"q": "nail", "page": 0, "size": 1000}
    resp = requests.get(search_url, headers=headers, params=params)
    resp.raise_for_status()
    ids = [hit["id"] for hit in resp.json()["hits"]]
    ensure_dir(dest / "images")
    for img_id in ids:
        img_url = f"https://isic-archive.com/api/v1/image/{img_id}/download"
        img_resp = requests.get(img_url, headers=headers, stream=True)
        img_resp.raise_for_status()
        img_path = dest / "images" / f"{img_id}.jpg"
        with open(img_path, "wb") as f:
            for chunk in img_resp.iter_content(chunk_size=8192):
                f.write(chunk)
    print(f"ISIC downloaded {len(ids)} images.")

def download_roboflow(dest: Path, api_key: str):
    # Roboflow provides a direct download URL for public datasets.
    project = "nail-disease-detection"
    version = 1
    url = f"https://public.roboflow.com/dataset/{project}/{version}?format=zip"
    r = requests.get(url, stream=True)
    r.raise_for_status()
    zip_path = dest / "roboflow_nail.zip"
    with open(zip_path, "wb") as f:
        for chunk in r.iter_content(chunk_size=8192):
            f.write(chunk)
    shutil.unpack_archive(str(zip_path), str(dest / "images"))
    os.remove(zip_path)
    print("Roboflow dataset downloaded.")

def download_huggingface(dest: Path):
    # Use huggingface hub CLI to download the dataset.
    # Requires `huggingface_hub` package.
    from huggingface_hub import snapshot_download
    repo_id = "kaist-ai/onychomycosis"
    cache_dir = snapshot_download(repo_id=repo_id, repo_type="dataset", local_dir=dest / "hf_onychomycosis", local_dir_use_symlinks=False)
    # The repo contains an `images/` folder.
    shutil.copytree(Path(cache_dir) / "images", dest / "images", dirs_exist_ok=True)
    print("HuggingFace dataset downloaded.")

def main():
    parser = argparse.ArgumentParser(description="Download nail disease datasets")
    parser.add_argument("--dest", type=str, default="../datasets", help="Destination folder (relative to script)")
    args = parser.parse_args()
    base = Path(__file__).parent.parent / args.dest
    sources = {
        "DermNet": download_dermnet,
        "ISIC": download_isic,
        "Roboflow": download_roboflow,
        "HuggingFace": download_huggingface,
    }
    # Load API keys from .env if present
    api_key_isic = os.getenv("ISIC_API_KEY", "")
    api_key_roboflow = os.getenv("ROB​OFLOW_API_KEY", "")
    for name, func in sources.items():
        dest = base / name.lower()
        ensure_dir(dest)
        try:
            if name == "ISIC":
                func(dest, api_key_isic)
            elif name == "Roboflow":
                func(dest, api_key_roboflow)
            else:
                func(dest)
        except Exception as e:
            print(f"Failed {name}: {e}", file=sys.stderr)

if __name__ == "__main__":
    main()
