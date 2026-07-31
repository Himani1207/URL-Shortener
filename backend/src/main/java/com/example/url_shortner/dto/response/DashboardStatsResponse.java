package com.example.url_shortner.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Account-level totals for the dashboard summary tiles.
 *
 * <p><b>Why a dedicated endpoint:</b> without it the dashboard would have to fetch
 * every link the user owns and sum the counters client-side. That transfers the
 * whole link list just to display four numbers, and gets slower with every link
 * added. These are four aggregate queries that the database answers from indexes.
 *
 * <p>Deliberately small: the brief calls for a clean dashboard, not a wall of
 * widgets, so this covers only the tiles actually rendered.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Account-level dashboard totals")
public class DashboardStatsResponse {

    /** Every link the user owns, active or not. */
    private Long totalLinks;

    private Long activeLinks;

    /** Sum of the denormalised counters on the links themselves. */
    private Long totalClicks;

    /** Clicks recorded in the last 7 days, for the trend caption. */
    private Long clicksLast7Days;

    /** Links created in the last 7 days. */
    private Long linksLast7Days;
}
