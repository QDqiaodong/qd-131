package com.example.filmprop.dto.response;

/**
 * 扫码后条码校验结果：码值是否合法、是否已与库内档案重复。
 */
public class BarcodeCheckResponse {
    private boolean valid;
    private boolean duplicate;
    private String message;

    public BarcodeCheckResponse() {
    }

    public BarcodeCheckResponse(boolean valid, boolean duplicate, String message) {
        this.valid = valid;
        this.duplicate = duplicate;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
