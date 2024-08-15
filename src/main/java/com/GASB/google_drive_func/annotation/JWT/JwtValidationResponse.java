package com.GASB.google_drive_func.annotation.JWT;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class JwtValidationResponse implements Serializable {
    private final String email;
    private final String status;
}
