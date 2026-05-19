# SyAi Local AI Stack Runbook

This runbook starts the full local AI workflow for the notes editor:

- Android app on phone
- Spring Boot AI router on laptop, port `8088`
- Ollama for text and vision models, port `11434`
- FastAPI for DreamShaper image generation and Faster-Whisper transcription, port `8000`

The intended network flow is:

```text
Android App
  -> Spring Boot image-generator :8088
  -> Ollama :11434
  -> FastAPI :8000
  -> DreamShaper / Faster-Whisper
```

## 1. Requirements

Install these first:

- Java 17
- Android Studio / Android SDK
- Python 3.10 or 3.11
- Git
- Ollama
- NVIDIA driver and CUDA-capable PyTorch if using the RTX 3050 laptop

Recommended models:

| Task | Model |
| --- | --- |
| Text-to-text | `phi3` |
| Image+text-to-text | `llava:phi3:3.8b` |
| Text-to-image | DreamShaper / `Lykon/DreamShaper` |
| Img2Img | DreamShaper |
| Voice-to-text | Faster-Whisper `base` first, `small` if quality is more important |

## 2. Find Laptop LAN IP

Run this on the laptop:

```powershell
ipconfig
```

Find the active Wi-Fi adapter and copy the IPv4 address, for example:

```text
192.168.2.75
```

Your phone and laptop must be on the same Wi-Fi.

## 3. Configure Android App Host URL

Edit:

```text
local.properties
```

Set:

```properties
LOCAL_IMAGE_GENERATOR_BASE_URL=http://192.168.2.75:8088/
startLocalServer=false
```

Replace `192.168.2.75` with your laptop IPv4 address.

Keep `startLocalServer=false` if you are starting the backend manually. This is usually cleaner while debugging.

## 4. Start Ollama

Open a terminal and verify Ollama is available:

```powershell
ollama --version
```

Pull the required models:

```powershell
ollama pull phi3
ollama pull llava:phi3:3.8b
```

Check installed models:

```powershell
ollama list
```

Important: Ollama model names must match exactly. Some installs expose the Phi-3 LLaVA model as `llava-phi3`, `llava-phi3:latest`, `llava:latest`, or another tag instead of `llava:phi3:3.8b`.

If Spring Boot logs this:

```text
Llava Vision returned non-200 status code: 400
{"error":"invalid model name"}
```

Run:

```powershell
ollama list
curl http://localhost:11434/api/tags
```

Then use one of the exact names shown there. The backend now tries to auto-detect installed vision models in this order:

```text
llava:phi3:3.8b
llava-phi3:latest
llava-phi3
llava:latest
llava
bakllava:latest
bakllava
any installed model containing llava/vision/bakllava
```

Quick text model test:

```powershell
ollama run phi3 "Write one sentence about photosynthesis."
```

Quick vision model availability test:

```powershell
ollama run llava:phi3:3.8b
```

Ollama normally serves HTTP at:

```text
http://localhost:11434
```

Health check:

```powershell
curl http://localhost:11434/api/tags
```

## 5. Setup Python FastAPI Server

From repo root:

```powershell
cd image-generator
```

Create venv if not already created:

```powershell
python -m venv venv
```

Activate it:

```powershell
.\venv\Scripts\activate
```

Install dependencies:

```powershell
pip install -r python\requirements.txt
```

For RTX 3050, install CUDA PyTorch from the official PyTorch selector if the default `torch` package does not detect CUDA.

Check CUDA:

```powershell
python -c "import torch; print(torch.cuda.is_available()); print(torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'CPU')"
```

Start FastAPI:

```powershell
python python\fastapi_server.py
```

Expected URL:

```text
http://localhost:8000
```

Health check from another terminal:

```powershell
curl http://localhost:8000/health
```

## 6. Start Spring Boot AI Router

Open a new terminal.

From repo root:

```powershell
cd image-generator
```

Start Spring Boot:

```powershell
.\gradlew.bat bootRun
```

Expected URL:

```text
http://localhost:8088
```

Health check:

```powershell
curl http://localhost:8088/api/v1/images/health
```

From the phone browser, test:

```text
http://192.168.2.75:8088/api/v1/images/health
```

If phone cannot connect, allow Windows Firewall inbound access for port `8088`.

## 7. Optional Firewall Commands

Run PowerShell as Administrator:

```powershell
New-NetFirewallRule -DisplayName "SyAi Spring Boot 8088" -Direction Inbound -Protocol TCP -LocalPort 8088 -Action Allow
New-NetFirewallRule -DisplayName "SyAi FastAPI 8000" -Direction Inbound -Protocol TCP -LocalPort 8000 -Action Allow
```

