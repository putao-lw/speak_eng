import base64
import io
import json
import math
import os
import re
import wave
from typing import Literal

import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

load_dotenv()

MIMO_API_KEY = os.getenv("MIMO_API_KEY", "").strip()
MIMO_BASE_URL = os.getenv("MIMO_BASE_URL", "").rstrip("/")
MIMO_CHAT_MODEL = os.getenv("MIMO_CHAT_MODEL", "MiMo-V2.5-Pro")
MIMO_TTS_MODEL = os.getenv("MIMO_TTS_MODEL", "MiMo-V2.5-TTS")

SYSTEM_PROMPT = """你是一个英语口语陪练 AI，目标是帮助中文母语用户练习自然、地道、能真实交流的英语。你不是严厉的语法老师，而是一个友好、耐心、会纠错的口语搭子。

你需要根据用户选择的难度控制英文表达：

junior 初中：
- 用简单词汇
- 用短句
- 尽量避免复杂从句
- 反馈要非常简单
- 鼓励用户开口

senior 高中：
- 使用中等难度词汇
- 可以使用常见从句
- 鼓励用户把话说完整
- 反馈要指出明显不自然的地方

college 大学：
- 使用更自然、更真实的口语表达
- 可以加入观点、原因、例子
- 反馈要关注地道性、逻辑和表达质量
- 不要使用过难的学术词，除非话题需要

你每次必须输出严格 JSON，不要输出 Markdown，不要输出解释性废话。JSON 字段必须完整。

你必须完成：
1. 翻译用户英文为中文
2. 给用户这句话评分
3. 找出明显错误点或不自然点
4. 给出更地道表达
5. 用中文解释为什么这样更自然
6. 生成下一句 AI 英文回复
7. 生成 AI 回复的中文翻译
8. 给一句中文鼓励

评分时不要机械追求语法满分。只要能自然表达意思，即使语法不是 100% 完美，也可以给较高分。重点评价真实交流效果。"""

app = FastAPI(title="English Speaking Partner Backend")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class HistoryMessage(BaseModel):
    role: Literal["ai", "user"]
    english: str
    chinese: str = ""


class ChatRequest(BaseModel):
    session_id: str
    level: Literal["junior", "senior", "college"]
    topic: str
    user_input: str
    input_type: Literal["voice", "text"]
    history: list[HistoryMessage] = Field(default_factory=list)


class Score(BaseModel):
    total: int
    naturalness: int
    clarity: int
    grammar: int
    vocabulary: int
    level_match: int


class Problem(BaseModel):
    type: Literal["grammar", "vocabulary", "naturalness", "clarity", "pronunciation_hint", "other"]
    original_part: str
    explanation_zh: str


class BetterExpression(BaseModel):
    english: str
    chinese: str
    why_zh: str


class AiReply(BaseModel):
    english: str
    chinese: str


class ChatResponse(BaseModel):
    user_original: str
    user_translation_zh: str
    score: Score
    problems: list[Problem]
    better_expressions: list[BetterExpression]
    ai_reply: AiReply
    encouragement_zh: str


class TtsRequest(BaseModel):
    text: str
    voice: Literal["default_female", "default_male", "custom"] = "default_female"
    speed: Literal["slow", "normal", "fast"] = "normal"


def extract_json(text: str) -> dict:
    """模型偶尔会包裹文本，这里尽量抽出第一个 JSON 对象。"""
    text = text.strip()
    if text.startswith("```"):
        text = re.sub(r"^```(?:json)?", "", text).strip()
        text = re.sub(r"```$", "", text).strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", text, re.S)
        if not match:
            raise
        return json.loads(match.group(0))


def clamp_response(data: dict, request: ChatRequest) -> ChatResponse:
    data.setdefault("user_original", request.user_input)
    data.setdefault("user_translation_zh", "（模拟翻译）" + request.user_input)
    data.setdefault("score", {})
    data["score"].setdefault("total", 82)
    data["score"].setdefault("naturalness", 24)
    data["score"].setdefault("clarity", 22)
    data["score"].setdefault("grammar", 17)
    data["score"].setdefault("vocabulary", 12)
    data["score"].setdefault("level_match", 7)
    data.setdefault("problems", [])
    data.setdefault("better_expressions", [])
    data.setdefault("ai_reply", {})
    data["ai_reply"].setdefault("english", "That sounds good. Can you tell me a little more?")
    data["ai_reply"].setdefault("chinese", "听起来不错。你能再多说一点吗？")
    data.setdefault("encouragement_zh", "说得不错，继续大胆开口。")
    return ChatResponse(**data)


