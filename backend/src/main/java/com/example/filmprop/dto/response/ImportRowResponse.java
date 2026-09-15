package com.example.filmprop.dto.response;

public class ImportRowResponse {
    private Long id;
    private Long batchId;
    private Integer rowNo;
    private String propCode;
    private String crewName;
    private String startDateRaw;
    private String endDateRaw;
    private String remark;
    private String validateStatus;
    private String validateMessage;
    private String writeStatus;
    private String writeMessage;
    private Long bindingId;

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
}
