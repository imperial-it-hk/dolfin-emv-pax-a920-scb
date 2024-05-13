package com.pax.appstore.dto;

public class RequestDto {
    private Long actionId;
    private int status;
    private int errorCode;

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(Long actionId) {
        this.actionId = actionId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return "RequestDto{" +
                "actionId=" + actionId +
                ", status=" + status +
                ", errorCode=" + errorCode +
                '}';
    }
}
