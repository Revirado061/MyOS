# 操作系统模拟器 API 文档

## 目录
- [进程管理](#进程管理)
- [内存管理](#内存管理)
- [文件系统](#文件系统)
- [设备管理](#设备管理)
- [中断处理](#中断处理)

## 进程管理

### 创建进程
- **请求方法**: POST
- **请求URL**: `/process`
- **请求参数**:
  ```json
  {
    "name": "进程名称",
    "priority": 1,  // 优先级（1-3）
    "memorySize": 256  // 内存大小（MB）
  }
  ```
- **返回数据**:
  ```json
  {
    "id": 1,
    "name": "进程名称",
    "priority": 1,
    "state": "NEW",
    "memorySize": 256,
    "createTime": "2024-03-21T10:00:00",
    "lastUpdateTime": "2024-03-21T10:00:00"
  }
  ```
- **错误码**:
  - 400: 请求参数错误
  - 500: 服务器内部错误

### 获取所有进程
- **请求方法**: GET
- **请求URL**: `/process`
- **返回数据**:
  ```json
  [
    {
      "id": 1,
      "name": "进程1",
      "priority": 1,
      "state": "RUNNING",
      "memorySize": 256,
      "createTime": "2024-03-21T10:00:00",
      "lastUpdateTime": "2024-03-21T10:00:00"
    }
  ]
  ```

### 获取当前运行进程
- **请求方法**: GET
- **请求URL**: `/process/current`
- **返回数据**:
  ```json
  {
    "id": 1,
    "name": "当前进程",
    "priority": 1,
    "state": "RUNNING",
    "memorySize": 256,
    "createTime": "2024-03-21T10:00:00",
    "lastUpdateTime": "2024-03-21T10:00:00"
  }
  ```

## 内存管理

### 获取空闲内存大小
- **请求方法**: GET
- **请求URL**: `/memory/free-size`
- **返回数据**:
  ```json
  1024   // 当前空闲内存大小
  ```

### 获取内存状态
- **请求方法**: GET
- **请求URL**: `/memory/status`
- **返回数据**:
  ```json
  [1, 0, 2, 0, 3]  // int[256] 数组表示每个内存块的状态，非0值表示被进程占用（非0值为对应进程的id），0表示空闲
  ```

### 获取空闲内存块列表
- **请求方法**: GET
- **请求URL**: `/memory/free-blocks`
- **返回数据**:
  ```json
  [
    {
      "start": 0,    // 起始地址
      "size": 256    // 块大小（MB）
    }
  ]
  ```

### 获取内存使用率
- **请求方法**: GET
- **请求URL**: `/memory/usage`
- **返回数据**:
  ```json
  0.75  // 返回内存使用率（0-1之间的浮点数）
  ```

## 文件系统

### 创建文件
- **请求方法**: POST
- **请求URL**: `/file`
- **请求参数**:
  ```json
  {
    "name": "文件名",
    "type": "text",  // text 或 binary
    "size": 1024  // 文件大小（KB）
  }
  ```
- **返回数据**:
  ```json
  {
    "success": true,
    "message": "文件创建成功",
    "data": {
      "name": "文件名",
      "type": "text",
      "size": 1024,
      "path": "/当前目录/文件名"
    }
  }
  ```

### 删除文件
- **请求方法**: DELETE
- **请求URL**: `/file/{name}`
- **返回数据**:
  ```json
  {
    "success": true,
    "message": "文件删除成功"
  }
  ```

### 读取文件内容
- **请求方法**: GET
- **请求URL**: `/file/{name}/content`
- **返回数据**:
  ```json
  {
    "content": "文件内容",
    "size": 1024
  }
  ```

### 写入文件内容
- **请求方法**: POST
- **请求URL**: `/file/{name}/content`
- **请求参数**:
  ```json
  {
    "content": "要写入的内容"
  }
  ```
- **返回数据**:
  ```json
  {
    "success": true,
    "message": "文件写入成功"
  }
  ```

### 创建目录
- **请求方法**: POST
- **请求URL**: `/file/directory`
- **请求参数**:
  ```json
  {
    "name": "目录名"
  }
  ```
- **返回数据**:
  ```json
  {
    "success": true,
    "message": "目录创建成功"
  }
  ```

### 切换目录
- **请求方法**: POST
- **请求URL**: `/file/change-directory`
- **请求参数**:
  ```json
  {
    "path": "目标目录路径"
  }
  ```
- **返回数据**:
  ```json
  {
    "success": true,
    "message": "目录切换成功",
    "currentPath": "/新目录路径"
  }
  ```

### 删除目录
- **请求方法**: DELETE
- **请求URL**: `/file/directory/{name}`
- **返回数据**:
  ```json
  {
    "success": true,
    "message": "目录删除成功"
  }
  ```

## 设备管理

### 获取可用设备
- **请求方法**: GET
- **请求URL**: `/device/available`
- **返回数据**:
  ```json
  [
    {
      "deviceCode": "A1",
      "type": "PRINTER",
      "status": "IDLE",
      "version": 1
    }
  ]
  ```

### 获取设备状态
- **请求方法**: GET
- **请求URL**: `/device/status`
- **请求参数**:
  - Query: `deviceCode=A1`
- **返回数据**:
  ```json
  {
    "deviceCode": "A1",
    "status": "BUSY",
    "occupiedByProcess": 1,
    "allocatedTime": "2024-03-21T10:00:00"
  }
  ```

### 分配设备
- **请求方法**: POST
- **请求URL**: `/device/allocate`
- **请求参数**:
  ```json
  {
    "deviceCode": "A1",
    "processId": 1
  }
  ```
- **返回数据**:
  ```json
  {
    "success": true,
    "message": "设备分配成功"
  }
  ```

### 释放设备
- **请求方法**: POST
- **请求URL**: `/device/release`
- **请求参数**:
  ```json
  {
    "deviceCode": "A1",
    "processId": 1
  }
  ```
- **返回数据**:
  ```json
  {
    "success": true,
    "message": "设备释放成功"
  }
  ```

## 中断处理

### 获取中断状态
- **请求方法**: GET
- **请求URL**: `/interrupt/status`
- **返回数据**:
  ```json
  {
    "type": "TIMER",
    "status": "active",
    "enabled": true
  }
  ```

### 触发中断
- **请求方法**: POST
- **请求URL**: `/interrupt/trigger`
- **请求参数**:
  ```json
  {
    "vector": 1,
    "type": "IO",
    "processId": 1,
    "data": "A1"
  }
  ```
- **返回数据**:
  ```json
  {
    "success": true,
    "message": "中断触发成功"
  }
  ```

## 通用错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 注意事项

1. 所有请求和响应均使用 JSON 格式
2. 时间格式采用 ISO 8601 标准
3. 内存大小单位为 MB
4. 文件大小单位为 KB
5. 所有接口都需要进行错误处理
6. 部分接口可能需要权限验证 