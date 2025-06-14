# MyOS 进程管理 API 文档


## 基本信息
- 基础URL: `http://localhost:8080`
- 所有请求和响应均使用 JSON 格式
- 认证方式：待定

## 通用响应格式

### 成功响应
```json
{
    "success": true,
    "message": "操作成功",
    "data": {
        // 具体响应数据
    }
}
```

### 错误响应
```json
{
    "success": false,
    "message": "错误描述"
}
```

## API 端点

### 1. 进程管理

#### 1.1 创建进程
- **URL**: `/process`
- **方法**: `POST`
- **描述**: 创建新的进程
- **请求体**:
```json
{
    "name": "进程名称",
    "priority": 1,        // 优先级（1-3）
    "memorySize": 1024    // 内存大小（MB）
}
```
- **响应**: 返回创建的进程对象

#### 1.2 创建并启动进程
- **URL**: `/process/create-and-start`
- **方法**: `POST`
- **描述**: 创建新进程并立即设置为就绪状态
- **请求体**: 同创建进程
- **响应**: 返回创建的进程对象

#### 1.3 获取进程列表
- **URL**: `/process`
- **方法**: `GET`
- **描述**: 获取所有进程列表
- **响应**: 返回进程列表数组

#### 1.4 获取特定状态的进程
- **URL**: `/process/{state}`
- **方法**: `GET`
- **描述**: 获取特定状态的进程列表
- **路径参数**:
  - `state`: 进程状态（NEW/READY/RUNNING/WAITING/TERMINATED）
- **响应**: 返回进程列表数组

#### 1.5 获取当前运行进程
- **URL**: `/process/current`
- **方法**: `GET`
- **描述**: 获取当前正在运行的进程
- **响应**: 返回当前进程对象

#### 1.6 删除进程
- **URL**: `/process/{id}`
- **方法**: `DELETE`
- **描述**: 删除指定进程
- **响应**:
```json
{
    "success": true,
    "message": "进程已删除"
}
```

#### 1.7 更新进程状态
- **URL**: `/process/{id}/state`
- **方法**: `PUT`
- **描述**: 更新进程状态
- **请求参数**:
  - `state`: 新状态（NEW/READY/RUNNING/WAITING/TERMINATED）
- **响应**:
```json
{
    "success": true,
    "message": "进程状态已更新"
}
```

#### 1.8 进程调度
- **URL**: `/process/schedule`
- **方法**: `POST`
- **描述**: 手动触发进程调度
- **响应**: 返回调度后的当前进程对象

#### 1.9 进程阻塞
- **URL**: `/process/{id}/block`
- **方法**: `POST`
- **描述**: 阻塞指定进程
- **响应**:
```json
{
    "success": true,
    "message": "进程已阻塞"
}
```

#### 1.10 进程唤醒
- **URL**: `/process/{id}/wakeup`
- **方法**: `POST`
- **描述**: 唤醒指定进程
- **响应**:
```json
{
    "success": true,
    "message": "进程已唤醒"
}
```

#### 1.11 进程启动
- **URL**: `/process/{id}/start`
- **方法**: `POST`
- **描述**: 启动指定进程（仅NEW状态可用）
- **响应**:
```json
{
    "success": true,
    "message": "进程已启动"
}
```

#### 1.12 进程终止
- **URL**: `/process/{id}/terminate`
- **方法**: `POST`
- **描述**: 终止指定进程
- **响应**:
```json
{
    "success": true,
    "message": "进程已终止"
}
```

#### 1.13 更新进程优先级
- **URL**: `/process/{id}/priority`
- **方法**: `PUT`
- **描述**: 更新进程优先级
- **请求参数**:
  - `priority`: 新的优先级值
- **响应**:
```json
{
    "success": true,
    "message": "进程优先级已更新"
}
```

#### 1.14 获取进程信息
- **URL**: `/process/{id}/info`
- **方法**: `GET`
- **描述**: 获取指定进程的详细信息
- **响应**: 返回进程详细信息对象

