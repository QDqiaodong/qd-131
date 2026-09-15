package com.example.filmprop.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "occupancy_import_rows")
public class OccupancyImportRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    /** 数据行号（不含表头，从 1 开始） */
    @Column(name = "row_no", nullable = false)
    private Integer rowNo;

    @Column(name = "prop_code", length = 50)
    private String propCode;

    @Column(name = "crew_name", length = 100)
    private String crewName;

    @Column(name = "start_date_raw", length = 20)
    private String startDateRaw;

    @Column(name = "end_date_raw", length = 20)
    private String endDateRaw;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "prop_id")
    private Long propId;

    @Column(name = "crew_id")
    private Long crewId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** 校验状态：passed-通过，failed-未通过 */
    @Column(name = "validate_status", length = 20)
    private String validateStatus = "failed";

    @Column(name = "validate_message", length = 500)
    private String validateMessage;

    /** 写入状态：pending-待写入，written-已写入，not_written-未写入 */
    @Column(name = "write_status", length = 20)
    private String writeStatus = "pending";

    @Column(name = "write_message", length = 500)
    private String writeMessage;

    @Column(name = "binding_id")
    private Long bindingId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public String getPropCode() {
        return propCode;
    }

    public void setPropCode(String propCode) {
        this.propCode = propCode;
    }

    public String getCrewName() {
        return crewName;
    }

    public void setCrewName(String crewName) {
        this.crewName = crewName;
    }

    public String getStartDateRaw() {
        return startDateRaw;
    }

    public void setStartDateRaw(String startDateRaw) {
        this.startDateRaw = startDateRaw;
    }

    public String getEndDateRaw() {
        return endDateRaw;
    }

    public void setEndDateRaw(String endDateRaw) {
        this.endDateRaw = endDateRaw;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getValidateStatus() {
        return validateStatus;
    }

    public void setValidateStatus(String validateStatus) {
        this.validateStatus = validateStatus;
    }

    public String getValidateMessage() {
        return validateMessage;
    }

    public void setValidateMessage(String validateMessage) {
        this.validateMessage = validateMessage;
    }

    public String getWriteStatus() {
        return writeStatus;
    }

    public void setWriteStatus(String writeStatus) {
        this.writeStatus = writeStatus;
    }

    public String getWriteMessage() {
        return writeMessage;
    }

    public void setWriteMessage(String writeMessage) {
        this.writeMessage = writeMessage;
    }

    public Long getBindingId() {
        return bindingId;
    }

    public void setBindingId(Long bindingId) {
        this.bindingId = bindingId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
