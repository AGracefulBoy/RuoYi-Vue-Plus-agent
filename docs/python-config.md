# Python包管理配置说明

## 配置项说明

系统提供了Python包管理的配置功能，支持本地和远程两种执行模式。

### 执行模式

| 模式 | 说明 |
|------|------|
| `local` | 本地模式，在应用服务器本地执行Python命令 |
| `remote` | 远程模式，通过SSH连接到远程服务器执行Python命令 |

### 配置项

#### 通用配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `python.execution-mode` | 执行模式：local或remote | `local` |

#### 本地模式配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `python.local.virtualenv-path` | 本地虚拟环境目录路径 | `./py-runtime` |
| `python.local.activate-script` | 本地激活脚本路径 | `./myenv/bin/activate` |

#### 远程模式配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `python.remote.host` | 远程服务器地址 | `localhost` |
| `python.remote.port` | SSH端口 | `22` |
| `python.remote.username` | SSH用户名 | `root` |
| `python.remote.password` | SSH密码（可选） | 无 |
| `python.remote.private-key-path` | SSH私钥路径（可选） | 无 |
| `python.remote.private-key-passphrase` | SSH私钥密码（可选） | 无 |
| `python.remote.connection-timeout` | 连接超时时间（秒） | `30` |
| `python.remote.virtualenv-path` | 远程虚拟环境目录路径 | `/opt/py-runtime` |
| `python.remote.activate-script` | 远程激活脚本路径 | `/opt/myenv/bin/activate` |

### 配置文件位置

- **开发环境**: `ruoyi-admin/src/main/resources/application-dev.yml`
- **生产环境**: `ruoyi-admin/src/main/resources/application-prod.yml`
- **通用配置**: `ruoyi-admin/src/main/resources/application.yml`

### 配置示例

#### 本地模式配置

```yaml
# Python包管理配置
python:
  # 执行模式：local-本地执行, remote-远程执行
  execution-mode: local
  # 本地模式配置
  local:
    # 虚拟环境路径
    virtualenv-path: ./py-runtime
    # 激活脚本路径
    activate-script: ./myenv/bin/activate
```

#### 远程模式配置（密码认证）

```yaml
# Python包管理配置
python:
  # 执行模式：local-本地执行, remote-远程执行
  execution-mode: remote
  # 远程模式配置
  remote:
    # 远程服务器地址
    host: 192.168.1.100
    # SSH端口
    port: 22
    # 用户名
    username: python-user
    # 密码
    password: your-password
    # 连接超时时间（秒）
    connection-timeout: 30
    # 远程虚拟环境路径
    virtualenv-path: /opt/py-runtime
    # 远程激活脚本路径
    activate-script: /opt/myenv/bin/activate
```

#### 远程模式配置（密钥认证）

```yaml
# Python包管理配置
python:
  # 执行模式：local-本地执行, remote-远程执行
  execution-mode: remote
  # 远程模式配置
  remote:
    # 远程服务器地址
    host: 192.168.1.100
    # SSH端口
    port: 22
    # 用户名
    username: python-user
    # 私钥路径
    private-key-path: ~/.ssh/id_rsa
    # 私钥密码（如果私钥有密码）
    private-key-passphrase: your-key-passphrase
    # 连接超时时间（秒）
    connection-timeout: 30
    # 远程虚拟环境路径
    virtualenv-path: /opt/py-runtime
    # 远程激活脚本路径
    activate-script: /opt/myenv/bin/activate
```

### 环境差异

- **开发环境**: 连接到开发服务器 `192.168.1.100`
- **生产环境**: 连接到生产服务器 `192.168.1.200`

### 使用说明

1. 根据您的实际环境选择执行模式
2. 如果选择远程模式，配置SSH连接信息
3. 确保远程服务器的Python虚拟环境已正确安装
4. 使用测试接口验证连接是否正常
5. 重启应用使配置生效

### SSH连接设置

#### 密钥认证（推荐）

1. 生成SSH密钥对：
   ```bash
   ssh-keygen -t rsa -b 4096 -C "your-email@example.com"
   ```

2. 将公钥复制到远程服务器：
   ```bash
   ssh-copy-id python-user@192.168.1.100
   ```

3. 在配置文件中使用私钥路径

#### 密码认证

直接在配置文件中设置密码，但不推荐在生产环境中使用。

### 测试连接

使用以下API测试连接：

```http
GET /system/pythonPackage/testConnection
```

### API接口说明

#### 包管理接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/system/pythonPackage/install` | POST | 手动安装包 |
| `/system/pythonPackage/uninstall` | POST | 根据包名卸载包 |
| `/system/pythonPackage/uninstall/{packageId}` | POST | 根据包ID卸载包（推荐） |
| `/system/pythonPackage/uninstall/batch` | POST | 批量根据包ID卸载包 |
| `/system/pythonPackage/installed` | GET | 获取已安装包列表 |
| `/system/pythonPackage/syncStatus` | POST | 同步包安装状态 |
| `/system/pythonPackage/testConnection` | GET | 测试连接 |


### 虚拟环境激活

#####进入项目根目录
cd /xxx/xxx

####创建 py-runtime 目录
mkdir py-runtime
cd py-runtime

#### 创建 Python 虚拟环境
python3 -m venv myenv

####激活虚拟环境
source myenv/bin/activate


#### 版本管理功能

**精确版本卸载**：
- 系统会在卸载前检查包的实际安装版本
- 与数据库中记录的版本进行对比
- 如果版本不匹配会发出警告但仍继续卸载
- 卸载后自动更新数据库状态

**状态同步**：
- 自动检测实际安装状态与数据库记录的差异
- 一键同步所有包的安装状态
- 识别版本不一致的情况

**使用示例**：

```bash
# 根据包ID卸载（推荐方式）
POST /system/pythonPackage/uninstall/123

# 批量卸载
POST /system/pythonPackage/uninstall/batch
Body: [123, 124, 125]

# 获取已安装包列表
GET /system/pythonPackage/installed

# 同步状态
POST /system/pythonPackage/syncStatus
```

### 注意事项

- **安全性**: 生产环境建议使用密钥认证而非密码认证
- **网络连接**: 确保应用服务器能够访问远程Python服务器
- **权限**: 确保SSH用户有权限访问虚拟环境目录
- **防火墙**: 确保SSH端口（默认22）未被防火墙阻止
- **虚拟环境**: 确保远程服务器的Python虚拟环境已正确安装和配置
- **版本管理**: 建议定期使用同步功能确保数据库状态与实际环境一致
- **卸载策略**: 推荐使用包ID卸载而非包名卸载，以确保版本一致性

