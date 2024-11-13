package com.streaming.app.video_streaming_backend.Payload;

import org.springframework.http.HttpStatus;

public class CustomMessage {
    private String message;
    private HttpStatus statusCode;
    private boolean success=false;

    public CustomMessage(String message, HttpStatus statusCode, boolean success) {
        this.message = message;
        this.statusCode = statusCode;
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "CustomMessage{" +
                "message='" + message + '\'' +
                ", statusCode=" + statusCode +
                ", success=" + success +
                '}';
    }
}
