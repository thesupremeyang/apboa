# Apboa Agent 助手部署说明

本文档用于将 Apboa Agent 助手部署到服务器，并保证以下 3 个核心工作流可用：

1. 通过自然语言创建智能体：查询平台技能和模型，必要时搜索或创建技能，上传技能后创建智能体，并自动写入系统提示词。
2. 搜索并上传技能：通过 `find-skills-wzr-999` 搜索社区技能，安装、打包并上传到 Apboa 平台。
3. 创建并上传技能：通过 `skill-creator` 创建本地技能，按 Apboa 要求打包并上传。

## 项目结构

```text
creatagent/
├── my-agent/                 # Java Web 服务，提供聊天页面和 /api/chat 接口
│   ├── pom.xml
│   ├── skills/               # Agent 可调用的本地 skills
│   ├── temp_prompts/         # 创建智能体时生成的系统提示词文件
│   └── src/main/
│       ├── java/com/myagent/
│       │   ├── ChatServer.java
│       │   └── AgentPrompts.java
│       └── resources/static/
│           └── chat.html
└── opencli-apboa/            # Apboa OpenCLI adapter
    ├── package.json
    └── clis/apboa/
```

## 服务器环境要求

推荐环境：

- 操作系统：Ubuntu 22.04 LTS 或同等 Linux 服务器
- JDK：Java 8 或更高版本，推荐 Java 17
- Maven：3.8+
- Node.js：18+
- npm：9+
- Nginx：用于反向代理和接入现有平台域名
- 出站网络：服务器需要能访问模型服务、Apboa 平台接口、npm registry 和 skills 社区源

检查命令：

```bash
java -version
mvn -version
node -v
npm -v
nginx -v
```

## 关键运行配置

当前代码中模型配置写在以下 Java 文件里：

```text
my-agent/src/main/java/com/myagent/ChatServer.java
my-agent/src/main/java/com/myagent/App.java
my-agent/src/main/java/com/myagent/TestWorkflows.java
```

核心字段：

```java
static final String BASE_URL = "https://token-plan-sgp.xiaomimimo.com";
static final String API_KEY = "...";
static final String MODEL = "mimo-v2.5-pro";
static final int PORT = 8080;
```

生产环境建议后续改成环境变量读取，避免 API Key 写在源码中。当前部署可以先按现有代码运行。

Apboa 平台登录 token 存放在：

```text
~/.apboa/token
```

如果 `opencli apboa ...` 提示未登录，需要在服务器上重新登录。

## 部署步骤

### 1. 上传代码到服务器

假设部署目录为 `/opt/apboa-agent`：

```bash
sudo mkdir -p /opt/apboa-agent
sudo chown -R "$USER":"$USER" /opt/apboa-agent
```

将本地 `creatagent` 目录上传到服务器，例如：

```bash
scp -r C:/Users/14420/Desktop/creatagent/* user@server:/opt/apboa-agent/
```

服务器上确认结构：

```bash
cd /opt/apboa-agent
ls
# 应看到 my-agent 和 opencli-apboa
```

### 2. 安装 OpenCLI adapter

```bash
cd /opt/apboa-agent/opencli-apboa
npm install
npm install -g @jackwener/opencli
```

将本项目的 Apboa adapter 同步到 OpenCLI 用户目录：

```bash
mkdir -p ~/.opencli/clis/apboa
cp -r /opt/apboa-agent/opencli-apboa/clis/apboa/* ~/.opencli/clis/apboa/
```

验证 adapter 是否可用：

```bash
opencli apboa model-list -f json
```

如果提示未登录，执行：

```bash
opencli apboa login <你的账号> --password <你的密码>
```

再次验证：

```bash
opencli apboa model-list -f json
opencli apboa skill-list --size 5 -f json
```

### 3. 安装 skills CLI

工作流 B 需要用 `npx skills find` 和 `npx skills add` 搜索、安装社区技能。先验证：

```bash
npx skills --help
npx skills find docker
```

