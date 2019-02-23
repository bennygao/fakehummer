package com.hanshow.fakehummer.api.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LogMessage {
    private LogLevel level;
    private String message;
}
