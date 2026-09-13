package com.example.filmprop.repository;

import com.example.filmprop.entity.ScheduleChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleChangeLogRepository extends JpaRepository<ScheduleChangeLog, Long> {
    List<ScheduleChangeLog> findByBindingId(Long bindingId);
    List<ScheduleChangeLog> findByPropId(Long propId);
    List<ScheduleChangeLog> findByCrewId(Long crewId);
    List<ScheduleChangeLog> findByConflictDetected(Boolean conflictDetected);
    List<ScheduleChangeLog> findAllByOrderByCreatedAtDesc();
}
