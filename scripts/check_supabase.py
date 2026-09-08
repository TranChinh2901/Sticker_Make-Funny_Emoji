#!/usr/bin/env python3
"""Read-only schema exposure check. Never prints URL, keys, or response bodies.
A successful probe is NOT a CRUD/RLS/Storage end-to-end test.
"""
import json
from pathlib import Path
import sys
import urllib.error
import urllib.request

root = Path(__file__).resolve().parents[1]
properties = {}
try:
    for line in (root / "local.properties").read_text().splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            properties[key.strip()] = value.strip().replace("\\:", ":").replace("\\=", "=")
except OSError:
    sys.exit("Missing local.properties")
url = properties.get("SUPABASE_URL", "").rstrip("/")
key = properties.get("SUPABASE_PUBLISHABLE_KEY", "")
if not url.startswith("https://") or not key:
    sys.exit("Missing HTTPS Supabase URL or publishable key")
tables = ["sticker_collections", "stickers", "collection_stickers", "user_settings", "app_feedback", "sticker_favorites"]
failed = False
for table in tables:
    request = urllib.request.Request(
        f"{url}/rest/v1/{table}?select=*&limit=0", headers={"apikey": key}
    )
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            print(f"{table}: HTTP {response.status}")
    except urllib.error.HTTPError as error:
        failed = True
        try:
            code = json.loads(error.read()).get("code", "unknown")
        except (ValueError, AttributeError):
            code = "unknown"
        # Only emit a short server error identifier, never arbitrary server content.
        code = code if isinstance(code, str) and code.isalnum() and len(code) < 20 else "unknown"
        print(f"{table}: HTTP {error.code}, {code}")
    except (OSError, ValueError):
        failed = True
        print(f"{table}: connection failed")
print("Schema probes failed. Check deployment and permissions." if failed else
      "Schema probes passed. Guest auth, ownership isolation, uploads and CRUD still need live tests.")
sys.exit(1 if failed else 0)
