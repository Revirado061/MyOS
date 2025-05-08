# 操作系统模拟程序

这是一个基于 Spring Boot 3 和 Vue 3 的操作系统模拟程序，用于演示操作系统的基本功能。

## 项目结构

- `MYOS-backend`: Spring Boot 后端项目
- `MYOS-frontend`: Vue 3 前端项目

## 开发环境要求

- JDK 17 或更高版本
- Node.js 16 或更高版本
- Maven 3.6 或更高版本
- npm 8 或更高版本

## 启动步骤

### 1. 启动后端服务

```bash
# 进入后端项目目录
cd MYOS-backend

# 使用 Maven 编译项目
mvn clean package

# 运行项目
java -jar target/MYOS-0.0.1-SNAPSHOT.jar
```

后端服务将在 http://localhost:8080 启动

### 2. 启动前端服务

```bash
# 进入前端项目目录
cd MYOS-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务将在 http://localhost:5173 启动

## 功能模块

1. 进程管理
   - 创建进程
   - 查看进程列表
   - 更新进程状态
   - 删除进程

2. 内存管理
   - 内存分配
   - 内存释放
   - 内存使用情况可视化

3. 文件系统
   - 创建文件
   - 查看文件列表
   - 读写文件内容
   - 删除文件

4. 设备管理
   - 查看设备列表
   - 请求设备
   - 释放设备

## 技术栈

### 后端
- Spring Boot 3
- Spring Data JPA
- H2 数据库
- Lombok

### 前端
- Vue 3
- TypeScript
- Element Plus
- Axios
- Vue Router 