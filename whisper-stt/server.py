"""
VDAS Whisper STT Microservice
=============================
Local speech-to-text server using OpenAI Whisper (base model).
Exposes a single POST /transcribe endpoint for WAV audio files.

Safety constraints:
- Model loaded ONCE at startup
- Max upload size: 1 MB
- Max audio duration: 5 seconds
- Single-worker to prevent concurrent Whisper execution
- No system command execution — no os.system, subprocess, etc.
"""

import io
import threading
import wave

import numpy as np
import whisper
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.responses import JSONResponse

# ── Constants ──────────────────────────────────────────────────────
WHISPER_MODEL = "base"
MAX_UPLOAD_BYTES = 1 * 1024 * 1024  # 1 MB
MAX_AUDIO_DURATION_SEC = 5.0
ALLOWED_SAMPLE_RATE = 16000
ALLOWED_CHANNELS = 1
ALLOWED_SAMPLE_WIDTH = 2  # 16-bit = 2 bytes

# ── App & Model ───────────────────────────────────────────────────
app = FastAPI(title="VDAS Whisper STT", version="1.0.0")

print(f"[STT] Loading Whisper model '{WHISPER_MODEL}'...")
model = whisper.load_model(WHISPER_MODEL)
print(f"[STT] Model loaded successfully.")

# Lock to serialize Whisper inference (single-worker safety)
_inference_lock = threading.Lock()


@app.get("/health")
def health():
    """Health check endpoint for Java startup verification."""
    return {"status": "ok"}


@app.post("/transcribe")
async def transcribe(file: UploadFile = File(...)):
    """
    Accepts a WAV file (16kHz, mono, 16-bit PCM signed).
    Returns {"text": "<lowercase trimmed transcription>"}.
    """

    # ── 1. Validate content type ──────────────────────────────────
    if file.content_type and file.content_type not in (
        "audio/wav",
        "audio/wave",
        "audio/x-wav",
        "application/octet-stream",
    ):
        raise HTTPException(
            status_code=415,
            detail=f"Unsupported media type: {file.content_type}. Expected WAV audio.",
        )

    # ── 2. Read and enforce size limit ────────────────────────────
    raw_bytes = await file.read()
    if len(raw_bytes) > MAX_UPLOAD_BYTES:
        raise HTTPException(
            status_code=413,
            detail=f"File too large: {len(raw_bytes)} bytes. Max: {MAX_UPLOAD_BYTES} bytes.",
        )

    if len(raw_bytes) == 0:
        raise HTTPException(status_code=400, detail="Empty file received.")

    # ── 3. Validate WAV structure and extract PCM frames ──────────
    try:
        with wave.open(io.BytesIO(raw_bytes), "rb") as wf:
            sample_rate = wf.getframerate()
            channels = wf.getnchannels()
            sample_width = wf.getsampwidth()
            n_frames = wf.getnframes()
            duration_sec = n_frames / sample_rate

            if sample_rate != ALLOWED_SAMPLE_RATE:
                raise HTTPException(
                    status_code=400,
                    detail=f"Invalid sample rate: {sample_rate}. Expected {ALLOWED_SAMPLE_RATE}.",
                )
            if channels != ALLOWED_CHANNELS:
                raise HTTPException(
                    status_code=400,
                    detail=f"Invalid channels: {channels}. Expected mono ({ALLOWED_CHANNELS}).",
                )
            if sample_width != ALLOWED_SAMPLE_WIDTH:
                raise HTTPException(
                    status_code=400,
                    detail=f"Invalid sample width: {sample_width} bytes. Expected {ALLOWED_SAMPLE_WIDTH} (16-bit).",
                )
            if duration_sec > MAX_AUDIO_DURATION_SEC:
                raise HTTPException(
                    status_code=400,
                    detail=f"Audio too long: {duration_sec:.1f}s. Max: {MAX_AUDIO_DURATION_SEC}s.",
                )

            # Read raw PCM frames directly — no ffmpeg needed
            pcm_bytes = wf.readframes(n_frames)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=400, detail=f"Invalid WAV file: {str(e)}"
        )

    # ── 4. Convert PCM to float32 numpy array ─────────────────────
    # 16-bit signed PCM → float32 normalized to [-1.0, 1.0]
    audio_np = np.frombuffer(pcm_bytes, dtype=np.int16).astype(np.float32) / 32768.0

    # Pad or trim to exactly 30 seconds (Whisper's expected input length)
    # whisper.pad_or_trim handles this cleanly
    audio_np = whisper.pad_or_trim(audio_np)

    # ── 5. Transcribe with lock ───────────────────────────────────
    acquired = _inference_lock.acquire(timeout=10)
    if not acquired:
        raise HTTPException(
            status_code=503,
            detail="STT service busy. Try again shortly.",
        )
    try:
        # Pass numpy array directly — bypasses load_audio() / ffmpeg entirely
        mel = whisper.log_mel_spectrogram(audio_np, model.dims.n_mels).to(model.device)
        options = whisper.DecodingOptions(language="en", fp16=False)
        result = whisper.decode(model, mel, options)
    except Exception as e:
        print(f"[STT] Transcription error: {e}")
        raise HTTPException(status_code=500, detail=f"Transcription failed: {str(e)}")
    finally:
        _inference_lock.release()

    # ── 6. Return lowercase trimmed text ──────────────────────────
    # whisper.decode returns a DecodingResult object, not a dict
    text = result.text.strip().lower()
    return JSONResponse(content={"text": text})


# ── Entry point ───────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn

    # Single worker to prevent concurrent Whisper execution
    uvicorn.run(
        "server:app",
        host="0.0.0.0",
        port=8000,
        workers=1,
        log_level="info",
    )
