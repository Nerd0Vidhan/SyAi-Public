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
    parser.add_argument("--auth-token", default="")
    return parser.parse_args()


def main():
    args = parse_args()

    with open(args.request, "r", encoding="utf-8") as f:
        request = json.load(f)

    from diffusers import StableDiffusionPipeline
    import torch

    auth_token = args.auth_token.strip() or None
    dtype = torch.float16 if args.device.startswith("cuda") else torch.float32

    pipe = StableDiffusionPipeline.from_pretrained(
        args.model_id,
        torch_dtype=dtype,
        use_auth_token=auth_token
    )
    pipe = pipe.to(args.device)
    if args.device.startswith("cuda"):
        pipe.enable_attention_slicing()

    seed = request.get("seed")
    if seed is None:
        seed = random.randint(1, 2_147_483_647)

    generator = torch.Generator(device=args.device).manual_seed(seed)

    prompt = request["prompt"]
    page_context = (request.get("pageContext") or "").strip()
    if page_context:
        prompt = f"{prompt}\n\nContext from note page:\n{page_context}"

    image = pipe(
        prompt=prompt,
        negative_prompt=request.get("negativePrompt"),
        width=request.get("width", 512),
        height=request.get("height", 512),
        num_inference_steps=request.get("steps", 30),
        guidance_scale=float(request.get("guidanceScale", 8)),
        generator=generator
    ).images[0]

    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    image.save(args.output)
    print(args.output)


if __name__ == "__main__":
    main()
