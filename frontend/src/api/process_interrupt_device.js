import request from './config'

// 进程相关 API
export const processApi = {
    // 创建进程
    createProcess: (processData) => {
        return request.post('/process', processData);
    },

    // 获取所有进程
    getAllProcesses: () => {
        return request.get('/process');
    },

    // 获取特定状态的进程
    getProcessesByState: (state) => {
        return request.get(`/process/${state}`);
    },

    // 获取当前运行进程
    getCurrentProcess: () => {
        return request.get('/process/current');
    },

    // 删除进程
    deleteProcess: (id) => {
        return request.delete(`/process/${id}`);
    },

    // 更新进程状态
    updateProcessState: (id, state) => {
        return request.put(`/process/${id}/state`, { state });
    },

    // 终止进程
    terminateProcess: (id) => {
        return request.post(`/process/${id}/terminate`);
    },

    // 更新进程优先级
    updateProcessPriority: (id, priority) => {
        return request.put(`/process/${id}/priority`, { priority });
    },

    // 获取进程详细信息
    getProcessInfo: (id) => {
        return request.get(`/process/${id}/info`);
    },

    // 获取进程统计信息
    getProcessStats: () => {
        return request.get('/process/stats');
    },

    // 设置调度算法
    setSchedulingAlgorithm: (algorithm) => {
        return request.put(`/process/scheduling-algorithm?algorithm=${algorithm}`);
    },

    // 获取当前调度算法
    getSchedulingAlgorithm: () => {
        return request.get('/process/scheduling-algorithm');
    },

    // 批量更新进程优先级
    batchUpdatePriorities: (priorities) => {
        return request.put('/process/batch-update-priorities', priorities);
    }
};

// 中断相关 API
export const interruptApi = {
    // 触发进程中断
    triggerInterrupt: (id, reason) => {
        return request.post(`/process/${id}/interrupt?reason=${reason}`);
    }
};

// 设备相关 API
export const deviceApi = {
    // 请求设备
    requestDevice: (id, deviceType) => {
        return request.post(`/process/${id}/request-device`, { deviceType });
    },

    // 释放设备
    releaseDevice: (id, deviceType) => {
        return request.post(`/process/${id}/release-device`, { deviceType });
    }
};
