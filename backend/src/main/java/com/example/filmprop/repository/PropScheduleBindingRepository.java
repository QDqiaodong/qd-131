package com.example.filmprop.repository;

import com.example.filmprop.entity.PropScheduleBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PropScheduleBindingRepository extends JpaRepository<PropScheduleBinding, Long> {
    List<PropScheduleBinding> findByPropId(Long propId);
    List<PropScheduleBinding> findByCrewId(Long crewId);
    List<PropScheduleBinding> findByStatus(String status);
    
    @Query("SELECT b FROM PropScheduleBinding b WHERE b.propId = :propId AND b.status = 'active' " +
           "AND b.startDate <= :endDate AND b.endDate >= :startDate AND b.id != :excludeId")
    List<PropScheduleBinding> findConflictingBindings(
            @Param("propId") Long propId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId);
    
    @Query("SELECT b FROM PropScheduleBinding b WHERE b.status = 'active' " +
           "AND b.startDate <= :endDate AND b.endDate >= :startDate")
    List<PropScheduleBinding> findBindingsInDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    @Query("SELECT b FROM PropScheduleBinding b WHERE b.crewId = :crewId AND b.status = 'active' " +
           "ORDER BY b.startDate")
    List<PropScheduleBinding> findActiveBindingsByCrewId(@Param("crewId") Long crewId);
}
