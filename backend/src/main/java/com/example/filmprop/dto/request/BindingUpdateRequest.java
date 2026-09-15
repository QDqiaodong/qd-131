package com.example.filmprop.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class BindingUpdateRequest {
    @NotNull(message = "绑定ID不能为空")
    private Long id;

    @NotNull(message = "占用版本号不能为空，请刷新后重试")
    private Long version;

    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private String bindingType;
    
    private String remark;
    
    private String changeReason;
    
    private String operator = "system";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public String getBindingType() {
        return bindingType;
    }

    public void setBindingType(String bindingType) {
        this.bindingType = bindingType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
}
