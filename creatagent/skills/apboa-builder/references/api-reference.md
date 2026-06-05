# Apboa Platform API Reference

## Authentication

### Login

```
POST /api/auth/login
Content-Type: application/json

{"username": "<username>", "password": "<password>"}
```

Response: `{ "data": { "accessToken": "..." } }`

## Skills

### List Skills

```
GET /api/skill/page?page=1&size=1000&enabled=true
Authorization: Bearer <token>
```

### Upload Skills

```
POST /api/skill/import/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data

file: skills.zip (containing skills/<name>/SKILL.md)
category: "分类名"
cover: "true"
```

**Use Python `requests`** — `curl.exe -F` corrupts Chinese field values on Windows.

## Models

### List Model Configs

```
GET /api/model/config/page?page=1&size=1000&enabled=true
Authorization: Bearer <token>
```

## Prompt Templates

### List Prompt Templates

```
GET /api/prompt/template/page?page=1&size=1000&enabled=true
Authorization: Bearer <token>
```

## Agents

### List Agents

```
GET /api/agent/definition/page?page=1&size=1000
Authorization: Bearer <token>
```

### Create Agent

```
POST /api/agent/definition
Authorization: Bearer <token>
Content-Type: application/json; charset=utf-8

{see payload template in SKILL.md Step 7}
```

### Update Agent

```
PUT /api/agent/definition
Authorization: Bearer <token>
Content-Type: application/json; charset=utf-8

{same as create, but include "id" field}
```
