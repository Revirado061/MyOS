package com.group.myos.process.repository;

import com.group.myos.process.model.Process;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessRepository extends JpaRepository<Process, Long> {
    List<Process> findByState(Process.ProcessState state);
    List<Process> findByInMemory(boolean inMemory);
} 