Usually the phone only needs `8088`; Spring Boot calls FastAPI locally on the laptop.

## 8. Run Android App

From repo root:

```powershell
.\gradlew.bat :app:installDebug
```

Or run from Android Studio.

Make sure the phone grants:

- Microphone permission
- Notification permission on Android 13+
- Network access is normal because the app uses cleartext LAN HTTP

## 9. AI Tool Flow Test

In the Note Editor:

1. Open AI tool.
2. Type: `Write a short checklist about photosynthesis`.
3. Tap `Continuous Stream Optimization`.
4. Expected behavior: text streams into the current page as append-only chunks.

Vision prompt test:

1. Open AI tool.
2. Attach one image.
3. Type: `Explain what is in this image and create study notes`.
4. Expected behavior: Spring sends image context to `llava:phi3:3.8b`, then streams text output.

Drawing test:

```text
Draw a simple labeled plant cell diagram
```

Expected behavior: backend creates one drawing object and streams point chunks into that object instead of creating overlapping duplicate strokes.

Image generation test:

```text
Generate a clean biology study illustration of a plant cell
```

Use the `Local Image Gen` tab. This calls DreamShaper through the image-generation job path.

Mic test:

1. Tap mic icon in AI sheet.
2. Speak.
3. Tap `Stop & Transcribe`.
4. Expected behavior: Faster-Whisper returns transcript into the prompt field.

## 10. Important URLs

| Service | URL |
| --- | --- |
| Android backend base | `http://<LAPTOP_IP>:8088/` |
| Spring health | `http://<LAPTOP_IP>:8088/api/v1/images/health` |
| AI WebSocket | `ws://<LAPTOP_IP>:8088/api/v1/ai/stream` |
| Transcription | `http://<LAPTOP_IP>:8088/api/v1/ai/transcribe` |
| FastAPI health | `http://localhost:8000/health` |
| Ollama models | `http://localhost:11434/api/tags` |

## 11. Common Fixes

If Android logs this:

```text
sent ping but didn't receive pong within 20000ms
```

The model call was taking longer than the old WebSocket heartbeat window. The app now uses a longer ping interval and retries reconnecting every 30 seconds for up to 1 hour. The backend also runs AI work on background threads so WebSocket handling is not blocked by long LLaVA/DreamShaper calls.

The WebSocket protocol also includes a `sessionId` now. Android ignores stale frames from old sessions after reconnecting, and Spring echoes progress events such as:

```json
{"type":"PROGRESS","message":"Running LLaVA vision analysis on attached images...","progress":35}
```

If it still happens:

```powershell
curl http://localhost:11434/api/tags
curl http://localhost:8000/health
curl http://localhost:8088/api/v1/images/health
```

Then fully restart Spring Boot:

```powershell
cd C:\Users\Vidhan\AndroidStudioProjects\SyAi-Public\image-generator
.\gradlew.bat bootRun
```

If Android cannot connect:

```powershell
ipconfig
curl http://localhost:8088/api/v1/images/health
```

Then test from phone browser:

```text
http://<LAPTOP_IP>:8088/api/v1/images/health
```

If Ollama model missing:

```powershell
ollama pull phi3
ollama pull llava:phi3:3.8b
```

If CUDA is not detected:

```powershell
.\venv\Scripts\activate
python -c "import torch; print(torch.cuda.is_available())"
```

Install CUDA PyTorch from:

```text
https://pytorch.org/get-started/locally/
```

If Faster-Whisper fails on audio:

```powershell
.\venv\Scripts\activate
pip install faster-whisper
```

Make sure `ffmpeg` is installed and available:

```powershell
ffmpeg -version
```

If it is missing, install it:

```powershell
winget install Gyan.FFmpeg
```

## 12. Recommended Start Order

Use this order every time:

```powershell
# Terminal 1
ollama list
ollama run phi3 "ready"
```

```powershell
# Terminal 2
cd C:\Users\Vidhan\AndroidStudioProjects\SyAi-Public\image-generator
.\venv\Scripts\activate
python python\fastapi_server.py
```

```powershell
# Terminal 3
cd C:\Users\Vidhan\AndroidStudioProjects\SyAi-Public\image-generator
.\gradlew.bat bootRun
```

```powershell
# Terminal 4, optional
cd C:\Users\Vidhan\AndroidStudioProjects\SyAi-Public
.\gradlew.bat :app:installDebug
```

Then open the app and use AI Tool in the editor.
