# English Speaking Partner / AI 英语口语搭子

一个用于英语口语训练的 Android App。用户可以选择初中、高中、大学难度，像打电话一样和 AI 进行英文对话。每次用户用语音或文字说一句英文后，系统会返回中文翻译、评分、问题点、更自然表达、中文解释，以及 AI 的下一句英文回复。

## 功能列表

- 难度选择：初中、高中、大学
- 话题选择：日常生活、校园生活、兴趣爱好、旅行、食物、购物、面试、自由聊天、自定义话题
- 通话式练习页：AI 英文回复、中文翻译、用户句子分析、语音输入、文字输入、重播、暂停/继续
- 每句话评分：总分、地道程度、清楚程度、语法、词汇、难度匹配
- 历史记录：保存时间、难度、话题、用户句子、评分、地道表达和 AI 回复
- 设置：后端 API 地址、AI 音色、语速、自动播放、中文翻译、历史保存、清空历史
- Mock 模式：没有 MiMo API Key 时也能演示完整流程

## 技术栈

- Android：Kotlin、Jetpack Compose、Material 3、MVVM、StateFlow
- 网络：Retrofit、OkHttp、Gson
- 本地：Room、DataStore
- 语音：Android SpeechRecognizer、MediaPlayer
- 后端：FastAPI、httpx、python-dotenv
- CI：GitHub Actions、JDK 17、Gradle

## 安卓端运行方法

```bash
cd android
./gradlew :app:assembleDebug
```

Android Studio 打开 `android/` 目录也可以直接运行。模拟器访问本机后端默认使用：

```text
http://10.0.2.2:8000/
```

真机调试时，请在 App 设置页把后端地址改成电脑局域网地址，例如：

```text
http://192.168.1.8:8000/
```

## 后端运行方法

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

Windows PowerShell：

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

健康检查：

```bash
curl http://127.0.0.1:8000/health
```

## 配置 MiMo API Key

复制 `backend/.env.example` 为 `backend/.env`，然后填写：

```env
MIMO_API_KEY=your_api_key_here
MIMO_BASE_URL=https://your-mimo-api-base-url
MIMO_CHAT_MODEL=MiMo-V2.5-Pro
MIMO_TTS_MODEL=MiMo-V2.5-TTS
```

后端优先使用 `MiMo-V2.5-Pro` 做主对话和评分，使用 `MiMo-V2.5-TTS` 生成英文语音。真实接口按 OpenAI-compatible 风格预留：`/chat/completions` 和 `/audio/speech`。如果你的 MiMo 服务路径不同，只需要改 `backend/main.py` 里的请求路径和 payload。

## 为什么不能把 API Key 写进 APK

APK 可以被反编译。如果把 API Key 写进 Android 代码、Gradle 配置或资源文件，别人拿到 APK 后就可能提取密钥并盗用额度。所以 Android App 只请求自己的后端：

- `POST /api/chat`
- `POST /api/tts`
- `GET /health`

真正的 MiMo API Key 只保存在服务器环境变量或部署平台的 Secret 中，不提交到 GitHub。

## GitHub Actions 自动打包 APK

仓库已经包含：

```text
.github/workflows/android-apk.yml
```

触发方式：

- push 到 `main`
- pull_request 到 `main`
- 手动 `workflow_dispatch`

构建命令：

```bash
cd android
./gradlew :app:assembleDebug --no-daemon
```

Artifact 名称：

```text
EnglishSpeakingPartner-debug-apk
```

APK 路径：

```text
android/app/build/outputs/apk/debug/*.apk
```

## 如何下载 APK

进入 GitHub 仓库：

```text
Actions -> 选择最新构建 -> Artifacts -> 下载 EnglishSpeakingPartner-debug-apk
```

下载后解压，即可得到 debug APK。

## 推送到 GitHub

```bash
git init
git add .
git commit -m "Initial English Speaking Partner app"
git branch -M main
git remote add origin https://github.com/putao-lw/speak_eng.git
git push -u origin main
```

不要提交 `backend/.env`。如果需要线上部署，请在服务器或 GitHub Secrets 中配置 `MIMO_API_KEY`。

## 后续可扩展功能

- 发音评分
- 口音选择
- CET-4/CET-6 口语模式
- 雅思口语模式
- 面试英语模式
- 单词本
- 错题本
- 每日练习报告
