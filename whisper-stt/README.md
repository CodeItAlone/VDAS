# VDAS Whisper STT Microservice

Local speech-to-text server using OpenAI Whisper (`base` model).  
Used by the VDAS Java application for offline voice command transcription.

## Setup

```bash
# 1. Create virtual environment
cd whisper-stt
python -m venv venv

# 2. Activate (Windows)
venv\Scripts\activate

# 3. Install dependencies
pip install -r requirements.txt
```

> **Note**: First run will download the Whisper `base` model (~140 MB).  
> Requires Python 3.9+ and `ffmpeg` installed on PATH.

## Run

```bash
python server.py
```

Server starts on `http://localhost:8000` (single worker).

## Endpoints

| Method | Path          | Description                        |
|--------|---------------|------------------------------------|
| GET    | `/health`     | Returns `{"status": "ok"}`         |
| POST   | `/transcribe` | Accepts WAV, returns `{"text":"…"}` |

## Constraints

- WAV format: 16kHz, mono, 16-bit PCM signed
- Max upload size: 1 MB
- Max audio duration: 5 seconds
- Single concurrent inference (locked)
