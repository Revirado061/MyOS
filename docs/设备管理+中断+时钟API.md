# 设备管理

### 1、获取可用设备

- **请求方法**: GET

- **请求URL**: `/device/available`

- **返回数据**:

  ```json
  {
      "data": [
          {
              "id": 1,
              "name": "打印机1",
              "type": "PRINTER",
              "status": "IDLE",
              "currentProcessId": null,
              "remainingTime": 0,
              "waitQueue": [],
              "version": 0
          },
          {
              "id": 4,
              "name": "磁盘2",
              "type": "DISK",
              "status": "IDLE",
              "currentProcessId": null,
              "remainingTime": 0,
              "waitQueue": [],
              "version": 0
          }
      ],
      "status": "success",
      "message": "可用设备列表已获取"
  }
  ```

### 2、获取全部设备

- **请求方法**: GET
- **请求URL**: `/device`
- **返回数据**:

	```{
	    "data": [
	        {
	            "id": 1,
	            "name": "打印机1",
	            "type": "PRINTER",
	            "status": "IDLE",
	            "currentProcessId": null,
	            "remainingTime": 0,
	            "waitQueue": [],
	            "version": 0
	        },
	        {
	            "id": 2,
	            "name": "打印机2",
	            "type": "PRINTER",
	            "status": "IDLE",
	            "currentProcessId": null,
	            "remainingTime": 0,
	            "waitQueue": [],
	            "version": 0
	        }
	    ],
	    "status": "success",
	    "message": "设备列表已获取"
	}
	```
	
	

### 3、获取设备状态

- **请求方法**: GET

- **请求URL**: `/device/status`

- **请求参数**:

  - Query: `deviceId=1`

- **请求示例：**

  ```
  http://localhost:8080/device/status?deviceId=1
  ```

  

- **返回数据**:

  ```json
  {
      "message": "设备状态已获取",
      "data": {
          "currentProcessId": null,
          "waitQueueSize": 0,
          "name": "打印机1",
          "type": "PRINTER",
          "deviceId": 1,
          "status": "IDLE",    //返回状态结果
          "remainingTime": 0
      },
      "status": "success"
  }
  ```

### 4、分配设备

- **请求方法**: POST

- **请求URL**: `/device/allocate`

- **请求参数**:

  ```json
  {
    "deviceId": 1,
    "processId": 3,
    "timeout": 5  //设备分配给该进程的时间，超时自动释放
  }
  ```

- **返回数据**:

  ```json
  {
      "data": {
          "id": 3,
          "name": "磁盘1",
          "type": "DISK",
          "status": "BUSY",
          "currentProcessId": 1,
          "remainingTime": 5,
          "waitQueue": [],
          "version": 9
      },
      "status": "success",
      "message": "设备已分配给进程 1"
  }
  ```

### 5、释放设备

- **请求方法**: POST

- **请求URL**: `/device/release`

- **请求参数**:

  ```json
  {
    "deviceId": 3,
    "processId": 1
  }
  ```

- **返回数据**:

  ```json
  {
      "data": {
          "id": 3,
          "name": "磁盘1",
          "type": "DISK",
          "status": "IDLE",
          "currentProcessId": null,
          "remainingTime": 0,
          "waitQueue": [],
          "version": 8
      },
      "status": "success",
      "message": "设备已释放，无等待进程"
  }
  ```

### 6、按类型获取设备列表

#### 接口说明

获取指定类型的所有设备列表，支持按设备类型（如打印机、磁盘等）进行筛选。

#### 请求信息

- **请求方法**: GET
- **请求URL**: `/device/type/{type}`
- **请求参数**:
  - Path Variable: `type` - 设备类型
    - 可选值：
      - `PRINTER` - 打印机
      - `DISK` - 磁盘
      - `SCANNER` - 扫描仪
      - `NETWORK` - 网络设备
      - `OTHER` - 其他设备

#### 响应信息

- **响应格式**: JSON

- **响应字段**:

  ```json
  {
    "status": "success",      // 响应状态
    "message": "设备列表已获取",  // 响应消息
    "data": [                 // 设备列表数据
      {
        "id": 1,             // 设备ID
        "name": "打印机1",      // 设备名称
        "type": "PRINTER",   // 设备类型
        "status": "IDLE",    // 设备状态
        "currentProcessId": null,  // 当前占用进程ID
        "remainingTime": 0,       // 剩余使用时间
        "waitQueueSize": 0        // 等待队列大小
      }
      // ... 更多设备
    ]
  }
  ```

#### 示例

1. 获取所有打印机设备：

```http
GET http://localhost:8080/device/type/PRINTER
```

2. 获取所有磁盘设备：

```http
GET http://localhost:8080/device/type/DISK
```



### 7、获取设备信息接口

#### 接口说明
获取指定设备的详细信息，包括设备ID、名称、类型、状态、当前使用进程、剩余使用时间等信息。

