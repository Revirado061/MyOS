# 文件系统API文档

## 1. 目录操作

### 1.1 获取当前目录路径
- **请求方式**：GET
- **URL**：`/filesystem/current-path`
- **响应示例**：
```json
{
    "data": "/docs/projects"
}
```

### 1.2 获取完整目录树
- **请求方式**：GET
- **URL**：`/filesystem/tree`
- **响应示例**：
```json
{
    "success": true,
    "tree": {
        "path": "/",
        "children": [
            {
                "path": "/0613",
                "children": [
                    {
                        "path": "/0613/1444",
                        "children": [
                            {
                                "path": "/0613/1444/test",
                                "children": [
                                    {
                                        "path": "/0613/1444/test/test123.txt",
                                        "isOpen": true,
                                        "size": 13,
                                        "name": "test123.txt",
                                        "isAllocated": true,
                                        "type": "file"
                                    },
                                    {
                                        "path": "/0613/1444/test/testABC.txt",
                                        "isOpen": true,
                                        "size": 31,
                                        "name": "testABC.txt",
                                        "isAllocated": true,
                                        "type": "file"
                                    }
                                ],
                                "name": "test",
                                "type": "directory"
                            }
                        ],
                        "name": "1444",
                        "type": "directory"
                    }
                ],
                "name": "0613",
                "type": "directory"
            }
        ],
        "name": "/",
        "type": "directory"
    }
}
```

### 1.3 创建目录
- **请求方式**：POST
- **URL**：`/filesystem/directory`
- **参数**：
  - `name`: 目录名称（必填）
- **响应示例**：
```json
{
    "success": true,
    "message": "目录创建成功"
}
```

### 1.4 切换目录
- **请求方式**：POST
- **URL**：`/filesystem/change-directory`
- **参数**：
  - `path`: 目标目录路径（必填）
    - 使用 "/" 返回根目录
    - 使用 ".." 返回上级目录
    - 使用目录名称进入子目录
- **响应示例**：
```json
{
    "success": true,
    "message": "目录切换成功"
}
```

### 1.5 列出目录内容
- **请求方式**：GET
- **URL**：`/filesystem/list`
- **响应示例**：
```json
{
    "success": true,
    "contents": ["file1.txt", "file2.txt", "docs", "projects"]
}
```

### 1.6 删除目录
- **请求方式**：DELETE
- **URL**：`/filesystem/directory`
- **参数**：
  - `name`: 目录名称（必填）
- **响应示例**：
```json
{
    "success": true,
    "message": "目录删除成功"
}
```

## 2. 文件操作

### 2.1 创建文件
- **请求方式**：POST
- **URL**：`/filesystem/file`
- **参数**：
  - `name`: 文件名称（必填）
- **响应示例**：
```json
{
    "success": true,
    "message": "文件创建成功"
}
```

### 2.2 打开文件
- **请求方式**：POST
- **URL**：`/filesystem/file/open`
- **参数**：
  - `name`: 文件名称（必填）
- **响应示例**：
```json
{
    "success": true,
    "message": "文件打开成功"
}
```

### 2.3 关闭文件
- **请求方式**：POST
- **URL**：`/filesystem/file/close`
- **参数**：
  - `name`: 文件名称（必填）
- **响应示例**：
```json
{
    "success": true,
    "message": "文件关闭成功"
}
```

### 2.4 读取文件内容
- **请求方式**：GET
- **URL**：`/filesystem/file/content`
- **参数**：
  - `name`: 文件名称（必填）
- **响应示例**：
```json
{
    "success": true,
    "content": "Hello, World!"
}
```

### 2.5 写入文件内容
- **请求方式**：POST
- **URL**：`/filesystem/file/content`
- **参数**：
  - `name`: 文件名称（必填，Query参数）
  - `content`: 文件内容（必填，Body参数）
- **请求头**：
  - `Content-Type: text/plain`
- **响应示例**：
```json
{
    "success": true,
    "message": "文件写入成功"
}
```

### 2.6 删除文件
- **请求方式**：DELETE
- **URL**：`/filesystem/file`
- **参数**：
  - `name`: 文件名称（必填）
- **响应示例**：
```json
{
    "success": true,
    "message": "文件删除成功"
}
```

## 3. 错误响应

所有接口在发生错误时都会返回统一的错误格式：

```json
{
    "success": false,
    "message": "错误信息描述"
}
```

常见错误信息：
- "文件不存在或未打开"
- "文件已存在或创建失败"
- "目录不存在或切换失败"
- "目录已存在或创建失败"
- "文件内容不能为空"

## 4. 使用注意事项

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

## 5. 磁盘管理

### 5.1 获取磁盘使用状态
- **请求方式**：GET
- **URL**：`/filesystem/disk-status`
- **响应示例**：
```json
{
    "success": true,
    "status": {
        "totalSize": 8589934592,        // 总大小（8GB）
        "blockSize": 32,                // 块大小（32B）
        "totalBlocks": 268435456,       // 总块数
        "usedBlocks": 1024,             // 已使用块数
        "freeBlocks": 268434432,        // 空闲块数
        "usagePercentage": 0.000381     // 使用百分比
    }
}
```

### 5.2 磁盘空间说明
- 总磁盘大小：8GB
- 块大小：32B
- 总块数：268,435,456个
- 文件存储采用连续分配方式
- 支持动态分配和回收
- 使用位图管理空闲块 