package com.example.url_shortner.repository.projection;

/**
 * Spring Data closed projection for {@code GROUP BY label, COUNT(*)} queries.
 *
 * <p>Used by the analytics breakdowns (browser, operating system, device). A
 * projection keeps the aggregation in the database and returns only two columns,
 * instead of loading every {@code ClickAnalytics} row into memory just to count
 * them — the difference matters as soon as a link accumulates real traffic.
 */
public interface LabelCount {

    /** The grouped value, e.g. "Chrome 120" or "Windows 10/11". */
    String getLabel();

    /** Number of clicks carrying that value. */
    Long getCount();
}
