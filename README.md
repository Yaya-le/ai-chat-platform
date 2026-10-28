# AI 聊天平台

一个基于 Vue + Spring Boot + FastAPI 的全栈 AI 聊天应用。

## 技术栈

| 层级 | 技术 | 端口 |
|---|---|---|
| 前端 | Vue 3 + Vite + Element Plus | 5173 |
| 后端 | Java Spring Boot + MyBatis Plus | 8080 |
| AI引擎 | Python FastAPI + OpenAI SDK | 8001 |
| 数据库 | MySQL 8.0 | 3306 |

## 项目结构

```
ai-chat-platform/
├── ai-backend/          # Java后端服务
│   ├── src/main/java/...
│   ├── ai-chat.sql      # 数据库脚本
│   └── pom.xml
├── ai-frontend/         # Vue前端
│   ├── src/
│   └── package.json
├── ai-engine/           # Python AI引擎
│   ├── main.py
│   └── .env.example     # 配置模板
└── README.md
```

## 环境要求

- JDK 17+
- Node.js 16+
- Python 3.10+
- MySQL 8.0+

## 快速开始

### 第一步：配置数据库

1. 启动MySQL服务
2. 新建数据库 `ai_chat1`（或者直接运行脚本自动创建）
3. 导入 `ai-backend/ai-chat.sql` 脚本

```bash
# 命令行导入方式
mysql -u root -p < ai-backend/ai-chat.sql
```

脚本会自动创建三张表：
- `t_user` 用户表
- `t_chat_session` 会话表
- `t_chat_message` 消息表

### 第二步：配置AI密钥

1. 去 [智谱AI开放平台](https://open.bigmodel.cn/) 注册账号，获取API密钥
2. 复制 `ai-engine/.env.example` 为 `ai-engine/.env`
3. 填入你自己的API密钥

```env
AI_API_KEY=你的API密钥
AI_BASE_URL=https://open.bigmodel.cn/api/paas/v4
AI_MODEL=glm-4-flash
```

### 第三步：启动后端服务

```bash
cd ai-backend

# Maven方式启动
mvn spring-boot:run

# 或者用IDE运行 AiBackendApplication.java
```

看到 `Started AiBackendApplication` 就是启动成功，端口 8080。

### 第四步：启动前端服务

```bash
cd ai-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问 http://localhost:5173 就是前端页面。

### 第五步：启动AI引擎服务

```bash
cd ai-engine

# 安装依赖
pip install fastapi uvicorn openai python-dotenv

# 启动服务
uvicorn main:app --port 8001
```

看到 `Uvicorn running on http://0.0.0.0:8001` 就是启动成功。

## 使用说明

1. 打开浏览器访问 http://localhost:5173
2. 用测试账号登录：
   - 用户名：`jack`
   - 密码：`123456`
3. 点击"新建对话"，输入问题就能和AI聊天了

## 默认账号

| 用户名 | 密码 | 昵称 |
|---|---|---|
| jack | 123456 | 小杰 |

## 常见问题

**Q: 发消息报500错误怎么办？**
A: 检查三个服务是不是都启动了：后端8080、前端5173、AI引擎8001

**Q: AI不回复怎么办？**
A: 检查 `ai-engine/.env` 里的API密钥是不是正确，有没有额度

**Q: 数据库连接失败？**
A: 检查 `ai-backend/src/main/resources/application.yml` 里的数据库用户名密码是不是和你本地一致
