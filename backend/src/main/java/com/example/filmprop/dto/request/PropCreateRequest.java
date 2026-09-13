package com.example.filmprop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PropCreateRequest {
    @NotBlank(message = "道具编号不能为空")
    private String propCode;
    
    @NotBlank(message = "道具名称不能为空")
    private String propName;
    
    @NotBlank(message = "适用场景不能为空")
    private String sceneType;
    
    private String material;
    
    private String specification;
    
    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须为正整数")
    private Integer quantity;

    public String getPropCode() {
        return propCode;
    }

    public void setPropCode(String propCode) {
        this.propCode = propCode;
    }

    public String getPropName() {
        return propName;
    }

    public void setPropName(String propName) {
        this.propName = propName;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
