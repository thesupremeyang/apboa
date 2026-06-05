# Apboa 系统管理脚本

## 脚本说明

| 脚本 | 功能 |
|------|------|
| `start-apboa.bat` | 启动完整系统（Redis + 后端 + 前端） |
| `stop-apboa.bat` | 停止系统（后端 + 前端） |
| `status-apboa.bat` | 查看系统运行状态 |
| `start-backend.bat` | 仅启动后端 |
| `start-frontend.bat` | 仅启动前端 |

## 开机自启动

已配置以下任务计划程序任务：

| 任务名 | 触发条件 | 说明 |
|--------|----------|------|
| Apboa System | 系统启动时 | 启动Redis、后端、前端 |
| Apboa Redis | 系统启动时 | 启动Redis服务 |

### 管理任务计划程序任务

```cmd
# 查看任务状态
schtasks /query /tn "Apboa System"

# 手动运行任务
schtasks /run /tn "Apboa System"

# 禁用任务
schtasks /change /tn "Apboa System" /disable

# 启用任务
schtasks /change /tn "Apboa System" /enable

# 删除任务
schtasks /delete /tn "Apboa System" /f
```

## 访问地址

- **前端**：http://localhost:3000
- **后端API**：http://localhost:3060

## 默认登录

- 账号：`admin`
- 密码：`Admin@123.com`
