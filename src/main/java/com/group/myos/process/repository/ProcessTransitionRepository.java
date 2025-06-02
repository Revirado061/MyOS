package com.group.myos.process.repository;

import com.group.myos.process.model.Process;
import com.group.myos.process.model.ProcessTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessTransitionRepository extends JpaRepository<ProcessTransition, Long> {
    List<ProcessTransition> findByProcessId(Long processId);
    List<ProcessTransition> findByProcessOrderByTransitionTimeDesc(Process process);
    List<ProcessTransition> findByFromState(Process.ProcessState fromState);
    List<ProcessTransition> findByToState(Process.ProcessState toState);
} 