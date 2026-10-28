from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
from pydantic import BaseModel
from typing import List
from openai import AsyncOpenAI
import os

# 载入配置
load_dotenv()

app = FastAPI(title="AI Engine")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 请求模型
# message: 用户输入的消息
# model: 模型名称
# history: 历史对话
# system: prompt提示词
class ChatRequest(BaseModel):
    message: str
    model: str = "default"
    history: List[dict] = []
    system: str = None

# 配置大模型
AI_API_KEY=os.getenv("AI_API_KEY")
AI_BASE_URL=os.getenv("AI_BASE_URL")
AI_MODEL=os.getenv("AI_MODEL")

# 构造消息
def build_message(message: str, history: List[dict], system: str) -> str:
    msgs = []
    if system:
        msgs.append({"role": "system", "content": system})
    for h in history:
        msgs.append({"role": h["role"], "content": h["content"]})
    msgs.append({"role": "user", "content": message})
    return msgs

# 调用LLM回答问题接口
@app.post("/chat/complete")
async def chat_complete(req: ChatRequest):
    message = build_message(req.message, req.history, req.system)
    print(message)
    response = await AsyncOpenAI(api_key=AI_API_KEY, base_url=AI_BASE_URL).chat.completions.create(
        model=AI_MODEL,
        messages=message
    )
    reply = response.choices[0].message.content
    print(reply)
    return reply

@app.get("/hello")
def hello():
    return {"message": "你好，请问需要什么帮助？"}
