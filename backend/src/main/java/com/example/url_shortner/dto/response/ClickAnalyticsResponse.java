package com.example.url_shortner.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickAnalyticsResponse {

    private String ipAddress;

    private String browser;

    private String operatingSystem;

    private String device;

    private LocalDateTime clickedAt;
}