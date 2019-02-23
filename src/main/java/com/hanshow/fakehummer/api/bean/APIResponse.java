package com.hanshow.fakehummer.api.bean;

import lombok.Data;

@Data
public class APIResponse {
    private int code;
    private String errorMessage;
    private Object data;

    public APIResponse(int code, String errorMessage) {
        this.code = code;
        this.errorMessage = errorMessage;
        this.data = null;
    }

    public APIResponse(Object data) {
        this.code = 0;
        this.errorMessage = "Success";
        this.data = data;
    }
}
