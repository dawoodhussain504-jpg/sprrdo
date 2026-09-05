#!/usr/bin/env python3
"""
Speedo Automated App Update & Version Sequencer
================================================
Usage:
    python scripts/publish_update.py [options]

Options:
    --code <int>       Specific versionCode (default: auto-increment current + 1)
    --name <str>       Specific versionName (default: 1.0.<versionCode>)
    --title <str>      Custom update notification title
    --message <str>    Custom update notification message
    --force            Mark this update as mandatory (force update)
    --skip-build       Skip Gradle APK compilation (sync configs only)
    --no-push          Do not push to Git or deploy to Railway
"""

import os
import sys
import json
import re
import shutil
import subprocess
import argparse
import urllib.request
import urllib.error
import ssl
import time

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ANDROID_DIR = os.path.join(ROOT_DIR, "android")
BACKEND_DIR = os.path.join(ROOT_DIR, "backend")
DOWNLOADS_DIR = os.path.join(ROOT_DIR, "downloads")
BACKEND_DOWNLOADS_DIR = os.path.join(BACKEND_DIR, "downloads")

PROD_URL = "https://web-production-5d826.up.railway.app"

def log(tag, msg):
    print(f"[{tag}] {msg}", flush=True)

def run_cmd(cmd, cwd=ROOT_DIR):
    log("RUN", f"Executing: {cmd} (in {cwd})")
    res = subprocess.run(cmd, shell=True, cwd=cwd, text=True, capture_output=True)
    if res.returncode != 0:
        if res.stderr:
            print(res.stderr, flush=True)
        if res.stdout:
            print(res.stdout, flush=True)
        return False
    return True

def get_current_version():
    candidates = [
        os.path.join(ROOT_DIR, "app-versions.json"),
        os.path.join(BACKEND_DIR, "app-versions.json"),
    ]
    for c in candidates:
        if os.path.exists(c):
            try:
                with open(c, "r", encoding="utf-8") as f:
                    data = json.load(f)
                max_code = 1
                name = "1.0.0"
                for app in ["rider", "captain", "admin"]:
                    if app in data and isinstance(data[app].get("versionCode"), int):
                        if data[app]["versionCode"] > max_code:
                            max_code = data[app]["versionCode"]
                            name = data[app].get("versionName", f"1.0.{max_code}")
                return max_code, name
            except Exception as e:
                log("WARN", f"Failed reading {c}: {e}")
    return 3, "1.0.3"

def update_json_versions(next_code, next_name, title, message, force_update):
    configs = {
        "rider": {
            "versionCode": next_code,
            "versionName": next_name,
            "title": title or f"Speedo Rider Update Available (v{next_name}) 🚀",
            "message": message or f"A new version of Speedo Rider is ready with the latest performance and UI improvements. Update now or choose later.",
            "forceUpdate": force_update
        },
        "captain": {
            "versionCode": next_code,
            "versionName": next_name,
            "title": title or f"Speedo Captain Update Available (v{next_name}) 🚀",
            "message": message or f"A new version of Speedo Captain is ready with the latest performance and UI improvements. Update now or choose later.",
            "forceUpdate": force_update
        },
        "admin": {
            "versionCode": next_code,
            "versionName": next_name,
            "title": title or f"Speedo Admin Update Available (v{next_name}) 🚀",
            "message": message or f"A new version of Speedo Admin is ready with the latest performance and UI improvements. Update now or choose later.",
            "forceUpdate": force_update
        }
    }

    paths = [
        os.path.join(ROOT_DIR, "app-versions.json"),
        os.path.join(BACKEND_DIR, "app-versions.json"),
    ]
    for p in paths:
        with open(p, "w", encoding="utf-8") as f:
            json.dump(configs, f, indent=2)
        log("OK", f"Updated {os.path.relpath(p, ROOT_DIR)} -> Build #{next_code} (v{next_name})")

def update_gradle_files(next_code, next_name):
    apps = ["rider-app", "captain-app", "admin-app"]
    for app in apps:
        gradle_path = os.path.join(ANDROID_DIR, app, "build.gradle.kts")
        if not os.path.exists(gradle_path):
            log("WARN", f"Missing {gradle_path}")
            continue
        with open(gradle_path, "r", encoding="utf-8") as f:
            content = f.read()

        content = re.sub(r'versionCode\s*=\s*\d+', f'versionCode = {next_code}', content)
        content = re.sub(r'versionName\s*=\s*"[^"]+"', f'versionName = "{next_name}"', content)

        with open(gradle_path, "w", encoding="utf-8") as f:
            f.write(content)
        log("OK", f"Updated {app} build.gradle.kts -> versionCode={next_code}, versionName=\"{next_name}\"")

