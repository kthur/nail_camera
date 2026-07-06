# scripts/download_nail_datasets.py
"""Download public nail disease image datasets with strict timeouts and progress prints.
"""
import os, sys, argparse, requests, shutil, subprocess
from pathlib import Path

def ensure_dir(path: Path):
    path.mkdir(parents=True, exist_ok=True)

def install_package(package_name: str):
    try:
        __import__(package_name)
    except ImportError:
        print(f"[*] Installing dependency '{package_name}' via pip...", flush=True)
        try:
            subprocess.check_call([sys.executable, "-m", "pip", "install", package_name])
            print(f"[+] '{package_name}' installed.", flush=True)
        except Exception as e:
            print(f"[-] Failed to install {package_name}: {e}", flush=True)

def has_kaggle_credentials() -> bool:
    if os.getenv("KAGGLE_USERNAME") and os.getenv("KAGGLE_KEY"):
        return True
    home = Path.home()
    config_file = home / ".kaggle" / "kaggle.json"
    return config_file.exists()

def download_alternative_kaggle(dest: Path):
    if not has_kaggle_credentials():
        print("[!] Kaggle key absent. Skipping saurabhshahane/nail-dataset.", flush=True)
        return
        
    install_package("kagglehub")
    import kagglehub
    dataset_slug = "saurabhshahane/nail-dataset"
    print(f"[*] Downloading {dataset_slug} from Kaggle...", flush=True)
    try:
        download_path = Path(kagglehub.dataset_download(dataset_slug))
        ensure_dir(dest / "images")
        copied = 0
        for ext in ["*.jpg", "*.jpeg", "*.png"]:
            for f in download_path.rglob(ext):
                shutil.copy2(f, dest / "images" / f.name)
                copied += 1
        print(f"[+] Kaggle Alternative: Copied {copied} images.", flush=True)
    except Exception as e:
        print(f"[-] Kaggle download failed for {dataset_slug}: {e}", flush=True)

def download_isic(dest: Path, api_key: str):
    print("[*] Querying ISIC Archive API v2...", flush=True)
    search_url = "https://api.isic-archive.com/api/v2/images"
    params = {
        "query": "anatom_site_general:\"upper extremity\"",
        "limit": 15
    }
    
    headers = {}
    if api_key:
        headers["Authorization"] = f"Bearer {api_key}"
        
    try:
        resp = requests.get(search_url, headers=headers, params=params, timeout=5)
        resp.raise_for_status()
        results = resp.json().get("results", [])
        print(f"[+] Found {len(results)} candidate images in ISIC.", flush=True)
        
        ensure_dir(dest / "images")
        downloaded = 0
        for item in results:
            img_id = item["isic_id"]
            # Extract direct full image file URL
            img_url = item.get("files", {}).get("full", {}).get("url")
            if not img_url:
                img_url = f"https://api.isic-archive.com/api/v2/images/{img_id}/download"
                
            try:
                # Fast timeout (3s) to prevent hanging
                img_resp = requests.get(img_url, stream=True, timeout=3)
                img_resp.raise_for_status()
                img_path = dest / "images" / f"{img_id}.jpg"
                with open(img_path, "wb") as f:
                    for chunk in img_resp.iter_content(chunk_size=8192):
                        f.write(chunk)
                downloaded += 1
                print(f"    Downloaded image {downloaded}/{len(results)}: {img_id}", flush=True)
            except Exception as ex:
                print(f"    [!] Failed download for {img_id}: {ex}", flush=True)
                continue
        print(f"[+] ISIC API v2: Completed {downloaded} downloads.", flush=True)
    except Exception as e:
        print(f"[-] ISIC v2 query failed: {e}. Skipping ISIC.", flush=True)

def download_roboflow(dest: Path, api_key: str = ""):
    if not has_kaggle_credentials():
        print("[!] Kaggle key absent. Skipping polycarp/nail-disease-dataset mirror.", flush=True)
        return
        
    install_package("kagglehub")
    import kagglehub
    dataset_slug = "polycarp/nail-disease-dataset"
    print(f"[*] Downloading mirror dataset {dataset_slug}...", flush=True)
    try:
        download_path = Path(kagglehub.dataset_download(dataset_slug))
        ensure_dir(dest / "images")
        copied = 0
        for ext in ["*.jpg", "*.jpeg", "*.png"]:
            for f in download_path.rglob(ext):
                shutil.copy2(f, dest / "images" / f.name)
                copied += 1
        print(f"[+] Roboflow/Kaggle mirror: Copied {copied} images.", flush=True)
    except Exception as e:
        print(f"[-] Kaggle download failed for {dataset_slug}: {e}", flush=True)

def download_huggingface(dest: Path):
    install_package("huggingface_hub")
    from huggingface_hub import snapshot_download
    repo_id = "kaist-ai/onychomycosis"
    print(f"[*] Downloading {repo_id} from HuggingFace Hub (This might take a moment)...", flush=True)
    try:
        cache_dir = snapshot_download(
            repo_id=repo_id, 
            repo_type="dataset", 
            local_dir=dest / "hf_onychomycosis", 
            local_dir_use_symlinks=False
        )
        ensure_dir(dest / "images")
        images_dir = Path(cache_dir) / "images"
        copied = 0
        if images_dir.exists():
            shutil.copytree(images_dir, dest / "images", dirs_exist_ok=True)
            copied = len(list((dest / "images").glob("*")))
        else:
            for ext in ["*.jpg", "*.jpeg", "*.png"]:
                for f in Path(cache_dir).rglob(ext):
                    shutil.copy2(f, dest / "images" / f.name)
                    copied += 1
        print(f"[+] HuggingFace: Compiled {copied} images.", flush=True)
    except Exception as e:
        print(f"[-] HuggingFace snapshot failed: {e}. Skipping HF.", flush=True)

def main():
    parser = argparse.ArgumentParser(description="Download nail disease datasets")
    parser.add_argument("--dest", type=str, default="../datasets", help="Destination folder (relative to script)")
    args = parser.parse_args()
    base = Path(__file__).parent.parent / args.dest
    
    sources = {
        "DermNet_KaggleAlternative": download_alternative_kaggle,
        "ISIC": download_isic,
        "Roboflow_Mirror": download_roboflow,
        "HuggingFace": download_huggingface,
    }
    
    api_key_isic = os.getenv("ISIC_API_KEY", "")
    for name, func in sources.items():
        dest = base / name.lower()
        ensure_dir(dest)
        try:
            print(f"\n--- Processing Source: {name} ---", flush=True)
            if name == "ISIC":
                func(dest, api_key_isic)
            else:
                func(dest)
        except Exception as e:
            print(f"[-] Failed {name}: {e}", file=sys.stderr, flush=True)
    print("\n[+] All download steps completed.", flush=True)

if __name__ == "__main__":
    main()