#### 1.15 获取进程统计信息
- **URL**: `/process/stats`
- **方法**: `GET`
- **描述**: 获取进程统计信息
- **响应**: 返回进程统计信息对象

#### 1.16 进程交换
- **URL**: `/process/{id}/swapin` 或 `/process/{id}/swapout`
- **方法**: `POST`
- **描述**: 将进程调入/调出内存
- **响应**:
```json
{
    "success": true,
    "message": "进程已调入/调出内存"
}
```

#### 1.17 批量更新优先级
- **URL**: `/process/batch-update-priorities`
- **方法**: `PUT`
- **描述**: 批量更新多个进程的优先级
- **请求体**:
```json
{
    "1": 1,  // 进程ID: 优先级
    "2": 2
}
```
- **响应**:
```json
{
    "success": true,
    "results": {
        "1": "优先级已更新为 1",
        "2": "优先级已更新为 2"
    }
}
```

#### 1.18 获取进程状态转换历史
- **URL**: `/process/{id}/transitions`
- **方法**: `GET`
- **描述**: 获取指定进程的状态转换历史
- **响应**: 返回状态转换历史数组


#### 1.19 进程请求设备
- **URL**: `/process/{id}/request-device`
- **方法**: `POST`
- **描述**: 运行中的进程请求使用设备，会将进程状态从RUNNING转为WAITING
- **请求参数**:
  - `deviceType`: 设备类型（必填）
  - `deviceId`: 设备ID（可选）
- **响应**:
```json
{
    "success": true,
    "message": "进程已请求设备并转入等待状态",
    "data": {
        "processId": 1,
        "processName": "进程名称",
        "deviceType": "设备类型",
        "deviceId": 1,
        "requestTime": "2023-06-01T12:00:00",
        "newState": "WAITING"
    }
}
```

#### 1.20 设备操作完成
- **URL**: `/process/{id}/device-complete`
- **方法**: `POST`
- **描述**: 设备操作完成，唤醒等待中的进程，将状态从WAITING转为READY
- **响应**:
```json
{
    "success": true,
    "message": "设备操作完成，进程已唤醒",
    "data": {
        "processId": 1,
        "processName": "进程名称",
        "newState": "READY",
        "wakeupTime": "2023-06-01T12:05:00"
    }
}
```

## 进程状态说明

| 状态 | 说明 |
|------|------|
| NEW | 新建 |
| READY | 就绪 |
| RUNNING | 运行 |
| WAITING | 等待/阻塞 |
| TERMINATED | 终止 |

## 注意事项
1. 所有时间戳使用ISO 8601格式
2. 内存大小单位为KB
3. 优先级范围为1-3，数字越小优先级越高
4. 进程状态转换需要遵循状态机规则
5. 删除进程前需要确保进程已终止

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| PROCESS_NOT_FOUND | 进程不存在 |
| INVALID_STATE_TRANSITION | 无效的状态转换 |
| INSUFFICIENT_MEMORY | 内存不足 |
| PROCESS_ALREADY_EXISTS | 进程已存在 |
| INVALID_PRIORITY | 无效的优先级值 |

## 前端组件使用示例

```javascript
// 创建进程
async createProcess(processData) {
  try {
    const response = await createProcess(processData);
    this.$message.success('进程创建成功');
    await this.fetchProcesses();
  } catch (error) {
    this.$message.error('进程创建失败');
  }
}

// 获取进程列表
async fetchProcesses() {
  try {
    const response = await getAllProcesses();
    this.processes = response.data;
  } catch (error) {
    this.$message.error('获取进程列表失败');
  }
}

// 终止进程
async handleTerminate(process) {
  try {
    await terminateProcess(process.id);
    this.$message.success(`进程 ${process.name} 已终止`);
    await this.fetchProcesses();
  } catch (error) {
    this.$message.error('终止进程失败');
  }
}
```

## 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如内存不足） |
| 500 | 服务器内部错误 | 