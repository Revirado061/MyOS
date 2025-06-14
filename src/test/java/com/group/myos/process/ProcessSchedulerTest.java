package com.group.myos.process;

import com.group.myos.process.model.Process;
import com.group.myos.process.model.ProcessTransition;
import com.group.myos.process.repository.ProcessRepository;
import com.group.myos.process.repository.ProcessTransitionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ProcessSchedulerTest {

    @Autowired
    private ProcessScheduler processScheduler;

    @Autowired
    private ProcessRepository processRepository;

    @Autowired
    private ProcessTransitionRepository processTransitionRepository;

    @Test
    public void testInterruptProcess() {
        // 创建测试进程
        Process process = new Process();
        process.setId(1L);
        process.setName("TestProcess");
        process.setPriority(1);
        process.setState(Process.ProcessState.RUNNING);
        process.setLastUpdateTime(LocalDateTime.now());
        processRepository.save(process);
        
        // 设置当前运行进程
        processScheduler.setCurrentProcess(process);
        
        // 测试IO中断
        processScheduler.interruptProcess(1L, "IO");
        Process updatedProcess = processRepository.findById(1L).orElse(null);
        assertNotNull(updatedProcess);
        assertEquals(Process.ProcessState.WAITING, updatedProcess.getState());
        assertEquals("等待IO完成", updatedProcess.getWaitingReason());
        
        // 重置进程状态
        process.setState(Process.ProcessState.RUNNING);
        process.setLastUpdateTime(LocalDateTime.now());
        processRepository.save(process);
        processScheduler.setCurrentProcess(process);
        
        // 测试DEVICE中断
        processScheduler.interruptProcess(1L, "DEVICE");
        updatedProcess = processRepository.findById(1L).orElse(null);
        assertNotNull(updatedProcess);
        assertEquals(Process.ProcessState.WAITING, updatedProcess.getState());
        assertEquals("等待设备", updatedProcess.getWaitingReason());
        
        // 重置进程状态
        process.setState(Process.ProcessState.RUNNING);
        process.setLastUpdateTime(LocalDateTime.now());
        processRepository.save(process);
        processScheduler.setCurrentProcess(process);
        
        // 测试PROCESS中断
        processScheduler.interruptProcess(1L, "PROCESS");
        updatedProcess = processRepository.findById(1L).orElse(null);
        assertNotNull(updatedProcess);
        assertEquals(Process.ProcessState.READY, updatedProcess.getState());
        
        // 重置进程状态
        process.setState(Process.ProcessState.RUNNING);
        process.setLastUpdateTime(LocalDateTime.now());
        processRepository.save(process);
        processScheduler.setCurrentProcess(process);
        
        // 测试ERROR中断
        processScheduler.interruptProcess(1L, "ERROR");
        updatedProcess = processRepository.findById(1L).orElse(null);
        assertNotNull(updatedProcess);
        assertEquals(Process.ProcessState.TERMINATED, updatedProcess.getState());
        
        // 重置进程状态
        process.setState(Process.ProcessState.RUNNING);
        process.setLastUpdateTime(LocalDateTime.now());
        processRepository.save(process);
        processScheduler.setCurrentProcess(process);
        
        // 测试CLOCK中断
        processScheduler.interruptProcess(1L, "CLOCK");
        updatedProcess = processRepository.findById(1L).orElse(null);
        assertNotNull(updatedProcess);
        assertEquals(Process.ProcessState.RUNNING, updatedProcess.getState()); // 时钟中断不改变状态
        
        // 重置进程状态
        process.setState(Process.ProcessState.RUNNING);
        process.setLastUpdateTime(LocalDateTime.now());
        processRepository.save(process);
        processScheduler.setCurrentProcess(process);
        
        // 测试OTHER中断
        processScheduler.interruptProcess(1L, "OTHER");
        updatedProcess = processRepository.findById(1L).orElse(null);
        assertNotNull(updatedProcess);
        assertEquals(Process.ProcessState.READY, updatedProcess.getState());
    }
    
    @Test
    public void testInterruptNonRunningProcess() {
        // 创建测试进程
        Process process = new Process();
        process.setId(1L);
        process.setName("TestProcess");
        process.setPriority(1);
        process.setState(Process.ProcessState.READY); // 设置为就绪状态
        process.setLastUpdateTime(LocalDateTime.now());
        processRepository.save(process);
        
        // 尝试中断非运行状态的进程
        processScheduler.interruptProcess(1L, "IO");
        Process updatedProcess = processRepository.findById(1L).orElse(null);
        assertNotNull(updatedProcess);
        assertEquals(Process.ProcessState.READY, updatedProcess.getState()); // 状态应该保持不变
    }
    
    @Test
    public void testInterruptNonExistentProcess() {
        // 尝试中断不存在的进程
        processScheduler.interruptProcess(999L, "IO");
        // 不应该抛出异常
    }
    
    @Test
    public void testInterruptProcessStateTransitions() {
        // 创建测试进程
        Process process = new Process();
        process.setId(1L);
        process.setName("TestProcess");
        process.setPriority(1);
        process.setState(Process.ProcessState.RUNNING);
        process.setLastUpdateTime(LocalDateTime.now());
        processRepository.save(process);
        
        // 设置当前运行进程
        processScheduler.setCurrentProcess(process);
        
        // 测试状态转换记录
        processScheduler.interruptProcess(1L, "IO");
        
        // 验证状态转换记录
        List<ProcessTransition> transitions = processTransitionRepository.findByProcessId(1L);
        assertFalse(transitions.isEmpty());
        ProcessTransition lastTransition = transitions.get(transitions.size() - 1);
        assertEquals(Process.ProcessState.RUNNING, lastTransition.getFromState());
        assertEquals(Process.ProcessState.WAITING, lastTransition.getToState());
        assertEquals("等待IO完成", lastTransition.getReason());
    }
} 