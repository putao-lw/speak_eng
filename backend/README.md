# Backend

FastAPI backend for English Speaking Partner. Android calls this service instead of calling MiMo directly, so API keys never enter the APK.

## Run

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

On Windows PowerShell:

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

If `MIMO_API_KEY` or `MIMO_BASE_URL` is empty, the backend uses Mock mode and returns demo chat feedback plus a tiny demo WAV audio clip.