如果服务器首次运行较慢，通常是 npm 正在下载依赖。确保服务器可以访问 npm registry。

### 4. 构建 Java 服务

```bash
cd /opt/apboa-agent/my-agent
mvn test
mvn package
```

当前推荐启动方式是使用 Maven 执行 `ChatServer`：

```bash
mvn exec:java -Dexec.mainClass=com.myagent.ChatServer
```

启动后访问：

```text
http://服务器IP:8080/
```

如果服务器防火墙未开放 8080，需要放行或只通过 Nginx 反代访问。

## 用 systemd 托管服务

创建服务文件：

```bash
sudo nano /etc/systemd/system/apboa-agent.service
```

写入：

```ini
[Unit]
Description=Apboa Agent Assistant
After=network.target

[Service]
Type=simple
WorkingDirectory=/opt/apboa-agent/my-agent
ExecStart=/usr/bin/mvn exec:java -Dexec.mainClass=com.myagent.ChatServer
Restart=always
RestartSec=5
User=YOUR_LINUX_USER
Environment=LANG=en_US.UTF-8
Environment=LC_ALL=en_US.UTF-8

[Install]
WantedBy=multi-user.target
```

将 `YOUR_LINUX_USER` 替换为实际部署用户。

启动：

```bash
sudo systemctl daemon-reload
sudo systemctl enable apboa-agent
sudo systemctl start apboa-agent
sudo systemctl status apboa-agent
```

查看日志：

```bash
journalctl -u apboa-agent -f
```

## Nginx 反向代理

如果要接入现有平台域名，例如：

```text
https://your-domain.com/agent-assistant/
```

可增加 Nginx 配置：

