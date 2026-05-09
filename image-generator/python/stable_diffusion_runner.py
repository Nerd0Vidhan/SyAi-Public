import argparse
import json
import os
import random


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--request", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--model-id", required=True)
    parser.add_argument("--device", default="cuda")
    return parser.parse_args()


def main():
    args = parse_args()

    with open(args.request, "r", encoding="utf-8") as f:
        request = json.load(f)

    from diffusers import StableDiffusionPipeline
    import torch

    dtype = torch.float16 if args.device.startswith("cuda") else torch.float32

    pipe = StableDiffusionPipeline.from_pretrained(
        args.model_id,
        torch_dtype=dtype
    )

    pipe.enable_attention_slicing()
    pipe.enable_vae_slicing()

    if args.device.startswith("cuda"):
        pipe = pipe.to(args.device)

    seed = request.get("seed")
    if seed is None:
        seed = random.randint(1, 2_147_483_647)

    generator = torch.Generator(device=args.device).manual_seed(seed)

    prompt = request["prompt"]
    page_context = (request.get("pageContext") or "").strip()
    if page_context:
        prompt = f"{prompt}\n\nContext from note page:\n{page_context}"

    try :

        width = int(request.get("width", 256))
        height = int(request.get("height", 256))

        width = max(128, min(width, 1024))
        height = max(128, min(height, 1024))

        width = (width // 8) * 8
        height = (height // 8) * 8

        steps = int(request.get("steps", 20))
        steps = max(1, min(steps, 50))
        guidance_scale = float(request.get("guidanceScale", 8))
        guidance_scale = max(1.0, min(guidance_scale, 20.0))

        image = pipe(
            prompt=prompt,
            negative_prompt=request.get("negativePrompt"),
            width=width,
            height=height,
            num_inference_steps=steps,
            guidance_scale=guidance_scale,
            generator=generator
        ).images[0]
    except Exception as e:
        print(f"Generation failed: {e}")
        raise

    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    image.save(args.output)
    print(args.output)


if __name__ == "__main__":
    main()