def mock_chat(request: ChatRequest) -> ChatResponse:
    text = request.user_input.strip()
    lower = text.lower()
    has_common_issue = "very like" in lower or "like play" in lower
    problems = []
    better = []
    if has_common_issue:
        problems.append(
            Problem(
                type="naturalness",
                original_part="very like / like play",
                explanation_zh="这个说法能理解，但不太自然。英语里更常说 really like，动作要用 playing。"
            )
        )
        better.append(
            BetterExpression(
                english="I really like playing basketball.",
                chinese="我真的很喜欢打篮球。",
                why_zh="really like 更口语，playing basketball 表示“打篮球这件事”。"
            )
        )
        score = Score(total=68, naturalness=17, clarity=20, grammar=13, vocabulary=10, level_match=8)
    else:
        problems.append(
            Problem(type="other", original_part=text[:30], explanation_zh="整体意思清楚，可以再补充一个原因让对话更自然。")
        )
        better.append(
            BetterExpression(
                english=f"{text.rstrip('.')}, and I can tell you more about it.",
                chinese="我还可以多讲一点。",
                why_zh="在真实聊天里补充细节，会让对方更容易接话。"
            )
        )
        score = Score(total=86, naturalness=25, clarity=23, grammar=18, vocabulary=12, level_match=8)

    if request.level == "junior":
        reply = AiReply("Nice! What do you usually do after school?", "不错！你放学后通常做什么？")
    elif request.level == "senior":
        reply = AiReply("That is interesting. Why do you enjoy it so much?", "这很有意思。你为什么这么喜欢它？")
    else:
        reply = AiReply("I like how you put that. Has this interest changed the way you spend your free time?", "我喜欢你的说法。这个兴趣有没有改变你安排空闲时间的方式？")

    return ChatResponse(
        user_original=text,
        user_translation_zh="我理解你想表达的是：" + text,
        score=score,
        problems=problems,
        better_expressions=better,
        ai_reply=reply,
        encouragement_zh="很好，真实交流最重要的是先把意思说出来，再一点点变自然。"
    )


async def call_mimo_chat(request: ChatRequest) -> ChatResponse:
    if not MIMO_API_KEY or not MIMO_BASE_URL:
        return mock_chat(request)

    user_payload = {
        "level": request.level,
        "topic": request.topic,
        "user_input": request.user_input,
        "input_type": request.input_type,
        "history": [m.model_dump() for m in request.history[-10:]],
        "required_json_schema": ChatResponse.model_json_schema(),
    }
    payload = {
        "model": MIMO_CHAT_MODEL,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": json.dumps(user_payload, ensure_ascii=False)},
        ],
        "temperature": 0.6,
        "response_format": {"type": "json_object"},
    }
    headers = {"Authorization": f"Bearer {MIMO_API_KEY}", "Content-Type": "application/json"}
    async with httpx.AsyncClient(timeout=60) as client:
        response = await client.post(f"{MIMO_BASE_URL}/chat/completions", json=payload, headers=headers)
    if response.status_code >= 400:
        raise HTTPException(status_code=502, detail=f"MiMo chat failed: {response.text[:300]}")
    body = response.json()
    content = body.get("choices", [{}])[0].get("message", {}).get("content", "")
    try:
        return clamp_response(extract_json(content), request)
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"模型返回 JSON 格式不正确: {exc}") from exc


def mock_wav_base64(speed: str) -> str:
    sample_rate = 16000
    duration = {"slow": 0.7, "normal": 0.5, "fast": 0.35}.get(speed, 0.5)
    frames = int(sample_rate * duration)
    buffer = io.BytesIO()
    with wave.open(buffer, "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate)
        for i in range(frames):
            sample = int(12000 * math.sin(2 * math.pi * 440 * i / sample_rate))
            wav.writeframesraw(sample.to_bytes(2, byteorder="little", signed=True))
    return base64.b64encode(buffer.getvalue()).decode("ascii")


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.post("/api/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    return await call_mimo_chat(request)


@app.post("/api/tts")
async def tts(request: TtsRequest):
    if not MIMO_API_KEY or not MIMO_BASE_URL:
        return {"audio_base64": mock_wav_base64(request.speed), "mime_type": "audio/wav"}

    payload = {
        "model": MIMO_TTS_MODEL,
        "input": request.text,
        "voice": request.voice,
        "speed": {"slow": 0.85, "normal": 1.0, "fast": 1.15}[request.speed],
    }
    headers = {"Authorization": f"Bearer {MIMO_API_KEY}", "Content-Type": "application/json"}
    async with httpx.AsyncClient(timeout=60) as client:
        response = await client.post(f"{MIMO_BASE_URL}/audio/speech", json=payload, headers=headers)
    if response.status_code >= 400:
        raise HTTPException(status_code=502, detail=f"MiMo TTS failed: {response.text[:300]}")

    content_type = response.headers.get("content-type", "")
    if content_type.startswith("application/json"):
        return response.json()
    return {
        "audio_base64": base64.b64encode(response.content).decode("ascii"),
        "mime_type": content_type or "audio/mpeg",
    }
