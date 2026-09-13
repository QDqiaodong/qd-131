package com.example.filmprop.dto.response;

import java.time.LocalDate;

public class ConflictCheckResponse {
    private boolean hasConflict;
    private String conflictMessage;
    private Long conflictingBindingId;
    private String conflictingCrewName;
    private LocalDate conflictingStartDate;
    private LocalDate conflictingEndDate;

    public boolean isHasConflict() {
        return hasConflict;
    }

    public void setHasConflict(boolean hasConflict) {
        this.hasConflict = hasConflict;
    }

    public String getConflictMessage() {
        return conflictMessage;
    }

    public void setConflictMessage(String conflictMessage) {
        this.conflictMessage = conflictMessage;
    }

    public Long getConflictingBindingId() {
        return conflictingBindingId;
    }

    public void setConflictingBindingId(Long conflictingBindingId) {
        this.conflictingBindingId = conflictingBindingId;
    }

    public String getConflictingCrewName() {
        return conflictingCrewName;
    }

    public void setConflictingCrewName(String conflictingCrewName) {
        this.conflictingCrewName = conflictingCrewName;
    }

    public LocalDate getConflictingStartDate() {
        return conflictingStartDate;
    }

    public void setConflictingStartDate(LocalDate conflictingStartDate) {
        this.conflictingStartDate = conflictingStartDate;
    }

    public LocalDate getConflictingEndDate() {
        return conflictingEndDate;
    }

    public void setConflictingEndDate(LocalDate conflictingEndDate) {
        this.conflictingEndDate = conflictingEndDate;
    }
}