```nginx
location /agent-assistant/ {
    proxy_pass http://127.0.0.1:8080/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

如果直接使用子域名，例如：

```text
https://agent.your-domain.com/
```

可使用：

```nginx
server {
    listen 80;
    server_name agent.your-domain.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

检查并重载：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

建议再使用 Certbot 配置 HTTPS。

## 部署后验收

### 1. Web 页面检查

访问：

```text
http://服务器IP:8080/
```

确认：

- 页面能正常加载。
- 中文显示正常。
- 输入框可以发送消息。
- 页面风格为浅色、简洁后台风格。

### 2. CLI 基础能力检查

```bash
opencli apboa model-list -f json
opencli apboa skill-list --size 5 -f json
```

预期：

- `model-list` 返回至少一个可用模型。
- `skill-list` 返回平台技能列表。

### 3. 工作流 A：创建智能体

在页面中输入：

```text
帮我创建一个测试智能体，名字叫部署验收助手，用来验证系统提示词是否会自动写入。不要关联额外技能。
```

验收点：

- Agent 会查询 `model-list`。
- Agent 会自动生成系统提示词。
- Agent 会调用 `opencli apboa agent-create ... --systemPrompt ... -f json`。
- 创建后会调用 `agent-get` 验证。
- 返回结果中应包含真实 `agentId`。
- `systemPromptLength > 0`。
- `followTemplate = false`。

### 4. 工作流 B：搜索并上传技能

在页面中输入：

```text
帮我搜索 Docker 部署相关的 skill，找到合适的就上传到 Apboa 平台。
```

验收点：

- Agent 会执行 `npx skills find docker` 或相关关键词。
- 找到后安装到本地。
- 打包结构必须是 `skills/<skill-name>/SKILL.md`。
- 调用 `opencli apboa skill-upload <zipPath> --category <category> -f json`。
- 上传成功必须返回非空 `skillId`，或后续 `skill-list` 能查到该技能。

### 5. 工作流 C：创建并上传技能

在页面中输入：

```text
帮我创建一个文本摘要 skill，用户输入长文本后输出三段式摘要，创建完成后上传到 Apboa 平台。
```

验收点：

- Agent 会调用 `skill-creator`。
- 本地生成 `skills/<skill-name>/SKILL.md`。
- 打包 zip 时结构必须是 `skills/<skill-name>/SKILL.md`。
- 上传成功必须返回非空 `skillId`，或后续 `skill-list` 能查到该技能。

## 技能包打包要求

Apboa 上传技能时，zip 内部结构必须类似：

```text
skills/
└── weather/
    └── SKILL.md
```

不要打包成：

```text
SKILL.md
```

错误结构可能导致 CLI 看起来执行了上传，但平台搜索不到技能。当前 `skill-upload` adapter 已经增加校验：如果上传后拿不到真实 `skillId`，会报错，不再假装上传成功。

Linux 打包示例：

```bash
cd /tmp/package-root
mkdir -p skills/weather
cp /path/to/weather/SKILL.md skills/weather/SKILL.md
zip -r weather.zip skills
opencli apboa skill-upload /tmp/package-root/weather.zip --category custom -f json
```

Windows 打包示例：

```powershell
Compress-Archive -Path "C:\tmp\package-root\skills" -DestinationPath "C:\tmp\weather.zip" -Force
opencli apboa skill-upload C:\tmp\weather.zip --category custom -f json
```

## 常见问题

### 1. 页面能打开，但发送消息后没有响应

检查 Java 服务日志：

```bash
journalctl -u apboa-agent -f
```

可能原因：

- 模型 API Key 不可用。
- 服务器无法访问模型 endpoint。
- Agent 调用 CLI 时找不到 `opencli` 或 `npx`。
- 工作目录不是 `/opt/apboa-agent/my-agent`，导致 `skills/` 加载失败。

### 2. 提示 `opencli` 命令不存在

安装并确认路径：

```bash
npm install -g @jackwener/opencli
which opencli
opencli --help
```

如果 systemd 中找不到 npm 全局命令，可以把 `ExecStart` 改成带完整 PATH 的脚本。

### 3. 提示 Apboa 未登录

在运行服务的同一个 Linux 用户下登录：

```bash
opencli apboa login <账号> --password <密码>
cat ~/.apboa/token
```

注意：如果 systemd 使用 `User=apboa`，就必须用 `apboa` 用户登录一次，不能用 root 登录后期望普通用户读取 token。

### 4. 技能上传成功但平台搜不到

检查 zip 结构：

```bash
unzip -l your-skill.zip
```

必须包含：

```text
skills/<skill-name>/SKILL.md
```

同时确认 CLI 返回了非空 `skillId`。

### 5. 创建智能体后系统提示词不显示

用以下命令检查：

```bash
opencli apboa agent-get <agentId> -f json
```

确认：

```text
systemPromptLength > 0
followTemplate = false
```

当前 adapter 已修复：传入 `--systemPrompt` 时会自动设置 `followTemplate=false`。如果旧智能体仍异常，可以执行：

```bash
opencli apboa agent-update <agentId> --systemPrompt @file:/path/to/prompt.txt -f json
```

### 6. 中文乱码

确认服务器 locale：

```bash
locale
```

建议设置：

```bash
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8
```

systemd 服务文件中也建议保留：

```ini
Environment=LANG=en_US.UTF-8
Environment=LC_ALL=en_US.UTF-8
```

## 更新部署

代码更新后：

```bash
cd /opt/apboa-agent/opencli-apboa
npm install
cp -r /opt/apboa-agent/opencli-apboa/clis/apboa/* ~/.opencli/clis/apboa/
npm test

cd /opt/apboa-agent/my-agent
mvn test
mvn package
sudo systemctl restart apboa-agent
sudo systemctl status apboa-agent
```

## 最小健康检查清单

每次部署完成后至少执行：

```bash
cd /opt/apboa-agent/opencli-apboa
npm test

cd /opt/apboa-agent/my-agent
mvn test

opencli apboa model-list -f json
opencli apboa skill-list --size 5 -f json
curl -I http://127.0.0.1:8080/
```

全部通过后，再从浏览器访问页面进行人工验收。

