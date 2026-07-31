package com.example.url_shortner.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * One point in the clicks-over-time series.
 *
 * <p>The service emits a contiguous run of days including zero-click ones, so the
 * chart does not have to infer gaps — a sparse series renders as a misleading
 * straight line between two distant dates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Click count for a single day")
public class DailyClickResponse {

    @Schema(example = "2026-07-28")
    private LocalDate date;

    @Schema(example = "17")
    private Long count;
}
