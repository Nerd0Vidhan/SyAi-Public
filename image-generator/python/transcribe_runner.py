import sys
import argparse
import os

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--audio", required=True)
    parser.add_argument("--model", default="base")
    args = parser.parse_args()
    
    if not os.path.exists(args.audio):
        print(f"Error: Audio file not found at {args.audio}", file=sys.stderr)
        sys.exit(1)
        
    try:
        from faster_whisper import WhisperModel
        # Use CUDA if available
        device = "cuda" if os.environ.get("USE_CPU") != "1" else "cpu"
        compute_type = "float16" if device == "cuda" else "int8"
        
        # Load model and transcribe
        model = WhisperModel(args.model, device=device, compute_type=compute_type)
        segments, info = model.transcribe(args.audio, beam_size=5)
        
        text = "".join([segment.text for segment in segments])
        print(text.strip())
    except Exception as e:
        print(f"Transcription failed: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
