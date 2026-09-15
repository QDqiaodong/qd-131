package com.example.filmprop.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "occupancy_import_batches")
public class OccupancyImportBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "operator", length = 50)
    private String operator;

    @Column(name = "total_rows")
    private Integer totalRows = 0;

    @Column(name = "passed_rows")
    private Integer passedRows = 0;

    @Column(name = "failed_rows")
    private Integer failedRows = 0;

    @Column(name = "written_rows")
    private Integer writtenRows = 0;

    @Column(name = "not_written_rows")
    private Integer notWrittenRows = 0;

    /** 状态：validated-已校验待写入，committed-已写入 */
    @Column(name = "status", length = 20)
    private String status = "validated";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "committed_at")
    private LocalDateTime committedAt;

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
