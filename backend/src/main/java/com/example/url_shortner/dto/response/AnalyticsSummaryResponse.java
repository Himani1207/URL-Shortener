package com.example.url_shortner.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

/**
 * Aggregated analytics for a single link, backing the dedicated Analytics page.
 *
 * <p>The original four fields are unchanged, so anything already consuming this
 * response keeps working. The additions exist because the device split alone is not
 * enough to render a useful analytics page:
 * <ul>
 *   <li>{@code uniqueVisitors} — distinct IP addresses; approximate by nature.</li>
 *   <li>{@code browsers} / {@code operatingSystems} / {@code devices} — full
 *       breakdowns, aggregated in SQL. The three fixed device counters are retained
 *       for backwards compatibility and are also present in {@code devices}.</li>
 *   <li>{@code clicksPerDay} — a contiguous daily series for the trend chart.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Aggregated analytics for one short link")
public class AnalyticsSummaryResponse {

    private String shortCode;

    private Long totalClicks;

    private Long mobileUsers;

    private Long desktopUsers;

    private Long tabletUsers;

    /** Distinct source IP addresses. See the repository note on why this is approximate. */
    private Long uniqueVisitors;

    private List<LabelCountResponse> browsers;

    private List<LabelCountResponse> operatingSystems;

    private List<LabelCountResponse> devices;

    /** One entry per day over the requested window, including zero-click days. */
    private List<DailyClickResponse> clicksPerDay;
}
