package com.example.filmprop.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_change_logs")
public class ScheduleChangeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "binding_id", nullable = false)
    private Long bindingId;
    
    @Column(name = "prop_id", nullable = false)
    private Long propId;
    
    @Column(name = "crew_id", nullable = false)
    private Long crewId;
    
    @Column(name = "original_start_date")
    private LocalDate originalStartDate;
    
    @Column(name = "original_end_date")
    private LocalDate originalEndDate;
    
    @Column(name = "new_start_date", nullable = false)
    private LocalDate newStartDate;
    
    @Column(name = "new_end_date", nullable = false)
    private LocalDate newEndDate;
    
    @Column(name = "change_type", nullable = false, length = 20)
    private String changeType;
    
    @Column(name = "change_reason", length = 500)
    private String changeReason;
    
    @Column(name = "operator", length = 50)
    private String operator;
    
    @Column(name = "conflict_detected")
    private Boolean conflictDetected = false;
    
    @Column(name = "conflict_description", length = 500)
    private String conflictDescription;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBindingId() {
        return bindingId;
    }

    public void setBindingId(Long bindingId) {
        this.bindingId = bindingId;
    }

    public Long getPropId() {
        return propId;
    }

    public void setPropId(Long propId) {
        this.propId = propId;
    }

    public Long getCrewId() {
        return crewId;
    }

    public void setCrewId(Long crewId) {
        this.crewId = crewId;
    }

    public LocalDate getOriginalStartDate() {
        return originalStartDate;
    }

    public void setOriginalStartDate(LocalDate originalStartDate) {
        this.originalStartDate = originalStartDate;
    }

    public LocalDate getOriginalEndDate() {
        return originalEndDate;
    }

    public void setOriginalEndDate(LocalDate originalEndDate) {
        this.originalEndDate = originalEndDate;
    }

    public LocalDate getNewStartDate() {
        return newStartDate;
    }

    public void setNewStartDate(LocalDate newStartDate) {
        this.newStartDate = newStartDate;
    }

    public LocalDate getNewEndDate() {
        return newEndDate;
    }

    public void setNewEndDate(LocalDate newEndDate) {
        this.newEndDate = newEndDate;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Boolean getConflictDetected() {
        return conflictDetected;
    }

    public void setConflictDetected(Boolean conflictDetected) {
        this.conflictDetected = conflictDetected;
    }

    public String getConflictDescription() {
        return conflictDescription;
    }

    public void setConflictDescription(String conflictDescription) {
        this.conflictDescription = conflictDescription;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
