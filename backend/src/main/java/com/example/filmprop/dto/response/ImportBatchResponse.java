package com.example.filmprop.dto.response;

import java.time.LocalDateTime;

public class ImportBatchResponse {
    private Long id;
    private String fileName;
    private String operator;
    private Integer totalRows;
    private Integer passedRows;
    private Integer failedRows;
    private Integer writtenRows;
    private Integer notWrittenRows;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime committedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getPassedRows() {
        return passedRows;
    }

    public void setPassedRows(Integer passedRows) {
        this.passedRows = passedRows;
    }

    public Integer getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(Integer failedRows) {
        this.failedRows = failedRows;
    }

    public Integer getWrittenRows() {
        return writtenRows;
    }

    public void setWrittenRows(Integer writtenRows) {
        this.writtenRows = writtenRows;
    }

    public Integer getNotWrittenRows() {
        return notWrittenRows;
    }

    public void setNotWrittenRows(Integer notWrittenRows) {
        this.notWrittenRows = notWrittenRows;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCommittedAt() {
        return committedAt;
    }

    public void setCommittedAt(LocalDateTime committedAt) {
        this.committedAt = committedAt;
    }
}