#### 请求信息
- 请求方法：GET
- 请求路径：/device/{deviceId}
- 请求参数：
  | 参数名   | 类型 | 位置 | 必填 | 说明            |
  | -------- | ---- | ---- | ---- | --------------- |
  | deviceId | Long | path | 是   | 设备ID，例如：1 |

#### 请求示例
```http
GET /device/1
```

#### 成功响应示例
```json
{
    "status": "success",
    "message": "设备信息已获取",
    "data": {
        "id": 1,
        "name": "打印机1",
        "type": "PRINTER",
        "status": "BUSY",
        "currentProcessId": 1,
        "remainingTime": 5,
        "waitQueue": []
    }
}
```

---



# 中断管理模块接口文档

## 1. 触发中断
### 接口说明
触发一个指定类型的中断

**中断类型：**

```  
    ERROR(1),    // 错误中断，最高优先级
    DEVICE(2),   // 设备中断
    IO(3),       // I/O中断
    PROCESS(4),  // 进程中断
    CLOCK(5),    // 时钟中断
    OTHER(6);    // 其他中断，最低优先级
```

### 请求信息
- 请求方法：POST
- 请求路径：`/interrupts/trigger`
- 请求参数：
  | 参数名   | 类型   | 位置 | 必填 | 说明                                            |
  | -------- | ------ | ---- | ---- | ----------------------------------------------- |
  | type     | String | body | 是   | 中断类型（ERROR/DEVICE/IO/PROCESS/CLOCK/OTHER） |
  | deviceId | Long   | body | 否   | 设备ID，设备中断时必填                          |
  | message  | String | body | 否   | 中断消息                                        |

### 请求示例
```json
{
    "type": "DEVICE", //
    "deviceId": 1,
    "message": "打印机就绪"
}
```

### 响应信息
```json
{
    "status": "success",
    "message": "中断已触发",
    "data": {
        "id": 1,
        "type": "DEVICE",
        "timestamp": 1234567890,
        "message": "打印机就绪"
    }
}
```

## 2. 获取中断队列
### 接口说明
获取当前等待处理的中断队列

### 请求信息
- 请求方法：GET
- 请求路径：`/interrupts/queue`

### 响应信息
```json
{
    "status": "success",
    "message": "中断队列已获取",
    "data": [
        {
            "id": 1,
            "type": "DEVICE",
            "timestamp": 1234567890,
            "message": "打印机就绪"
        }
    ]
}
```

## 3. 获取中断日志
### 接口说明
获取已处理的中断日志记录

### 请求信息
- 请求方法：GET
- 请求路径：`/interrupts/logs`
- 请求参数：
  | 参数名 | 类型    | 位置  | 必填 | 说明                       |
  | ------ | ------- | ----- | ---- | -------------------------- |
  | type   | String  | query | 否   | 中断类型过滤               |
  | limit  | Integer | query | 否   | 返回的最大日志数量，默认10 |

### 响应信息
```json
{
    "status": "success",
    "message": "中断日志已获取",
    "data": [
        {
            "id": 1,
            "interruptId": 1,
            "type": "DEVICE",
            "timestamp": 1234567890,
            "message": "打印机就绪",
            "result": "设备中断处理完成"
        }
    ]
}
```

---



# 时钟模块接口文档

## 1. 启动系统时钟
### 接口说明
启动系统时钟，开始计时和触发定时事件

### 请求信息
- 请求方法：POST
- 请求路径：`/timer/start`

### 响应信息
```json
{
    "status": "success",
    "message": "系统时钟已启动",
    "data": {
        "currentTime": 0
    }
}
```

## 2. 停止系统时钟
### 接口说明
停止系统时钟，暂停计时和定时事件

### 请求信息
- 请求方法：POST
- 请求路径：`/timer/stop`

### 响应信息
```json
{
    "status": "success",
    "message": "系统时钟已停止",
    "data": {
        "currentTime": 10
    }
}
```

## 3. 获取当前时间
### 接口说明
获取系统时钟的当前时间

### 请求信息
- 请求方法：GET
- 请求路径：`/timer/current`

### 响应信息
```json
{
    "status": "success",
    "message": "当前时间已获取",
    "data": {
        "currentTime": 10
    }
}
```

## 注意事项
1. 中断类型说明：
   - ERROR：错误中断，最高优先级
   - DEVICE：设备中断
   - IO：I/O中断
   - PROCESS：进程中断
   - CLOCK：时钟中断
   - OTHER：其他中断，最低优先级

2. 时钟模块功能：
   - 每秒触发一次设备超时检查
   - 每10秒触发一次时钟中断
   - 时钟启动后自动开始计时
   - 时钟停止后暂停所有定时事件

3. 中断处理：
   - 中断按优先级顺序处理
   - 每个中断在队列中等待3秒后处理
   - 处理结果会记录到日志中
   - 日志最多保存100条记录

