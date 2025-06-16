# 文件系统API文档

## 基础信息
- 基础URL: `http://localhost:8080/filesystem`
- 磁盘大小: 8GB
- 块大小: 32MB

## API列表

### 1. 获取当前目录路径
- **请求方式**: GET
- **URL**: `/current-path`
- **响应示例**:
```json
"/"
```

### 2. 创建文件
- **请求方式**: POST
- **URL**: `/file`
- **参数**:
  - `name`: 文件名
- **响应示例**:
```json
{
    "data": "/docs/projects"
}
```




### 3. 创建目录
- **请求方式**: POST
- **URL**: `/directory`
- **参数**:
  - `name`: 目录名
- **响应示例**:

```json
{
    "success": true,
    "message": "目录创建成功"
}
```


### 4. 删除文件
- **请求方式**: DELETE
- **URL**: `/file`
- **参数**:
  - `name`: 文件名
- **响应示例**:
```json
{
    "success": true,
    "message": "文件删除成功"
}
```

### 5. 删除目录
- **请求方式**: DELETE
- **URL**: `/directory`
- **参数**:
  - `name`: 目录名
- **响应示例**:
```json
{
    "success": true,
    "message": "目录删除成功"
}
```

### 6. 切换目录
- **请求方式**: POST
- **URL**: `/change-directory`
- **参数**:
  - `path`: 目标目录路径
- **响应示例**:

```json
{
    "success": true,
    "message": "目录切换成功"
}
```


### 7. 列出当前目录内容
- **请求方式**: GET
- **URL**: `/list`
- **响应示例**:

```json
{
    "success": true,
    "contents": ["file1.txt", "dir1"]
}
```


### 8. 获取目录详细内容
- **请求方式**: GET
- **URL**: `/directory-content`
- **响应示例**:
```json
{
    "success": true,
    "content": {
        "files": [
            {
                "name": "file1.txt",
                "size": 1024,
                "path": "/file1.txt",
                "isOpen": false,
                "isAllocated": true
            }
        ],
        "directories": ["dir1"]
    }
}
```


### 9. 打开文件
- **请求方式**: POST
- **URL**: `/file/open`
- **参数**:
  - `name`: 文件名
- **响应示例**:
```json
{
    "success": true,
    "message": "文件打开成功"
}
```

### 10. 关闭文件
- **请求方式**: POST
- **URL**: `/file/close`
- **参数**:
  - `name`: 文件名
- **响应示例**:
```json
{
    "success": true,
    "message": "文件关闭成功"
}
```

### 11. 读取文件内容
- **请求方式**: GET
- **URL**: `/file/content`
- **参数**:
  - `name`: 文件名
  - `startBlock`: (可选) 起始块号，从0开始
  - `numBlocks`: (可选) 要读取的块数
- **响应示例**:
```json
// 完整读取
{
    "success": true,
    "content": "文件内容"
}

// 分块读取
{
    "success": true,
    "content": "块内容",
    "startBlock": 0,
    "numBlocks": 1,
    "totalBlocks": 5
}
```

### 12. 写入文件内容
- **请求方式**: POST
- **URL**: `/file/content`
- **参数**:
  - `name`: 文件名
  - 请求体: 文件内容
- **响应示例**:
```json
{
    "success": true,
    "message": "文件写入成功"
}
```

### 13. 获取完整目录树
- **请求方式**: GET
- **URL**: `/tree`
- **响应示例**:
```json
{
    "success": true,
    "tree": {
        "name": "/",
        "type": "directory",
        "path": "/",
        "children": [
            {
                "name": "file1.txt",
                "type": "file",
                "path": "/file1.txt",
                "size": 1024,
                "isOpen": false,
                "isAllocated": true
            },
            {
                "name": "dir1",
                "type": "directory",
                "path": "/dir1",
                "children": []
            }
        ]
    }
}
```

### 14. 获取磁盘使用状态
- **请求方式**: GET
- **URL**: `/disk-status`
- **响应示例**:
```json
{
    "success": true,
    "status": {
        "totalSize": 8589934592,
        "blockSize": 33554432,
        "totalBlocks": 256,
        "usedBlocks": 10,
        "freeBlocks": 246,
        "usagePercentage": 3.90625
    }
}
```

## 注意事项
1. 所有文件操作前需要先打开文件
2. 文件内容读取支持分块读取，每个块大小为32MB
3. 分块读取时建议每次读取1-2个块，避免响应过大
4. 如果`startBlock + numBlocks`超过文件总块数，会自动调整到文件末尾
5. 文件路径使用正斜杠(/)作为分隔符
6. 根目录使用"/"表示



常见错误信息：
- "文件不存在或未打开"
- "文件已存在或创建失败"
- "目录不存在或切换失败"
- "目录已存在或创建失败"
- "文件内容不能为空"



1. 文件操作流程：
   - 创建文件
   - 打开文件
   - 读写文件内容
   - 关闭文件

2. 目录操作注意事项：
   - 目录删除会递归删除其中的所有文件和子目录
   - 使用相对路径时注意当前目录位置
   - 目录名称不能包含特殊字符

3. 文件操作注意事项：
   - 文件操作前必须先打开文件
   - 文件操作完成后应该关闭文件
   - 写入文件内容时需要设置正确的Content-Type
   - 所有请求都会返回布尔值（true/false）表示操作是否成功

4. 路径说明：
   - 根目录："/"
   - 上级目录：".."
   - 当前目录："."
   - 子目录：直接使用目录名 



5. 磁盘空间说明
   - 总磁盘大小：8GB
   - 块大小：32MB
   - 总块数：256个
   - 文件存储采用连续分配方式
   - 支持动态分配和回收
   - 使用位图管理空闲块 