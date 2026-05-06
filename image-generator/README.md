# SyAi Local Image Generator Host

This folder contains a standalone Spring Boot LAN host for Stable Diffusion image generation.

It is designed for this workflow:

- laptop runs the host service
- Android phone acts as client
- both stay on the same Wi-Fi
- the phone calls the laptop at a fixed REST path

## Fixed API Endpoint

Base route:

`http://<HOST_IP>:8088/api/v1/images`

Important:

- the route path is fixed
- the port is fixed to `8088`
- the laptop must expose a stable LAN IP if you want the phone config to stay unchanged

Recommended for a truly stable endpoint:

1. Reserve a static DHCP lease in your router for the laptop
2. Keep the Spring Boot port as `8088`
3. In Android, use a fixed base URL like:
   - `http://192.168.1.50:8088/api/v1/images`

## API

### Health

`GET /api/v1/images/health`

### Submit generation

`POST /api/v1/images/generate`

Example body:

```json
{
  "prompt": "A clean study notes illustration with blue sticky notes and a notebook",
  "negativePrompt": "blurry, distorted, extra hands, text artifacts",
  "width": 512,
  "height": 512,
  "steps": 30,
  "guidanceScale": 8,
  "seed": 12345,
  "pageContext": "Current page discusses biology diagrams and labeling."
}
```

### Job status

`GET /api/v1/images/jobs/{jobId}`

### Download result image

`GET /api/v1/images/files/{fileName}`

## Host Requirements

### Required

- Java 17
- Python 3.10 or 3.11
- pip
- Windows laptop on same Wi-Fi as the phone

### Strongly Recommended for usable performance

- NVIDIA GPU with at least 6 GB VRAM
- CUDA-enabled PyTorch install
- 16 GB system RAM
- 10+ GB free disk space for model cache

### CPU-only mode

CPU-only generation is technically possible, but it will be slow.

If you want CPU mode:

1. change `image-generator.device` in `application.yml` from `cuda` to `cpu`
2. install CPU-compatible PyTorch

## Python Setup

From the `image-generator` folder:

```powershell
python -m venv .venv
.venv\Scripts\activate
pip install -r python\requirements.txt
```

If you have an NVIDIA GPU, install the correct CUDA-enabled `torch` build from PyTorch official instructions.

## Spring Boot Setup

From the `image-generator` folder:

```powershell
gradlew.bat bootRun
```

If this folder does not yet have a Gradle wrapper, either:

- install Gradle locally and run `gradle bootRun`
- or generate a wrapper once with `gradle wrapper`

## Windows Firewall

You may need to allow inbound access on port `8088`.

Without this, your phone may fail to connect even on the same Wi-Fi.

## Stable Diffusion Model Notes

Default model:

- `runwayml/stable-diffusion-v1-5`

You may need:

- Hugging Face login
- acceptance of the model license on Hugging Face
- a token if the model requires authenticated access

Set the token in `src/main/resources/application.yml`:

```yaml
image-generator:
  auth-token: "YOUR_HF_TOKEN"
```

## Android Client Integration Suggestion

Use this base URL in the app:

`http://<HOST_IP>:8088/api/v1/images`

Recommended client flow:

1. POST prompt to `/generate`
2. poll `/jobs/{jobId}`
3. once completed, fetch `/files/{fileName}` or use the returned `imageUrl`
4. insert the image into the current note page

## What You Still Need From Me or You

### I can add next

- Android Retrofit client for this host
- WorkManager polling job
- in-app notification when image generation finishes
- automatic insertion of the generated image into the current page
- request signing or LAN auth if you want to secure the host

### You may need to provide

- your preferred fixed LAN IP
- whether your laptop has NVIDIA GPU or CPU-only mode
- whether you want a lighter model instead of SD 1.5
- whether you want queued multi-image generation