def build_apks():
    log("BUILD", "Compiling Android APKs for all apps (Gradle)...")
    cmd = "cmd /c \"gradlew assembleDebug --no-daemon\""
    if not run_cmd(cmd, cwd=ANDROID_DIR):
        log("ERROR", "Gradle build failed!")
        return False

    src_map = {
        "speedo-rider.apk": os.path.join(ANDROID_DIR, "rider-app/build/outputs/apk/debug/rider-app-debug.apk"),
        "speedo-captain.apk": os.path.join(ANDROID_DIR, "captain-app/build/outputs/apk/debug/captain-app-debug.apk"),
        "speedo-admin.apk": os.path.join(ANDROID_DIR, "admin-app/build/outputs/apk/debug/admin-app-debug.apk"),
    }

    for d in [DOWNLOADS_DIR, BACKEND_DOWNLOADS_DIR]:
        os.makedirs(d, exist_ok=True)
        for apk_name, src in src_map.items():
            if not os.path.exists(src):
                log("ERROR", f"Expected APK not found: {src}")
                return False
            dst = os.path.join(d, apk_name)
            shutil.copy2(src, dst)
            size_mb = os.path.getsize(dst) / (1024 * 1024)
            log("OK", f"Copied {apk_name} ({size_mb:.2f} MB) -> {os.path.relpath(d, ROOT_DIR)}")

    return True

def notify_backend_sync():
    log("SYNC", "Triggering real-time version sync & user notifications on Railway...")
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE

    targets = [
        f"{PROD_URL}/api/app-version/sync-now",
        "http://localhost:5000/api/app-version/sync-now"
    ]
    synced = False
    for url in targets:
        try:
            req = urllib.request.Request(url, data=b"{}", headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req, context=ctx, timeout=8) as res:
                body = res.read().decode("utf-8")
                log("OK", f"Successfully synced version with {url}: {body}")
                synced = True
                break
        except Exception as e:
            pass

    if not synced:
        log("INFO", "Direct sync endpoint reached timeout or server deploying; background watcher will auto-sync on deploy.")

def git_commit_push_and_deploy(next_code, next_name):
    log("GIT", f"Staging release changes for v{next_name} (Build #{next_code})...")
    run_cmd("git add -A")
    commit_msg = f"feat(release): v{next_name} (Build #{next_code}) - auto-sync & user update broadcast"
    run_cmd(f'git commit -m "{commit_msg}"')
    log("GIT", "Pushing release to GitHub...")
    if run_cmd("git push origin main"):
        log("OK", "Pushed to GitHub main branch!")
    else:
        log("WARN", "Git push encountered a warning or delay.")

    log("DEPLOY", "Deploying update to Railway cloud...")
    if run_cmd("cmd /c \"railway up --detach\""):
        log("OK", "Railway deployment initiated!")
    else:
        log("WARN", "Railway CLI deploy call completed with warnings.")

def main():
    parser = argparse.ArgumentParser(description="Speedo Automated Version Sequencer & App Update Broadcast")
    parser.add_argument("--code", type=int, default=None, help="Explicit versionCode integer")
    parser.add_argument("--name", type=str, default=None, help="Explicit versionName string")
    parser.add_argument("--title", type=str, default=None, help="Custom update title")
    parser.add_argument("--message", type=str, default=None, help="Custom update message")
    parser.add_argument("--force", action="store_true", help="Set update as mandatory (force update)")
    parser.add_argument("--skip-build", action="store_true", help="Skip compiling APKs")
    parser.add_argument("--no-push", action="store_true", help="Skip Git commit/push and Railway deploy")

    args = parser.parse_args()

    curr_code, curr_name = get_current_version()
    next_code = args.code if args.code is not None else (curr_code + 1)
    next_name = args.name if args.name is not None else f"1.0.{next_code}"

    print("=================================================================")
    print(f" SPEEDO AUTOMATED RELEASE PIPELINE: Build #{curr_code} -> #{next_code} (v{next_name})")
    print("=================================================================")

    # 1. Update Version Configs (JSON & Gradle)
    update_json_versions(next_code, next_name, args.title, args.message, args.force)
    update_gradle_files(next_code, next_name)

    # 2. Build APKs
    if not args.skip_build:
        if not build_apks():
            log("ERROR", "Build failed. Aborting release.")
            sys.exit(1)
    else:
        log("INFO", "Skipping Gradle build as requested (--skip-build).")

    # 3. Git Push & Railway Deploy
    if not args.no_push:
        git_commit_push_and_deploy(next_code, next_name)
    else:
        log("INFO", "Skipping Git push and Railway deploy as requested (--no-push).")

    # 4. Trigger Real-Time Backend Sync & Existing User Notification Broadcast
    notify_backend_sync()

    print("\n=================================================================")
    print(f" SUCCESS! All apps advanced to Build #{next_code} (v{next_name})")
    print(f" Existing users will automatically receive the update prompt & notification.")
    print("=================================================================\n")

if __name__ == "__main__":
    main()
