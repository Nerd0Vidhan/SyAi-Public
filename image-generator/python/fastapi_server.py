import os
import torch
import random
from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import FileResponse, JSONResponse
from diffusers import StableDiffusionPipeline, DPMSolverMultistepScheduler
from faster_whisper import WhisperModel
from PIL import Image

app = FastAPI(title="SyAi Local Backend Server")

# Global models caching
pipe = None
whisper_model = None

def get_sd_pipe():
    global pipe
    if pipe is None:
        model_id = "Lykon/DreamShaper"
        device = "cuda" if torch.cuda.is_available() else "cpu"
        dtype = torch.float16 if device == "cuda" else torch.float32
        
        print(f"Loading DreamShaper model on {device}...")
        pipe = StableDiffusionPipeline.from_pretrained(
            model_id,
            torch_dtype=dtype,
            safety_checker=None,
            requires_safety_checker=False
        )
        pipe.enable_attention_slicing()
        pipe.enable_vae_slicing()
        pipe.scheduler = DPMSolverMultistepScheduler.from_config(pipe.scheduler.config)
        pipe = pipe.to(device)
        print("DreamShaper loaded successfully.")
    return pipe

def get_whisper_model():
    global whisper_model
    if whisper_model is None:
        device = "cuda" if torch.cuda.is_available() else "cpu"
        compute_type = "float16" if device == "cuda" else "int8"
        print(f"Loading Faster-Whisper on {device}...")
        whisper_model = WhisperModel("base", device=device, compute_type=compute_type)
        print("Faster-Whisper loaded successfully.")
    return whisper_model

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/generate")
async def generate(
    prompt: str = Form(...),
    negative_prompt: str = Form("blurry, low quality, bad anatomy, deformed, duplicate"),
    width: int = Form(512),
    height: int = Form(512),
    steps: int = Form(20),
    guidance_scale: float = Form(7.5),
    seed: int = Form(None)
):
    try:
        pipeline = get_sd_pipe()
        device = "cuda" if torch.cuda.is_available() else "cpu"
        if seed is None:
            seed = random.randint(1, 2147483647)
        generator = torch.Generator(device=device).manual_seed(seed)
        
        # Ensure dimensions are multiples of 8
        width = (max(128, min(width, 1024)) // 8) * 8
        height = (max(128, min(height, 1024)) // 8) * 8
        
        image = pipeline(
            prompt=prompt,
            negative_prompt=negative_prompt,
            width=width,
            height=height,
            num_inference_steps=steps,
            guidance_scale=guidance_scale,
            generator=generator
        ).images[0]
        
        output_dir = "generated"
        os.makedirs(output_dir, exist_ok=True)
        output_path = os.path.join(output_dir, f"fastapi_{random.randint(1000,9999)}.png")
        image.save(output_path)
        return FileResponse(output_path, media_type="image/png")
    except Exception as e:
        import traceback
        traceback.print_exc()
        return JSONResponse(status_code=500, content={"error": str(e)})

@app.post("/transcribe")
async def transcribe(audio: UploadFile = File(...)):
    try:
        model = get_whisper_model()
        ext = os.path.splitext(audio.filename or "")[1] or ".m4a"
        temp_path = f"temp_{random.randint(1000,9999)}{ext}"
        with open(temp_path, "wb") as f:
            f.write(await audio.read())
            
        segments, info = model.transcribe(temp_path, beam_size=5)
        text = "".join([segment.text for segment in segments])
        
        if os.path.exists(temp_path):
            os.remove(temp_path)
            
        return {"text": text.strip()}
    except Exception as e:
        import traceback
        traceback.print_exc()
        return JSONResponse(status_code=500, content={"error": str(e)})

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
